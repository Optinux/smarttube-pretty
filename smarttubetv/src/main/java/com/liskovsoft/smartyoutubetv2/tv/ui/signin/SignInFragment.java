package com.liskovsoft.smartyoutubetv2.tv.ui.signin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionsStylist;
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
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.guided.M3GuidedActionsStylist;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
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

    @Override
    public GuidedActionsStylist onCreateActionsStylist() {
        return new M3GuidedActionsStylist();
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
            float radiusPx = getResources().getDimension(R.dimen.m3_card_large_corner_radius);
            int padPx = Math.round(14 * density);
            int outlineColor = ViewUtil.getThemeColor(
                    getContext(), R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
            int strokePx = getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);
            iconView.setPadding(padPx, padPx, padPx, padPx);
            iconView.setBackground(ViewUtil.createRoundedDrawable(Color.WHITE, outlineColor, strokePx, radiusPx));
            ViewUtil.applyRoundedOutline(iconView, radiusPx);
        }

        if (getView() != null) {
            View root = getView();
            View bg = root.findViewById(R.id.guidedstep_background);
            if (bg != null) {
                bg.setBackgroundColor(ViewUtil.getThemeColor(
                        getContext(), R.attr.m3Surface, R.color.m3_grey_surface));
            }
            View actionsBg = root.findViewById(R.id.action_fragment_background);
            if (actionsBg != null) {
                actionsBg.setBackgroundColor(ViewUtil.getThemeColor(
                        getContext(), R.attr.m3SurfaceContainerLow, R.color.m3_grey_surface_container_low));
            }
            M3Motion.animateEnter(root, 0f, 12f * getResources().getDisplayMetrics().density);
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
                .transition(ViewUtil.glideCrossFade(getContext()))
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
