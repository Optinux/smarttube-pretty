package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.SearchOrbView.Colors;
import androidx.leanback.widget.TitleView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.LanguageSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager.AccountChangeListener;
import com.liskovsoft.smartyoutubetv2.common.prefs.common.DataChangeBase.OnDataChange;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tooltips.TooltipCompatHandler;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.search.LongClickSearchOrbView;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time.DateTimeView;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.Locale;

import static androidx.leanback.widget.TitleViewAdapter.BRANDING_VIEW_VISIBLE;
import static androidx.leanback.widget.TitleViewAdapter.FULL_VIEW_VISIBLE;
import static androidx.leanback.widget.TitleViewAdapter.SEARCH_VIEW_VISIBLE;

/**
 * View that supports dpad navigation between children<br/>
 * NOTE: You should set android:nextFocusLeft and android:nextFocusRight<br/>
 * https://stackoverflow.com/questions/38169378/use-multiple-orb-buttons-or-other-buttons-in-the-leanbacks-title-view<br/>
 * https://stackoverflow.com/questions/40802470/add-button-to-browsefragment
 */
public class NavigateTitleView extends TitleView implements OnDataChange, AccountChangeListener {
    private static final long SETTINGS_TRANSITION_DURATION_MS = 240L;
    private LongClickSearchOrbView mAccountView;
    private SearchOrbView mLanguageView;
    private SearchOrbView mExitPip;
    private TextView mPipTitle;
    private int mSearchVisibility = View.INVISIBLE;
    private int mBrandingVisibility = View.INVISIBLE;
    private View mGlobalClockContainer;
    private DateTimeView mGlobalClock;
    private DateTimeView mGlobalDate;
    private View mTitleBadgeView;
    private View mTitleTextView;
    private SearchOrbView mSearchOrbView;
    private boolean mInitDone;
    private int mFlags = FULL_VIEW_VISIBLE;
    private int mIconWidth;
    private int mIconHeight;
    private boolean mIsSearchOrbEnabled;
    private boolean mIsAccountViewEnabled;
    private boolean mIsLanguageViewEnabled;
    private boolean mIsGlobalClockEnabled;
    private boolean mInSettingsMode;
    private float mSettingsProgress;
    private ValueAnimator mSettingsAnimator;

    public NavigateTitleView(Context context) {
        super(context);
    }

