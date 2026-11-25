package com.liskovsoft.smartyoutubetv2.tv.ui.signin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.List;

public class SignInFragment extends GuidedStepSupportFragment implements SignInView {
    private static final String TAG = SignInFragment.class.getSimpleName();
    private static final int CONTINUE = 2;
    private static final int OPEN_BROWSER = 3;
    private SignInPresenter mSignInPresenter;
    private String mFullSignInUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSignInPresenter = SignInPresenter.instance(getContext());
        mSignInPresenter.setView(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        styleGuidanceViews();
        mSignInPresenter.onViewInitialized();
    }

    private void styleGuidanceViews() {
        if (getContext() == null || getGuidanceStylist() == null) {
            return;
        }

        TextView titleView = getGuidanceStylist().getTitleView();
        if (titleView != null) {
            titleView.setTextColor(ViewUtil.getThemeColor(
                    getContext(), R.attr.m3OnSurface, R.color.m3_on_surface));
            titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }

        TextView descriptionView = getGuidanceStylist().getDescriptionView();
        if (descriptionView != null) {
            descriptionView.setTextColor(ViewUtil.getThemeColor(
                    getContext(), R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant));
            descriptionView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }

        ImageView iconView = getGuidanceStylist().getIconView();
        if (iconView != null) {
            float density = getResources().getDisplayMetrics().density;
            float radiusPx = 16f * density;
            int padPx = Math.round(12 * density);
            iconView.setPadding(padPx, padPx, padPx, padPx);
            iconView.setBackground(ViewUtil.createRoundedDrawable(Color.WHITE, 0, 0, radiusPx));
            ViewUtil.applyRoundedOutline(iconView, radiusPx);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSignInPresenter.onViewDestroyed();
    }

    @Override
    public void showCode(String userCode, String signInUrl) {
        setTitle(userCode, signInUrl, null);
    }

    @Override
    public void showCode(String userCode, String signInUrl, String fullSignInUrl) {
        setTitle(userCode, signInUrl, fullSignInUrl);
    }

    private void setTitle(String userCode, String signInUrl, String fullSignInUrl) {
        if (getContext() == null || TextUtils.isEmpty(userCode)) {
            return;
        }

        getGuidanceStylist().getTitleView().setText(userCode);

        mFullSignInUrl = fullSignInUrl != null ? fullSignInUrl : signInUrl;

        Glide.with(getContext())
                .load(Utils.toQrCodeLink(mFullSignInUrl))
                .placeholder(R.drawable.activate_account_qrcode)
                .apply(ViewUtil.glideOptions())
                .error(R.drawable.activate_account_qrcode)
                .listener(mErrorListener)
                .into(getGuidanceStylist().getIconView());

        String description = getString(R.string.signin_view_description, signInUrl);
        int start = description.indexOf(signInUrl);
        CharSequence coloredDescription = description;
        if (start >= 0) {
            int end = start + signInUrl.length();
            int accentColor = ViewUtil.getThemeColor(
                    getContext(), R.attr.m3Primary, R.color.m3_grey_primary);
            coloredDescription = Utils.color(description, accentColor, start, end);
        }

        getGuidanceStylist().getDescriptionView().setText(coloredDescription);
    }

    @Override
    public void close() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
        String title = getString(R.string.signin_view_title);
        String description = getString(R.string.signin_view_description, "");
        return new GuidanceStylist.Guidance(title, description, "", ContextCompat.getDrawable(getContext(), R.drawable.activate_account_qrcode));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction login = new GuidedAction.Builder()
                .id(CONTINUE)
                .title(getString(R.string.signin_view_action_text))
                .build();
        GuidedAction openBrowser = new GuidedAction.Builder()
                .id(OPEN_BROWSER)
                .title(getString(R.string.login_from_browser))
                .build();
        actions.add(login);
        actions.add(openBrowser);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            mSignInPresenter.onActionClicked();
        } else if (action.getId() == OPEN_BROWSER) {
            if (mFullSignInUrl != null) {
                Utils.openLinkExt(getContext(), mFullSignInUrl);
            }
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
