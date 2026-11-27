/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.picker.wallpapereffects;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.wallpaper.WallpaperDescription;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.AtomicFile;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.themepicker.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Previews photo effects and applies a static or live wallpaper to the chosen screen. */
public final class WallpaperEffectsActivity extends Activity {
    private static final int PICK_PHOTO = 1;
    private static final int MAX_IMAGE_SIDE = 2560;
    private static final String PHOTO_URI = "photo_uri";
    private static final String EFFECT = "effect";

    private enum Effect {
        GLASS,
        ORIGINAL,
        LIVE_BLUR,
    }

    private final ExecutorService mWorker = Executors.newSingleThreadExecutor();
    private Button mChoosePhoto;
    private Button mApply;
    private Button mSave;
    private ImageView mPreview;
    private TextView mStatus;
    private Spinner mEffects;
    private Effect mEffect = Effect.GLASS;
    private Bitmap mSourcePhoto;
    private Bitmap mEditedPhoto;
    private Button mRotate;
    private CheckBox mCrop;
    private int mRotation;
    private Uri mPhotoUri;
    private Bitmap mRenderedPhoto;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.wallpaper_effects_activity);
        LinearLayout content = findViewById(R.id.wallpaper_effects_content);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                            | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        content.requestApplyInsets();
        mChoosePhoto = findViewById(R.id.wallpaper_effects_choose_photo);
        mApply = findViewById(R.id.wallpaper_effects_apply_button);
        mSave = findViewById(R.id.wallpaper_effects_save_button);
        mPreview = findViewById(R.id.wallpaper_effects_image);
        mStatus = findViewById(R.id.wallpaper_effects_status);
        mChoosePhoto.setOnClickListener(view -> choosePhoto());
        mApply.setOnClickListener(view -> chooseDestination());
        mSave.setOnClickListener(view -> savePhoto());

        mRotate = findViewById(R.id.wallpaper_effects_rotate);
        mCrop = findViewById(R.id.wallpaper_effects_crop);
        if (state != null) {
            mRotation = state.getInt("rotation");
            mCrop.setChecked(state.getBoolean("crop"));
        }
        mRotate.setOnClickListener(view -> {
            mRotation = (mRotation + 90) % 360;
            loadPhoto(mPhotoUri);
        });
        mCrop.setOnCheckedChangeListener((button, checked) -> {
            if (mPhotoUri != null) {
                loadPhoto(mPhotoUri);
            }
        });

        mEffects = findViewById(R.id.wallpaper_effects_choices);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.wallpaper_effect_choices, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEffects.setAdapter(adapter);
        if (state != null) {
            mEffect = Effect.values()[state.getInt(EFFECT)];
            mEffects.setSelection(mEffect.ordinal());
            Uri uri = state.getParcelable(PHOTO_URI, Uri.class);
            if (uri != null) {
                loadPhoto(uri);
            }
        }
        mEffects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Effect selected = Effect.values()[position];
                if (selected != mEffect) {
                    mEffect = selected;
                    if (mPhotoUri != null) {
                        loadPhoto(mPhotoUri);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void choosePhoto() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        try {
            startActivityForResult(picker, PICK_PHOTO);
        } catch (ActivityNotFoundException e) {
            showStatus(R.string.wallpaper_effects_picker_unavailable);
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_PHOTO && result == RESULT_OK && data != null) {
            Uri selectedPhoto = data.getData();
            if (selectedPhoto != null) {
                mRotation = 0;
                loadPhoto(selectedPhoto);
            }
        }
    }

    private void loadPhoto(Uri uri) {
        Bitmap cachedPhoto = uri.equals(mPhotoUri) ? mSourcePhoto : null;
        Effect effect = mEffect;
        int rotation = mRotation;
        boolean crop = mCrop.isChecked();
        Rect screen = getWindowManager().getMaximumWindowMetrics().getBounds();
        float aspect = (float) screen.width() / screen.height();
        mPhotoUri = uri;
        mSourcePhoto = null;
        mRenderedPhoto = null;
        mPreview.setImageDrawable(null);
        setBusy(true, R.string.wallpaper_effects_loading);
        mWorker.execute(() -> {
            Bitmap source = cachedPhoto;
            Bitmap edited = null;
            Bitmap rendered = null;
            try {
                if (source == null) {
                    source = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(getContentResolver(), uri),
                            (decoder, info, imageSource) -> {
                                int width = info.getSize().getWidth();
                                int height = info.getSize().getHeight();
                                double scale = Math.min(1.0,
                                        (double) MAX_IMAGE_SIDE / Math.max(width, height));
                                decoder.setTargetSize(Math.max(1, (int) (width * scale)),
                                        Math.max(1, (int) (height * scale)));
                                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                            });
                }
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                edited = Bitmap.createBitmap(source, 0, 0, source.getWidth(),
                        source.getHeight(), matrix, true);
                if (crop) {
                    int width = Math.min(edited.getWidth(),
                            Math.max(1, Math.round(edited.getHeight() * aspect)));
                    int height = Math.min(edited.getHeight(),
                            Math.max(1, Math.round(edited.getWidth() / aspect)));
                    edited = Bitmap.createBitmap(edited,
                            (edited.getWidth() - width) / 2,
                            (edited.getHeight() - height) / 2, width, height);
                }
                rendered = switch (effect) {
                    case GLASS -> renderRibbedGlass(edited);
                    case ORIGINAL -> edited;
                    case LIVE_BLUR -> LiveBlurWallpaperService.createBlur(edited);
                };
            } catch (IOException | IllegalArgumentException | SecurityException e) {
                // A document may have been removed or may no longer be readable.
            }
            Bitmap decoded = source;
            Bitmap editedResult = edited;
            Bitmap result = rendered;
            runOnUiThread(() -> {
                if (isDestroyed()) {
                    return;
                }
                mSourcePhoto = decoded;
                mEditedPhoto = editedResult;
                mRenderedPhoto = result;
                mPreview.setImageBitmap(result);
                setBusy(false, result == null ? R.string.wallpaper_effects_image_error : 0);
            });
        });
    }

    private static Bitmap renderRibbedGlass(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int ribWidth = Math.max(8, width / 34);
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(0xff000000);
        Rect sourceColumn = new Rect(0, 0, 1, height);
        Rect outputColumn = new Rect(0, 0, 1, height);
        Paint tint = new Paint();
        for (int x = 0; x < width; x++) {
            double phase = 2 * Math.PI * (x % ribWidth) / ribWidth;
            int warpedX = x + (int) Math.round(Math.sin(phase) * ribWidth / 3);
            int sampleX = Math.max(0, Math.min(width - 1, warpedX));
            sourceColumn.set(sampleX, 0, sampleX + 1, height);
            outputColumn.set(x, 0, x + 1, height);
            canvas.drawBitmap(source, sourceColumn, outputColumn, null);
            double shade = Math.cos(phase);
            tint.setColor(shade >= 0 ? 0xffffffff : 0xff000000);
            tint.setAlpha((int) (Math.abs(shade) * 32));
            canvas.drawRect(outputColumn, tint);
        }
        return output;
    }

    private void chooseDestination() {
        int[] flags = {
                WallpaperManager.FLAG_SYSTEM,
                WallpaperManager.FLAG_LOCK,
                WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK,
        };
        CharSequence[] labels = {
                getText(R.string.wallpaper_effects_home),
                getText(R.string.wallpaper_effects_lock),
                getText(R.string.wallpaper_effects_both),
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_effects_apply)
                .setItems(labels, (dialog, index) -> applyWallpaper(flags[index]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyWallpaper(int flags) {
        boolean isLive = mEffect == Effect.LIVE_BLUR;
        Bitmap photo = isLive ? mEditedPhoto : mRenderedPhoto;
        WallpaperManager manager = getSystemService(WallpaperManager.class);
        if (manager == null || !manager.isWallpaperSupported() || !manager.isSetWallpaperAllowed()) {
            showStatus(R.string.wallpaper_effects_disallowed);
            return;
        }
        if (photo == null) {
            return;
        }
        setBusy(true, R.string.wallpaper_effects_applying);
        mWorker.execute(() -> {
            boolean success;
            try {
                success = isLive
                        ? applyLiveWallpaper(manager, photo, flags)
                        : manager.setBitmap(photo, null, true, flags) > 0;
            } catch (IOException | IllegalArgumentException
                    | SecurityException e) {
                success = false;
            }
            boolean applied = success;
            runOnUiThread(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (applied) {
                    finish();
                } else {
                    setBusy(false, R.string.wallpaper_effects_apply_error);
                }
            });
        });
    }

    private boolean applyLiveWallpaper(WallpaperManager manager, Bitmap photo, int flags)
            throws IOException {
        String imageId = UUID.randomUUID().toString();
        AtomicFile image = new AtomicFile(LiveBlurWallpaperService.imageFile(this, imageId));
        FileOutputStream stream = image.startWrite();
        try {
            if (!photo.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw new IOException("Could not encode the live wallpaper");
            }
            image.finishWrite(stream);
        } catch (IOException | RuntimeException e) {
            image.failWrite(stream);
            throw e;
        }

        PersistableBundle content = new PersistableBundle();
        content.putString(LiveBlurWallpaperService.IMAGE_ID, imageId);
        WallpaperDescription description = new WallpaperDescription.Builder()
                .setComponent(new ComponentName(this, LiveBlurWallpaperService.class))
                .setId(imageId)
                .setContent(content)
                .build();
        boolean applied = false;
        try {
            applied = manager.setWallpaperComponentWithDescription(description, flags);
            return applied;
        } finally {
            if (!applied) {
                image.delete();
            }
        }
    }

    private void savePhoto() {
        Bitmap photo = mRenderedPhoto;
        if (photo == null || mEffect == Effect.LIVE_BLUR) {
            return;
        }
        setBusy(true, R.string.wallpaper_effects_saving);
        mWorker.execute(() -> {
            boolean saved = false;
            Uri output = null;
            ContentResolver resolver = getContentResolver();
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME,
                        "wallpaper-" + System.currentTimeMillis() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                output = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (output == null) {
                    throw new IOException("Could not create a photo");
                }
                try (OutputStream stream = resolver.openOutputStream(output)) {
                    if (stream == null || !photo.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                        throw new IOException("Could not encode the photo");
                    }
                }
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                saved = resolver.update(output, values, null, null) == 1;
            } catch (IOException | IllegalArgumentException | SecurityException e) {
                // The media provider or volume may be unavailable.
            }
            if (!saved && output != null) {
                try {
                    resolver.delete(output, null, null);
                } catch (IllegalArgumentException | SecurityException e) {
                    // The media provider may have removed the pending item already.
                }
            }

            int message = saved ? R.string.wallpaper_effects_saved
                    : R.string.wallpaper_effects_save_error;
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    setBusy(false, message);
                }
            });
        });
    }

    private void setBusy(boolean busy, int status) {
        mEffects.setEnabled(!busy);
        mRotate.setEnabled(!busy && mSourcePhoto != null);
        mCrop.setEnabled(!busy);
        mChoosePhoto.setEnabled(!busy);
        mApply.setEnabled(!busy && mRenderedPhoto != null);
        mSave.setEnabled(!busy && mRenderedPhoto != null && mEffect != Effect.LIVE_BLUR);
        showStatus(status);
    }

    private void showStatus(int status) {
        mStatus.setVisibility(status == 0 ? View.GONE : View.VISIBLE);
        if (status != 0) {
            mStatus.setText(status);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable(PHOTO_URI, mPhotoUri);
        state.putInt(EFFECT, mEffect.ordinal());
        state.putInt("rotation", mRotation);
        state.putBoolean("crop", mCrop.isChecked());
    }

    @Override
    protected void onDestroy() {
        mWorker.shutdown();
        super.onDestroy();
    }
}
