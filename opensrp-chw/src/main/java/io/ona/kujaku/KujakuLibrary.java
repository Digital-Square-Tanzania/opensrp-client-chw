package io.ona.kujaku;

import android.content.Context;

import androidx.annotation.NonNull;

public final class KujakuLibrary {

    private KujakuLibrary() {
        // No instances.
    }

    public static void init(@NonNull Context context) {
        // The original library initializes Realm-backed offline services.
        // The local replacement keeps map usage lightweight and stateless.
    }
}
