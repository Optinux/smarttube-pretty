package com.liskovsoft.smartyoutubetv2.tv.ui.signin;

import android.os.Bundle;
import androidx.leanback.app.GuidedStepSupportFragment;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

public class SignInActivity extends LeanbackActivity {
    @Override
    protected void initTheme() {
        int guidedThemeResId = MainUIData.instance(this).getColorScheme().guidedStepThemeResId;
        if (guidedThemeResId > 0) {
            setTheme(guidedThemeResId);
        }
        applyDynamicWindowBackground();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new SignInFragment(), android.R.id.content);
        }
    }

    @Override
    public void finish() {
        super.finish();

        finishReally();
    }
}
