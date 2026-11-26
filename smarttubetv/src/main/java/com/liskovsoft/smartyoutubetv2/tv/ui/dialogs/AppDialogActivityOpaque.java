package com.liskovsoft.smartyoutubetv2.tv.ui.dialogs;

// Android 8 fallback
public class AppDialogActivityOpaque extends AppDialogActivity {
    @Override
    protected void initTheme() {
        // Preserve App.Theme.Leanback.Preferences.Opaque (windowIsTranslucent=false) on Android 8
    }
}
