package com.android.launcher3.graphics;

import android.content.Context;
import android.content.SharedPreferences;

public class GridCustomizationsProxy {
    private static final String PREFS_NAME = "grid_customizations";
    private static final String KEY_GRID_COLUMNS = "grid_columns";
    private static final String KEY_GRID_ROWS = "grid_rows";

    public static void resetWorkspace(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static void setCustomGrid(Context context, int rows, int columns) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putInt(KEY_GRID_ROWS, rows)
            .putInt(KEY_GRID_COLUMNS, columns)
            .apply();
    }
}
