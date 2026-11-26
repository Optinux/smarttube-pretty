package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Interactive Material 3 Expressive segmented setting row supporting instant inline toggles (M3Switch),
 * value cycling / segmented pills, and stepper adjustments via D-pad or click.
 */
public class M3QuickToggleRow extends LinearLayout {
    public interface OnStepListener {
        void onStep(int delta);
    }

    private ImageView mIconView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mValuePill;
    private M3Switch mSwitch;
    private ImageView mTrailingIcon;
    private boolean mTrailingAccentTint;
    private OnStepListener mStepListener;
    private float[] mSegmentedRadii;
    private float mOuterRadiusPx;
    private int mFocusStrokePx;

    private int mRestingFill;
    private int mFocusedFill;
    private int mPrimary;
    private int mOnSurface;
    private int mOnSurfaceVariant;
    private int mOnSecondaryContainer;
    private int mPrimaryContainer;
    private int mOnPrimaryContainer;
    private int mOutline;

    public M3QuickToggleRow(Context context) {
        this(context, null);
    }

    public M3QuickToggleRow(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public M3QuickToggleRow(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        float density = context.getResources().getDisplayMetrics().density;
        setMinimumHeight(Math.round(58 * density));
        int padH = Math.round(16 * density);
        int padV = Math.round(10 * density);
        setPaddingRelative(padH, padV, padH, padV);

        mOuterRadiusPx = context.getResources().getDimension(R.dimen.m3_card_corner_radius);
        mFocusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);

        mIconView = new ImageView(context);
        int iconSize = Math.round(22 * density);
        LayoutParams iconLp = new LayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd(Math.round(14 * density));
        mIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mIconView.setVisibility(GONE);
        addView(mIconView, iconLp);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(VERTICAL);
        LayoutParams textColLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        textColLp.setMarginEnd(Math.round(12 * density));

        mTitleView = new TextView(context);
        mTitleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        mTitleView.setSingleLine(true);
        mTitleView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(mTitleView);

        mSubtitleView = new TextView(context);
        mSubtitleView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        mSubtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mSubtitleView.setSingleLine(true);
        mSubtitleView.setEllipsize(TextUtils.TruncateAt.END);
        mSubtitleView.setVisibility(GONE);
        textCol.addView(mSubtitleView);

        addView(textCol, textColLp);

        mValuePill = new TextView(context);
        mValuePill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mValuePill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mValuePill.setSingleLine(true);
        mValuePill.setEllipsize(TextUtils.TruncateAt.END);
        mValuePill.setMaxWidth(Math.round(180 * density));
        mValuePill.setPaddingRelative(Math.round(12 * density), Math.round(6 * density), Math.round(12 * density), Math.round(6 * density));
        mValuePill.setVisibility(GONE);
        addView(mValuePill);

        mSwitch = new M3Switch(context);
        mSwitch.setVisibility(GONE);
        addView(mSwitch);

        mTrailingIcon = new ImageView(context);
        int trailingSize = Math.round(20 * density);
        LayoutParams trailingLp = new LayoutParams(trailingSize, trailingSize);
        trailingLp.setMarginStart(Math.round(8 * density));
        mTrailingIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mTrailingIcon.setVisibility(GONE);
        addView(mTrailingIcon, trailingLp);

        resolveThemeColors();
        applyVisualState(false, false);

        setOnFocusChangeListener((v, hasFocus) -> {
            applyVisualState(hasFocus, true);
            M3Motion.animateFocusScale(this, hasFocus, 1.012f);
        });
    }

    public void setSegmentedPosition(boolean isFirst, boolean isLast) {
        float innerRadiusPx = getResources().getDimension(R.dimen.m3_segment_inner_corner_radius);
        mSegmentedRadii = M3Motion.buildSegmentedRadii(mOuterRadiusPx, innerRadiusPx, isFirst, isLast);
        applyVisualState(hasFocus(), false);
    }

    public void setTitle(CharSequence title) {
        mTitleView.setText(title);
    }

    public void setSubtitle(@Nullable CharSequence subtitle) {
        if (TextUtils.isEmpty(subtitle)) {
            mSubtitleView.setVisibility(GONE);
        } else {
            mSubtitleView.setText(subtitle);
            mSubtitleView.setVisibility(VISIBLE);
        }
    }

    public void setIcon(@DrawableRes int resId) {
        if (resId > 0) {
            mIconView.setImageDrawable(ContextCompat.getDrawable(getContext(), resId));
            mIconView.setVisibility(VISIBLE);
            M3Motion.setImageTint(mIconView, hasFocus() ? mOnSecondaryContainer : mOnSurfaceVariant);
        } else {
            mIconView.setVisibility(GONE);
        }
    }

    public void bindSwitch(boolean checked, boolean animate) {
        mValuePill.setVisibility(GONE);
        mTrailingIcon.setVisibility(GONE);
        mSwitch.setVisibility(VISIBLE);
        mSwitch.setChecked(checked, animate);
    }