    public NavigateTitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigateTitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // findViewById is null in constructor. Init mAccountView later.
    }

    @Override
    public View focusSearch(View focused, int direction) {
        View nextFoundFocusableViewInLayout = null;

        // Only concerned about focusing left and right at the moment
        if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {

            // Try to find the next focusable item in this layout for the supplied direction
            int nextFoundFocusableViewInLayoutId = -1;
            switch(direction) {
                case View.FOCUS_LEFT :
                    nextFoundFocusableViewInLayoutId = focused.getNextFocusLeftId();
                    break;
                case View.FOCUS_RIGHT :
                    nextFoundFocusableViewInLayoutId = focused.getNextFocusRightId();
                    break;
            }

            // View id for next focus direction found....get the View
            if (nextFoundFocusableViewInLayoutId != -1) {
                nextFoundFocusableViewInLayout = findViewById(nextFoundFocusableViewInLayoutId);
            }
        }

        //  Return the found View in the layout if it's focusable
        if (nextFoundFocusableViewInLayout != null && nextFoundFocusableViewInLayout != focused && nextFoundFocusableViewInLayout.isFocusable()) {
            if (nextFoundFocusableViewInLayout.getVisibility() == View.VISIBLE) {
                return nextFoundFocusableViewInLayout;
            } else {
                return focusSearch(nextFoundFocusableViewInLayout, direction);
            }
        } else {
            // No focusable view found in layout...propagate to super (should invoke the BrowseFrameLayout.OnFocusSearchListener
            return super.focusSearch(focused, direction);
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (mInSettingsMode) {
            return false;
        }
        // Gives focus to the SearchOrb first....if not...default to normal descendant focus search
        View searchView = getSearchAffordanceView();
        if (searchView != null && searchView.getVisibility() == View.VISIBLE && searchView.isFocusable() && searchView.requestFocus()) {
            return true;
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public void updateComponentsVisibility(int flags) {
        // Fix for: Fatal Exception: java.lang.IllegalStateException
        // Fragment has not been attached yet.
        // Inside: super.updateComponentsVisibility(flags);
        if (getWindowToken() == null) {
            return;
        }

        super.updateComponentsVisibility(flags);

        init();

        mFlags = flags;

        mSearchVisibility = (flags & SEARCH_VIEW_VISIBLE) == SEARCH_VIEW_VISIBLE
                ? View.VISIBLE : View.INVISIBLE;

        mBrandingVisibility = (flags & BRANDING_VIEW_VISIBLE) == BRANDING_VIEW_VISIBLE
                ? View.VISIBLE : View.INVISIBLE;

        if (mIsSearchOrbEnabled) {
            mSearchOrbView.setVisibility(View.GONE);
        }

        if (mIsAccountViewEnabled) {
            mAccountView.setVisibility(mSearchVisibility);
        }

        if (mIsLanguageViewEnabled) {
            mLanguageView.setVisibility(mSearchVisibility);
        }

        if (mExitPip != null && (PlaybackPresenter.instance(getContext()).isRunningInBackground() || mSearchVisibility != View.VISIBLE)) {
            mExitPip.setVisibility(mSearchVisibility);
            mPipTitle.setVisibility(mSearchVisibility);
        }

        if (mIsGlobalClockEnabled) {
            mGlobalClock.setVisibility(mBrandingVisibility);
            mGlobalDate.setVisibility(mBrandingVisibility);
            if (mGlobalClockContainer != null) {
                mGlobalClockContainer.setVisibility(mBrandingVisibility);
            }
        }

        applySettingsProgress(mSettingsProgress);
    }

    private void init() {
        if (mInitDone) {
            return;
        }

        MediaServiceManager.instance().addAccountListener(this);

        setupButtons();

        MainUIData mainUIData = MainUIData.instance(getContext());
        mainUIData.setOnChange(this);

        mInitDone = true;
    }

    private void setupButtons() {
        MainUIData mainUIData = MainUIData.instance(getContext());

        mSearchOrbView = findViewById(R.id.title_orb);

        mAccountView = findViewById(R.id.account_orb);
        mAccountView.setOnOrbClickedListener(v -> AccountSelectionPresenter.instance(getContext()).nextAccountOrDialog());
        mAccountView.setOnOrbLongClickedListener(v -> {
            AccountSettingsPresenter.instance(getContext()).show();
            return true;
        });
        TooltipCompatHandler.setTooltipText(mAccountView, getContext().getString(R.string.settings_accounts));

        mLanguageView = findViewById(R.id.language_orb);
        mLanguageView.setOnOrbClickedListener(v -> LanguageSettingsPresenter.instance(getContext()).show());
        TooltipCompatHandler.setTooltipText(mLanguageView, getContext().getString(R.string.settings_language_country));

        mExitPip = findViewById(R.id.exit_pip);
        mPipTitle = findViewById(R.id.pip_title);
        mExitPip.setOnOrbClickedListener(v -> ViewManager.instance(getContext()).startView(PlaybackView.class));
        ViewUtil.enableMarquee(mPipTitle);
        ViewUtil.setTextScrollSpeed(mPipTitle, mainUIData.getCardTextScrollSpeed());
        TooltipCompatHandler.setTooltipText(mExitPip, getContext().getString(R.string.return_to_background_video));

        mGlobalClockContainer = findViewById(R.id.global_clock_container);
        mGlobalClock = findViewById(R.id.global_time);
        mGlobalClock.showDate(false);

        mGlobalDate = findViewById(R.id.global_date);
        mGlobalDate.showTime(false);
        mGlobalDate.showDate(true);

        mTitleBadgeView = findViewById(R.id.title_badge);
        mTitleTextView = findViewById(R.id.title_text);

        int orbBg = ViewUtil.getThemeColor(getContext(), R.attr.m3OrbBackground, R.color.m3_grey_surface_container_high);
        int orbFocusedBg = ViewUtil.getThemeColor(getContext(), R.attr.m3OrbFocusedBackground, R.color.m3_grey_primary_container);
        int orbIconColor = ViewUtil.getThemeColor(getContext(), R.attr.m3OrbIconColor, R.color.m3_on_surface);
        int orbStrokeColor = ViewUtil.getThemeColor(getContext(), R.attr.m3Outline, R.color.m3_grey_outline);
        Colors orbColors = new Colors(orbBg, orbFocusedBg, orbIconColor);
        if (mSearchOrbView != null) {
            mSearchOrbView.setOrbColors(orbColors);
            mSearchOrbView.setFocusedStrokeColor(orbStrokeColor);
        }
        mAccountView.setOrbColors(orbColors);
        mAccountView.setFocusedStrokeColor(orbStrokeColor);
        mLanguageView.setOrbColors(orbColors);
        mLanguageView.setFocusedStrokeColor(orbStrokeColor);
        mExitPip.setOrbColors(orbColors);
        mExitPip.setFocusedStrokeColor(orbStrokeColor);

        int onSurface = ViewUtil.getThemeColor(getContext(), R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(getContext(), R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        int surfaceContainerHigh = ViewUtil.getThemeColor(getContext(), R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(getContext(), R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        float pillRadius = getResources().getDimension(R.dimen.m3_pill_corner_radius);
        int hairlinePx = getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);
        float density = getResources().getDisplayMetrics().density;
        int padH = Math.round(14 * density);
        int padV = Math.round(6 * density);

        mPipTitle.setTextColor(onSurface);
        mPipTitle.setBackground(ViewUtil.createRoundedDrawable(surfaceContainerHigh, outlineVariant, hairlinePx, pillRadius));
        mPipTitle.setPaddingRelative(padH, padV, padH, padV);
        mGlobalClock.setTextColor(onSurface);
        mGlobalDate.setTextColor(onSurfaceVariant);

        updateButtonsVisibility();
    }

    private void updateButtonsVisibility() {
        MainUIData mainUIData = MainUIData.instance(getContext());

        mIsSearchOrbEnabled = !mainUIData.isTopButtonEnabled(MainUIData.TOP_BUTTON_SEARCH);
        mIsAccountViewEnabled = mainUIData.isTopButtonEnabled(MainUIData.TOP_BUTTON_BROWSE_ACCOUNTS);
        mIsLanguageViewEnabled = mainUIData.isTopButtonEnabled(MainUIData.TOP_BUTTON_CHANGE_LANGUAGE);
        mIsGlobalClockEnabled = GeneralData.instance(getContext()).isGlobalClockEnabled();

        mSearchOrbView.setVisibility(mIsSearchOrbEnabled ? View.VISIBLE : View.GONE);
        mAccountView.setVisibility(mIsAccountViewEnabled ? View.VISIBLE : View.GONE);
        mLanguageView.setVisibility(mIsLanguageViewEnabled ? View.VISIBLE : View.GONE);
        mGlobalClock.setVisibility(mIsGlobalClockEnabled ? View.VISIBLE : View.GONE);
        mGlobalDate.setVisibility(mIsGlobalClockEnabled ? View.VISIBLE : View.GONE);
        if (mGlobalClockContainer != null) {
            mGlobalClockContainer.setVisibility(mIsGlobalClockEnabled ? View.VISIBLE : View.GONE);
        }

        applySettingsProgress(mSettingsProgress);

        Utils.postDelayed(this::updateAccountIcon, 1_000); // give a time to engine to fetch an updated icon url
        //updateAccountIcon();
        updateLanguageIcon();
    }

    /**
     * Smoothly fades out top bar elements (profile icon, clock, logo, extra orbs) when entering
     * full-height Settings, and fades them back in when leaving. Cancel-safe if toggled mid-animation.
     */
    public void setSettingsMode(boolean inSettings, boolean animate) {
        init();
        float targetProgress = inSettings ? 1f : 0f;
        if (mInSettingsMode == inSettings) {
            if (mSettingsAnimator != null && mSettingsAnimator.isRunning()) {
                return;
            }
            if (Math.abs(mSettingsProgress - targetProgress) < 0.001f) {
                updateSettingsInteractiveState(inSettings);
                return;
            }
        }
        mInSettingsMode = inSettings;
        updateSettingsInteractiveState(inSettings);

        if (mSettingsAnimator != null) {
            mSettingsAnimator.cancel();
            mSettingsAnimator = null;
        }

        if (!animate || !M3Motion.areAnimationsEnabled(getContext()) || !ViewCompat.isAttachedToWindow(this)) {
            mSettingsProgress = targetProgress;
            applySettingsProgress(targetProgress);
            return;
        }

        float startProgress = mSettingsProgress;
        long duration = Math.max(80L, Math.round(SETTINGS_TRANSITION_DURATION_MS * Math.abs(targetProgress - startProgress)));
        ValueAnimator animator = ValueAnimator.ofFloat(startProgress, targetProgress);
        animator.setDuration(duration);
        animator.setInterpolator(M3Motion.emphasizedDecelerate());
        animator.addUpdateListener(anim -> {
            mSettingsProgress = (float) anim.getAnimatedValue();
            applySettingsProgress(mSettingsProgress);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mSettingsAnimator == animation) {
                    mSettingsAnimator = null;
                    mSettingsProgress = targetProgress;
                    applySettingsProgress(targetProgress);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (mSettingsAnimator == animation) {
                    mSettingsAnimator = null;
                }
            }
        });
        mSettingsAnimator = animator;
        animator.start();
    }

    private void updateSettingsInteractiveState(boolean inSettings) {
        setOrbInteractive(mAccountView, !inSettings && mIsAccountViewEnabled);
        setOrbInteractive(mLanguageView, !inSettings && mIsLanguageViewEnabled);
        setOrbInteractive(mSearchOrbView, !inSettings && !mIsSearchOrbEnabled);
        boolean pipRunning = PlaybackPresenter.instance(getContext()).isRunningInBackground();
        setOrbInteractive(mExitPip, !inSettings && pipRunning);
        if (mGlobalClockContainer != null) {
            mGlobalClockContainer.setFocusable(false);
            mGlobalClockContainer.setClickable(false);
        }
        if (mGlobalClock != null) {
            mGlobalClock.setFocusable(false);
            mGlobalClock.setClickable(false);
        }
        if (mGlobalDate != null) {
            mGlobalDate.setFocusable(false);
            mGlobalDate.setClickable(false);
        }
    }

    private static void setOrbInteractive(@Nullable View view, boolean interactive) {
        if (view == null) {
            return;
        }
        view.setFocusable(interactive);
        view.setFocusableInTouchMode(interactive);
        view.setClickable(interactive);
        view.setEnabled(interactive);
    }

    private void applySettingsProgress(float p) {
        if (!mInitDone) {
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, p));
        float fadeAlpha = 1f - clamped;
        float density = getResources().getDisplayMetrics().density;
        boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
        float rtlSign = isRtl ? -1f : 1f;

        boolean showBrowseItems = clamped < 0.999f;

        // Profile icon fade-out (no morph/crossfade into back arrow)
        if (mAccountView != null) {
            if (mIsAccountViewEnabled && mSearchVisibility == View.VISIBLE) {
                mAccountView.setVisibility(showBrowseItems ? View.VISIBLE : View.INVISIBLE);
                mAccountView.setAlpha(fadeAlpha);
            } else {
                mAccountView.setVisibility(View.GONE);
            }
        }

        // Clock fade out with alpha + slight translate
        if (mGlobalClockContainer != null) {
            if (mIsGlobalClockEnabled && mBrandingVisibility == View.VISIBLE) {
                mGlobalClockContainer.setVisibility(showBrowseItems ? View.VISIBLE : View.INVISIBLE);
                mGlobalClockContainer.setAlpha(fadeAlpha);
                mGlobalClockContainer.setTranslationY(-6f * density * clamped);
                mGlobalClockContainer.setTranslationX(8f * density * rtlSign * clamped);
            } else {
                mGlobalClockContainer.setVisibility(View.GONE);
            }
        }

        // Fade branding logo/title and top scrim so full-height Settings is unobstructed
        boolean showBranding = (mFlags & BRANDING_VIEW_VISIBLE) == BRANDING_VIEW_VISIBLE;
        boolean hasBadge = getBadgeDrawable() != null;
        if (mTitleBadgeView != null) {
            if (showBranding && hasBadge) {
                mTitleBadgeView.setVisibility(showBrowseItems ? View.VISIBLE : View.INVISIBLE);
                mTitleBadgeView.setAlpha(fadeAlpha);
            } else {
                mTitleBadgeView.setVisibility(View.GONE);
            }
        }
        if (mTitleTextView != null) {
            if (showBranding && !hasBadge) {
                mTitleTextView.setVisibility(showBrowseItems ? View.VISIBLE : View.INVISIBLE);
                mTitleTextView.setAlpha(fadeAlpha);
            } else {
                mTitleTextView.setVisibility(View.GONE);
            }
        }

        if (mLanguageView != null && mIsLanguageViewEnabled && mSearchVisibility == View.VISIBLE) {
            mLanguageView.setVisibility(showBrowseItems ? View.VISIBLE : View.GONE);
            mLanguageView.setAlpha(fadeAlpha);
            mLanguageView.setTranslationY(-4f * density * clamped);
        }
        if (mSearchOrbView != null && !mIsSearchOrbEnabled && mSearchVisibility == View.VISIBLE) {
            mSearchOrbView.setVisibility(showBrowseItems ? View.VISIBLE : View.INVISIBLE);
            mSearchOrbView.setAlpha(fadeAlpha);
        }
        if (mExitPip != null && mPipTitle != null) {
            boolean pipActive = PlaybackPresenter.instance(getContext()).isRunningInBackground();
            if (pipActive && mSearchVisibility == View.VISIBLE) {
                int vis = showBrowseItems ? View.VISIBLE : View.INVISIBLE;
                mExitPip.setVisibility(vis);
                mExitPip.setAlpha(fadeAlpha);
                mPipTitle.setVisibility(vis);
                mPipTitle.setAlpha(fadeAlpha);
            }
        }

        Drawable bg = getBackground();
        if (bg != null) {
            bg.mutate().setAlpha(Math.round(255 * fadeAlpha));
        }

        updateSettingsInteractiveState(mInSettingsMode);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mSettingsAnimator != null) {
            mSettingsAnimator.cancel();
            mSettingsAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility == View.VISIBLE) { // scroll grid up, scroll grid down
            applyPipParameters();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) { // pip window closed, dialog closed
            applyPipParameters();
        }
    }

    @Override
    public void onAccountChanged(Account account) {
        updateAccountIcon();
    }

    private void applyPipParameters() {
        if (mExitPip != null) {
            int newVisibility = PlaybackPresenter.instance(getContext()).isRunningInBackground() ? mSearchVisibility : View.INVISIBLE;
            mExitPip.setVisibility(newVisibility);
            mPipTitle.setVisibility(newVisibility);

            if (newVisibility == View.VISIBLE) {
                Video video = PlaybackPresenter.instance(getContext()).getVideo();
                mPipTitle.setText(video != null ? String.format("%s - %s", video.getTitle(), video.getAuthor()) : "");
            }
            applySettingsProgress(mSettingsProgress);
        }
    }

    private void updateAccountIcon() {
        if (!mIsAccountViewEnabled) {
            return;
        }

        Account current = MediaServiceManager.instance().getSelectedAccount();

        if (current != null && current.getAvatarImageUrl() != null) {
            loadIcon(mAccountView, current.getAvatarImageUrl(), false);
            String accountName = current.getName() != null ? current.getName() : current.getEmail();
            //TooltipCompatHandler.setTooltipText(mAccountView, Utils.updateTooltip(getContext(), accountName));
            TooltipCompatHandler.setTooltipText(mAccountView, accountName);
        } else {
            Colors orbColors = mAccountView.getOrbColors();
            int iconColor = ViewUtil.getThemeColor(getContext(), R.attr.m3OrbIconColor, R.color.m3_on_surface);
            mAccountView.setOrbColors(new Colors(orbColors.color, orbColors.brightColor, iconColor));
            mAccountView.setOrbIcon(ContextCompat.getDrawable(getContext(), R.drawable.browse_title_account));
            TooltipCompatHandler.setTooltipText(mAccountView, getContext().getString(R.string.dialog_account_none));
        }
    }

    private void updateLanguageIcon() {
        if (!mIsLanguageViewEnabled) {
            return;
        }

        // Use delay to fix icon initialization on app boot
        Utils.postDelayed(() -> {
            Locale locale = LocaleUtility.getCurrentLocale(getContext());
            loadIcon(mLanguageView, Utils.getCountryFlagUrl(locale.getCountry()), true); // flag server could be down
            TooltipCompatHandler.setTooltipText(mLanguageView, String.format("%s (%s)", locale.getDisplayCountry(), locale.getDisplayLanguage()));
        }, 100);
    }

    private void loadIcon(SearchOrbView view, String url, boolean useCache) {
        if (view == null) {
            return;
        }

        // The view with GONE visibility has zero width and height
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            Utils.postDelayed(() -> loadIcon(view, url, useCache), 500);
            return;
        }

        Context context = view.getContext();

        if (context instanceof Activity && !Utils.checkActivity((Activity) context)) {
            return;
        }

        // Size of the view might increase after icon change (bug on some firmwares). So, it's better to cache initial values.
        if (mIconWidth == 0 || mIconHeight == 0) {
            mIconWidth = view.getWidth();
            mIconHeight = view.getHeight();
        }

        try {
            loadIcon(context, view, url, mIconWidth, mIconHeight, useCache);
        } catch (ExceptionInInitializerError e) {
            // Glide Kivi error
            e.printStackTrace();
        }
    }

    private static void loadIcon(Context context, SearchOrbView view, String url, int iconWidth, int iconHeight, boolean useCache) {
        Glide.with(context)
                .load(url)
                .apply(ViewUtil.glideOptions())
                .diskCacheStrategy(useCache ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .circleCrop() // resize image
                .into(new SimpleTarget<Drawable>(iconWidth, iconHeight) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Colors orbColors = view.getOrbColors();
                        view.setOrbColors(new Colors(orbColors.color, orbColors.brightColor, Color.TRANSPARENT));
                        view.setOrbIcon(resource);
                    }
                });
    }

    @Override
    public void onDataChange() {
        try {
            updateButtonsVisibility();
            updateComponentsVisibility(mFlags);
        } catch (IllegalStateException e) {
            // Fragment BrowseFragment has not been attached yet.
            e.printStackTrace();
        }
    }
}
