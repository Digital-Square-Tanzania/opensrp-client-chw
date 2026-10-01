package org.smartregister.chw.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Android16WindowInsetsTest {
    @Test
    public void repeatedInsetsPreserveOriginalPaddingWithoutAccumulating() {
        View view = mock(View.class);
        WindowInsetsCompat insets = mock(WindowInsetsCompat.class);
        int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                | WindowInsetsCompat.Type.ime();
        when(insets.getInsets(types)).thenReturn(Insets.of(8, 24, 9, 32));

        Android16WindowInsets.applyInsets(view, insets, 1, 2, 3, 4);
        Android16WindowInsets.applyInsets(view, insets, 1, 2, 3, 4);

        verify(view, times(2)).setPadding(9, 26, 12, 36);
    }

    @Test
    public void keyboardInsetsAreReleasedWhenKeyboardCloses() {
        View view = mock(View.class);
        WindowInsetsCompat insets = mock(WindowInsetsCompat.class);
        int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                | WindowInsetsCompat.Type.ime();
        when(insets.getInsets(types)).thenReturn(Insets.of(0, 24, 0, 320), Insets.of(0, 24, 0, 32));

        Android16WindowInsets.applyInsets(view, insets, 0, 0, 0, 0);
        Android16WindowInsets.applyInsets(view, insets, 0, 0, 0, 0);

        verify(view).setPadding(0, 24, 0, 320);
        verify(view).setPadding(0, 24, 0, 32);
    }
}
