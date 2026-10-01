package org.smartregister.chw.util;

import android.app.Activity;
import android.app.Application;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Keeps legacy activities, including library forms, inside Android 16's system bars. */
public final class Android16WindowInsets implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
        TypedArray attributes = activity.obtainStyledAttributes(new int[]{android.R.attr.windowIsFloating});
        boolean floating = attributes.getBoolean(0, false);
        attributes.recycle();
        if (floating) {
            return;
        }

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        // Include the AppCompat action bar, rather than padding only the activity's content.
        View root = content;
        while (root.getParent() instanceof View && root.getParent() != decor) {
            root = (View) root.getParent();
        }
        // The light window background is visible behind the transparent system bars.
        WindowCompat.getInsetsController(activity.getWindow(), root).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(activity.getWindow(), root).setAppearanceLightNavigationBars(true);
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            applyInsets(view, windowInsets, left, top, right, bottom);
            // Existing layouts use fitsSystemWindows; consuming here avoids double padding.
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    static void applyInsets(View view, WindowInsetsCompat windowInsets,
                            int left, int top, int right, int bottom) {
        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
        view.setPadding(left + insets.left, top + insets.top,
                right + insets.right, bottom + insets.bottom);
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
