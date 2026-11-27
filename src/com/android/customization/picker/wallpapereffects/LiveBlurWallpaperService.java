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

import android.app.wallpaper.WallpaperDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.io.File;

/** Draws an original, local blur transition each time the wallpaper becomes visible. */
public final class LiveBlurWallpaperService extends WallpaperService {
    static final String IMAGE_ID = "live_blur_image_id";

    static File imageFile(Context context, String imageId) {
        return new File(context.getFilesDir(), "live-blur-" + imageId + ".jpg");
    }

    static Bitmap createBlur(Bitmap source) {
        int sampleSide = 48;
        float scale = (float) sampleSide / Math.max(source.getWidth(), source.getHeight());
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        Bitmap sampled = Bitmap.createScaledBitmap(source, width, height, true);
        return Bitmap.createScaledBitmap(sampled, width * 4, height * 4, true);
    }

    @Override
    public Engine onCreateEngine() {
        return new BlurEngine(null);
    }

    @Override
    public Engine onCreateEngine(WallpaperDescription description) {
        String imageId = description.getContent().getString(IMAGE_ID);
        return new BlurEngine(imageId);
    }

    private final class BlurEngine extends Engine {
        private static final long TRANSITION_MS = 850;
        private static final long FRAME_MS = 32;
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private final Runnable mFrame = this::drawFrame;
        private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final RectF mBounds = new RectF();
        private final String mImageId;
        private Bitmap mPhoto;
        private Bitmap mBlurred;
        private boolean mVisible;
        private boolean mSurfaceReady;
        private long mStarted;

        BlurEngine(String imageId) {
            mImageId = imageId;
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            if (mPhoto == null && mImageId != null
                    && mImageId.matches("[0-9a-f-]{36}")) {
                mPhoto = BitmapFactory.decodeFile(
                        imageFile(LiveBlurWallpaperService.this, mImageId).getPath());
                if (mPhoto != null) {
                    mBlurred = createBlur(mPhoto);
                }
            }
            mSurfaceReady = true;
            drawFrame();
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            drawFrame();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                mStarted = SystemClock.uptimeMillis();
                drawFrame();
            } else {
                mHandler.removeCallbacks(mFrame);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            mSurfaceReady = false;
            mHandler.removeCallbacks(mFrame);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            mHandler.removeCallbacks(mFrame);
            mPhoto = null;
            mBlurred = null;
            super.onDestroy();
        }

        private void drawFrame() {
            mHandler.removeCallbacks(mFrame);
            if (!mSurfaceReady) {
                return;
            }

            float progress = Math.min(1f,
                    (float) (SystemClock.uptimeMillis() - mStarted) / TRANSITION_MS);
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.BLACK);
                    if (mPhoto != null) {
                        float scale = Math.max((float) canvas.getWidth() / mPhoto.getWidth(),
                                (float) canvas.getHeight() / mPhoto.getHeight());
                        float width = mPhoto.getWidth() * scale;
                        float height = mPhoto.getHeight() * scale;
                        float left = (canvas.getWidth() - width) / 2f;
                        float top = (canvas.getHeight() - height) / 2f;
                        mBounds.set(left, top, left + width, top + height);
                        mPaint.setAlpha(255);
                        canvas.drawBitmap(mPhoto, null, mBounds, mPaint);
                        mPaint.setAlpha(Math.round(255 * (1 - progress) * (1 - progress)));
                        canvas.drawBitmap(mBlurred, null, mBounds, mPaint);
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            if (mVisible && progress < 1 && mPhoto != null) {
                mHandler.postDelayed(mFrame, FRAME_MS);
            }
        }
    }
}