    public void bindValuePill(CharSequence text) {
        mSwitch.setVisibility(GONE);
        mTrailingIcon.setVisibility(GONE);
        mValuePill.setVisibility(VISIBLE);
        mValuePill.setText(text);
        float pillRadius = getResources().getDimension(R.dimen.m3_pill_corner_radius);
        mValuePill.setBackground(ViewUtil.createRoundedDrawable(mPrimaryContainer, 0, 0, pillRadius));
        mValuePill.setTextColor(mOnPrimaryContainer);
    }

    public void bindTrailingIcon(@DrawableRes int resId, boolean accentTint) {
        mSwitch.setVisibility(GONE);
        mValuePill.setVisibility(GONE);
        mTrailingAccentTint = accentTint;
        if (resId > 0) {
            mTrailingIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), resId));
            mTrailingIcon.setVisibility(VISIBLE);
            int restingColor = mTrailingAccentTint ? mPrimary : mOnSurfaceVariant;
            M3Motion.setImageTint(mTrailingIcon, hasFocus() ? mOnSecondaryContainer : restingColor);
        } else {
            mTrailingIcon.setVisibility(GONE);
        }
    }

    public void setOnStepListener(@Nullable OnStepListener listener) {
        mStepListener = listener;
    }

    public void refreshThemeColors() {
        resolveThemeColors();
        mSwitch.refreshThemeColors();
        applyVisualState(hasFocus(), false);
        if (mValuePill.getVisibility() == VISIBLE) {
            float pillRadius = getResources().getDimension(R.dimen.m3_pill_corner_radius);
            mValuePill.setBackground(ViewUtil.createRoundedDrawable(mPrimaryContainer, 0, 0, pillRadius));
            mValuePill.setTextColor(mOnPrimaryContainer);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mStepListener != null) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mStepListener.onStep(1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // Allow stepping down only if there is a left-step handler; caller returns focus to left rail if needed
                // Wait: on TV, pressing LEFT at leftmost edge of right pane should jump back to Left Rail!
                // So we only intercept DPAD_RIGHT or +/- or we let users cycle via OK / DPAD_RIGHT,
                // OR we handle STEP via OK + dedicated stepper buttons.
                // Let's keep DPAD_LEFT free for returning to the Left Category Rail!
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void resolveThemeColors() {
        Context ctx = getContext();
        mRestingFill = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        mFocusedFill = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        mPrimary = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
        mOnSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        mOnSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        mOnSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
        mPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        mOnPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        mOutline = ViewUtil.getThemeColor(ctx, R.attr.m3Outline, R.color.m3_grey_outline);
    }

    private void applyVisualState(boolean focused, boolean animate) {
        if (!animate) {
            M3Motion.cancelFocusAnimations(this);
        }

        int fromBg = focused ? mRestingFill : mFocusedFill;
        int toBg = focused ? mFocusedFill : mRestingFill;
        int fromStroke = focused ? Color.TRANSPARENT : mOutline;
        int toStroke = focused ? mOutline : Color.TRANSPARENT;
        int fromStrokePx = focused ? 0 : mFocusStrokePx;
        int toStrokePx = focused ? mFocusStrokePx : 0;
        int fromTitle = focused ? mOnSurface : mOnSecondaryContainer;
        int toTitle = focused ? mOnSecondaryContainer : mOnSurface;
        int fromSub = focused ? mOnSurfaceVariant : ColorUtils.setAlphaComponent(mOnSecondaryContainer, 200);
        int toSub = focused ? ColorUtils.setAlphaComponent(mOnSecondaryContainer, 200) : mOnSurfaceVariant;

        M3Motion.applyCardFocusTransition(
                this,
                focused,
                fromBg,
                toBg,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                mOuterRadiusPx,
                mSegmentedRadii,
                (fraction, currentFill) -> {
                    mTitleView.setTextColor(ColorUtils.blendARGB(fromTitle, toTitle, fraction));
                    mSubtitleView.setTextColor(ColorUtils.blendARGB(fromSub, toSub, fraction));
                    if (mIconView.getVisibility() == VISIBLE) {
                        M3Motion.setImageTint(mIconView, ColorUtils.blendARGB(mOnSurfaceVariant, mOnSecondaryContainer, focused ? fraction : 1f - fraction));
                    }
                    if (mTrailingIcon.getVisibility() == VISIBLE) {
                        int restingColor = mTrailingAccentTint ? mPrimary : mOnSurfaceVariant;
                        M3Motion.setImageTint(mTrailingIcon, ColorUtils.blendARGB(restingColor, mOnSecondaryContainer, focused ? fraction : 1f - fraction));
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        M3Motion.cancelFocusAnimations(this);
        super.onDetachedFromWindow();
    }
}
