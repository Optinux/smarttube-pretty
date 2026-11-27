package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Material 3 Expressive horizontal filter / quick-jump chip strip optimized for TV D-pad navigation.
 * Supports optional color-dot indicators and shape-morphing active chips.
 */
public class M3ChipBarView extends HorizontalScrollView {
    public static final class ChipItem {
        public final int id;
        public final CharSequence label;
        public final int dotColor;
        public final boolean hasDot;

        public ChipItem(int id, CharSequence label) {
            this(id, label, 0, false);
        }

        public ChipItem(int id, CharSequence label, int dotColor) {
            this(id, label, dotColor, true);
        }

        private ChipItem(int id, CharSequence label, int dotColor, boolean hasDot) {
            this.id = id;
            this.label = label;
            this.dotColor = dotColor;
            this.hasDot = hasDot;
        }
    }

    public interface OnChipSelectedListener {
        void onChipSelected(int index, ChipItem item);
    }

    private LinearLayout mContainer;
    private final List<ChipItem> mItems = new ArrayList<>();
    private final List<LinearLayout> mChipViews = new ArrayList<>();
    private int mSelectedIndex = 0;
    private OnChipSelectedListener mListener;

    private int mSurfaceHigh;
    private int mPrimaryContainer;
    private int mSecondaryContainer;
    private int mOnPrimaryContainer;
    private int mOnSecondaryContainer;
    private int mOnSurfaceVariant;
    private int mOutline;
    private int mOutlineVariant;
    private float mPillRadiusPx;
    private float mSelectedRadiusPx;
    private int mFocusStrokePx;
    private int mHairlineStrokePx;

    public M3ChipBarView(Context context) {
        this(context, null);
    }

    public M3ChipBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public M3ChipBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        setClipChildren(false);
        setFocusable(false);

        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setGravity(Gravity.CENTER_VERTICAL);
        mContainer.setClipToPadding(false);
        mContainer.setClipChildren(false);
        addView(mContainer, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        resolveThemeColors();
    }

    public void setOnChipSelectedListener(@Nullable OnChipSelectedListener listener) {
        mListener = listener;
    }

