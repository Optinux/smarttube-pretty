package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.guided;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionsStylist;
import androidx.leanback.widget.VerticalGridView;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Material 3 Expressive GuidedActionsStylist for TV onboarding/sign-in action lists.
 * Replaces legacy rectangular Leanback selector bars with rounded tonal action pills
 * and smooth M3 focus transitions while preserving Leanback click/focus listeners.
 */
public class M3GuidedActionsStylist extends GuidedActionsStylist {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View mainView = super.onCreateView(inflater, container);
        VerticalGridView grid = getActionsGridView();
        if (grid != null) {
            Context context = grid.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            int padH = Math.round(16 * density);
            int padV = Math.round(12 * density);
            grid.setPaddingRelative(padH, padV, padH, padV);
            grid.setClipToPadding(false);
            grid.setClipChildren(false);
            grid.setVerticalSpacing(Math.round(8 * density));
        }
        return mainView;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, GuidedAction action) {
        super.onBindViewHolder(vh, action);

        final View itemView = vh.itemView;
        if (itemView == null) {
            return;
        }
        if (VERSION.SDK_INT >= 23) {
            itemView.setForeground(null);
        }

        Context context = itemView.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        float cornerRadius = context.getResources().getDimension(R.dimen.m3_card_corner_radius);
        int focusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);

        int defaultBg = ViewUtil.getThemeColor(
                context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int focusedBg = ViewUtil.getThemeColor(
                context, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int outline = ViewUtil.getThemeColor(
                context, R.attr.m3Outline, R.color.m3_grey_outline);
        int defaultTitleColor = ViewUtil.getThemeColor(
                context, R.attr.m3OnSurface, R.color.m3_on_surface);
        int focusedTitleColor = ViewUtil.getThemeColor(
                context, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        int defaultDescColor = ViewUtil.getThemeColor(
                context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        int focusedDescColor = ColorUtils.setAlphaComponent(focusedTitleColor, 210);

        int padH = Math.round(18 * density);
        int padV = Math.round(14 * density);
        itemView.setPaddingRelative(padH, padV, padH, padV);
        ViewUtil.applyRoundedOutline(itemView, cornerRadius);

        final TextView titleView = vh.getTitleView();
        final TextView descView = vh.getDescriptionView();
        if (titleView != null) {
            titleView.setAlpha(action.isEnabled() ? 1.0f : 0.45f);
            titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
        if (descView != null) {
            descView.setAlpha(action.isEnabled() ? 1.0f : 0.45f);
            descView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        }

        boolean initiallyFocused = itemView.hasFocus();
        M3Motion.cancelFocusAnimations(itemView);
        applyActionColors(
                itemView,
                initiallyFocused,
                defaultBg,
                focusedBg,
                outline,
                focusStrokePx,
                cornerRadius,
                titleView,
                descView,
                defaultTitleColor,
                focusedTitleColor,
                defaultDescColor,
                focusedDescColor);
        M3Motion.animateFocusScale(itemView, initiallyFocused, 1.03f);

        Object prevObj = itemView.getTag(R.id.m3_focus_state_tag);
        // Preserve Leanback's own OnFocusChangeListener created before onBindViewHolder
        View.OnFocusChangeListener existing = itemView.getOnFocusChangeListener();
        if (itemView.getTag(R.id.m3_elevation_animator) == null && existing != null) {
            itemView.setTag(R.id.m3_elevation_animator, existing);
        }
        Object wrappedObj = itemView.getTag(R.id.m3_elevation_animator);
        final View.OnFocusChangeListener leanbackListener =
                wrappedObj instanceof View.OnFocusChangeListener
                        ? (View.OnFocusChangeListener) wrappedObj
                        : null;

        itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (leanbackListener != null) {
                leanbackListener.onFocusChange(v, hasFocus);
            }
            applyActionColors(
                    v,
                    hasFocus,
                    defaultBg,
                    focusedBg,
                    outline,
                    focusStrokePx,
                    cornerRadius,
                    titleView,
                    descView,
                    defaultTitleColor,
                    focusedTitleColor,
                    defaultDescColor,
                    focusedDescColor);
            M3Motion.animateFocusScale(v, hasFocus, 1.03f);
        });
    }

    private static void applyActionColors(
            View itemView,
            boolean focused,
            int defaultBg,
            int focusedBg,
            int outline,
            int focusStrokePx,
            float cornerRadius,
            TextView titleView,
            TextView descView,
            int defaultTitleColor,
            int focusedTitleColor,
            int defaultDescColor,
            int focusedDescColor) {
        int fromBg = focused ? defaultBg : focusedBg;
        int toBg = focused ? focusedBg : defaultBg;
        int fromStroke = focused ? Color.TRANSPARENT : outline;
        int toStroke = focused ? outline : Color.TRANSPARENT;
        int fromStrokePx = focused ? 0 : focusStrokePx;
        int toStrokePx = focused ? focusStrokePx : 0;
        int fromTitle = focused ? defaultTitleColor : focusedTitleColor;
        int toTitle = focused ? focusedTitleColor : defaultTitleColor;
        int fromDesc = focused ? defaultDescColor : focusedDescColor;
        int toDesc = focused ? focusedDescColor : defaultDescColor;

        M3Motion.applyCardFocusTransition(
                itemView,
                focused,
                fromBg,
                toBg,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                cornerRadius,
                (fraction, currentFill) -> {
                    if (titleView != null) {
                        titleView.setTextColor(ColorUtils.blendARGB(fromTitle, toTitle, fraction));
                    }
                    if (descView != null) {
                        descView.setTextColor(ColorUtils.blendARGB(fromDesc, toDesc, fraction));
                    }
                });
    }
}
