package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

/**
 * Root container for {@link SettingsGridFragment} that restores D-pad focus
 * to the previously active rail item or detail control when focus re-enters from the top bar.
 */
public class SettingsHubRootLayout extends LinearLayout {
    public interface OnRestoreFocusListener {
        boolean onRestoreFocus(int direction, @Nullable Rect previouslyFocusedRect);
    }

    private OnRestoreFocusListener mRestoreFocusListener;

    public SettingsHubRootLayout(Context context) {
        super(context);
    }

    public SettingsHubRootLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingsHubRootLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnRestoreFocusListener(@Nullable OnRestoreFocusListener listener) {
        mRestoreFocusListener = listener;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (mRestoreFocusListener != null && mRestoreFocusListener.onRestoreFocus(direction, previouslyFocusedRect)) {
            return true;
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }
}