    public void setChips(List<ChipItem> items, int selectedIndex) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        mSelectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, mItems.size() - 1)));
        rebuildChips();
    }

    public void setSelectedIndex(int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= mChipViews.size() || selectedIndex == mSelectedIndex) {
            return;
        }
        int prev = mSelectedIndex;
        mSelectedIndex = selectedIndex;
        updateChipVisual(prev, mChipViews.get(prev).hasFocus(), true);
        updateChipVisual(mSelectedIndex, mChipViews.get(mSelectedIndex).hasFocus(), true);
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public void refreshThemeColors() {
        resolveThemeColors();
        for (int i = 0; i < mChipViews.size(); i++) {
            updateChipVisual(i, mChipViews.get(i).hasFocus(), false);
        }
    }

    private void resolveThemeColors() {
        Context ctx = getContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        mSurfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        mPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        mSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        mOnPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        mOnSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
        mOnSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        mOutline = ViewUtil.getThemeColor(ctx, R.attr.m3Outline, R.color.m3_grey_outline);
        mOutlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        mPillRadiusPx = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        mSelectedRadiusPx = 12f * density;
        mFocusStrokePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        mHairlineStrokePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);
    }

    private void rebuildChips() {
        mContainer.removeAllViews();
        mChipViews.clear();
        Context ctx = getContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        int gap = Math.round(10 * density);
        int padH = Math.round(14 * density);
        int padV = Math.round(8 * density);
        int dotSize = Math.round(12 * density);

        for (int i = 0; i < mItems.size(); i++) {
            final int index = i;
            final ChipItem item = mItems.get(i);

            LinearLayout chip = new LinearLayout(ctx);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setFocusable(true);
            chip.setFocusableInTouchMode(true);
            chip.setClickable(true);
            chip.setMinimumHeight(Math.round(36 * density));
            chip.setPaddingRelative(padH, padV, padH, padV);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            if (i < mItems.size() - 1) {
                lp.setMarginEnd(gap);
            }

            if (item.hasDot) {
                View dot = new View(ctx);
                GradientDrawable dotDrawable = new GradientDrawable();
                dotDrawable.setShape(GradientDrawable.OVAL);
                dotDrawable.setColor(item.dotColor);
                dotDrawable.setStroke(Math.max(1, Math.round(1 * density)), ColorUtils.setAlphaComponent(Color.WHITE, 120));
                dot.setBackground(dotDrawable);
                LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
                dotLp.setMarginEnd(Math.round(8 * density));
                chip.addView(dot, dotLp);
            }

            TextView label = new TextView(ctx);
            label.setTag("label");
            label.setText(item.label);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            chip.addView(label, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            chip.setOnFocusChangeListener((v, hasFocus) -> {
                updateChipVisual(index, hasFocus, true);
                M3Motion.animateFocusScale(chip, hasFocus, M3Motion.FOCUS_SCALE_DEFAULT);
                if (hasFocus) {
                    smoothEnsureVisible(chip);
                }
            });

            chip.setOnClickListener(v -> {
                setSelectedIndex(index);
                if (mListener != null) {
                    mListener.onChipSelected(index, item);
                }
            });

            mChipViews.add(chip);
            mContainer.addView(chip, lp);
            updateChipVisual(index, false, false);
        }
    }

    private void smoothEnsureVisible(View child) {
        int left = child.getLeft() - getPaddingStart();
        int right = child.getRight() + getPaddingEnd();
        int scrollX = getScrollX();
        int width = getWidth();
        if (width <= 0) {
            return;
        }
        if (left < scrollX) {
            int targetX = Math.max(0, left - 24);
            if (M3Motion.areAnimationsEnabled(getContext())) {
                smoothScrollTo(targetX, 0);
            } else {
                scrollTo(targetX, 0);
            }
        } else if (right > scrollX + width) {
            int targetX = right - width + 24;
            if (M3Motion.areAnimationsEnabled(getContext())) {
                smoothScrollTo(targetX, 0);
            } else {
                scrollTo(targetX, 0);
            }
        }
    }

    private void updateChipVisual(int index, boolean focused, boolean animate) {
        if (index < 0 || index >= mChipViews.size()) {
            return;
        }
        LinearLayout chip = mChipViews.get(index);
        boolean selected = index == mSelectedIndex;
        if (!animate) {
            M3Motion.cancelFocusAnimations(chip);
        }

        int restingFill = selected ? mPrimaryContainer : ColorUtils.setAlphaComponent(mSurfaceHigh, 200);
        int focusedFill = selected ? mPrimaryContainer : mSecondaryContainer;
        int restingText = selected ? mOnPrimaryContainer : mOnSurfaceVariant;
        int focusedText = selected ? mOnPrimaryContainer : mOnSecondaryContainer;
        int restingStroke = selected ? Color.TRANSPARENT : mOutlineVariant;
        int focusedStroke = mOutline;

        int fromFill = focused ? restingFill : focusedFill;
        int toFill = focused ? focusedFill : restingFill;
        int fromStroke = focused ? restingStroke : focusedStroke;
        int toStroke = focused ? focusedStroke : restingStroke;
        int fromStrokePx = focused ? (selected ? 0 : mHairlineStrokePx) : mFocusStrokePx;
        int toStrokePx = focused ? mFocusStrokePx : (selected ? 0 : mHairlineStrokePx);
        int fromText = focused ? restingText : focusedText;
        int toText = focused ? focusedText : restingText;
        float radius = selected ? mSelectedRadiusPx : mPillRadiusPx;

        TextView label = chip.findViewWithTag("label");
        if (label != null) {
            label.setTypeface(Typeface.create(selected || focused ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        }

        if (animate) {
            // Reset focus tag so selection changes on an already-focused chip animate smoothly too.
            chip.setTag(R.id.m3_focus_state_tag, !focused);
        }
        float prevRadius = chip.getBackground() instanceof GradientDrawable && animate
                ? (selected ? mPillRadiusPx : mSelectedRadiusPx)
                : radius;
        if (animate && chip.getBackground() instanceof GradientDrawable) {
            M3Motion.animateCornerMorph(chip, (GradientDrawable) chip.getBackground(), prevRadius, radius);
        }

        M3Motion.applyCardFocusTransition(
                chip,
                focused,
                fromFill,
                toFill,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                animate && chip.getBackground() instanceof GradientDrawable ? -1f : radius,
                (fraction, currentFill) -> {
                    if (label != null) {
                        label.setTextColor(ColorUtils.blendARGB(fromText, toText, fraction));
                    }
                });
    }

    @Nullable
    public View getFirstChipView() {
        return mChipViews.isEmpty() ? null : mChipViews.get(0);
    }

    @Nullable
    public View getSelectedChipView() {
        if (mSelectedIndex >= 0 && mSelectedIndex < mChipViews.size()) {
            return mChipViews.get(mSelectedIndex);
        }
        return getFirstChipView();
    }
}
