package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class GridItemPresenter extends Presenter {
    private final Fragment mainFragment;

    public GridItemPresenter(Fragment mainFragment) {
        this.mainFragment = mainFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        TextView view = new TextView(context);

        Resources res = parent.getResources();
        int width = res.getDimensionPixelSize(R.dimen.settings_card_width);
        int height = res.getDimensionPixelSize(R.dimen.settings_card_height);

        int defaultBg = ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
        int focusedBg = ViewUtil.getThemeColor(context, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int defaultStroke = 0;
        int focusedStroke = ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        int defaultText = ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        int focusedText = ViewUtil.getThemeColor(context, R.attr.m3OnPrimaryContainer, R.color.m3_on_surface);

        float cornerRadius = res.getDimension(R.dimen.m3_card_large_corner_radius);
        int hairlineStrokePx = 0;
        int focusStrokePx = res.getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        int paddingPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, res.getDisplayMetrics()));

        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        ViewUtil.applyRoundedOutline(view, cornerRadius);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        view.setGravity(Gravity.CENTER);

        applyItemStyle(view, false, false, defaultBg, focusedBg, defaultStroke, focusedStroke,
                hairlineStrokePx, focusStrokePx, defaultText, focusedText, cornerRadius);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            applyItemStyle(
                    view, hasFocus, true, defaultBg, focusedBg, defaultStroke, focusedStroke,
                    hairlineStrokePx, focusStrokePx, defaultText, focusedText, cornerRadius);
            M3Motion.animateFocusScale(view, hasFocus, M3Motion.FOCUS_SCALE_CARD);
        });
        return new ViewHolder(view);
    }

    private static void applyItemStyle(
            TextView view,
            boolean focused,
            boolean allowAnimation,
            int defaultBg,
            int focusedBg,
            int defaultStroke,
            int focusedStroke,
            int hairlineStrokePx,
            int focusStrokePx,
            int defaultText,
            int focusedText,
            float cornerRadius) {
        if (!allowAnimation) {
            M3Motion.cancelFocusAnimations(view);
        }
        int fromBg = focused ? defaultBg : focusedBg;
        int toBg = focused ? focusedBg : defaultBg;
        int fromStroke = focused ? defaultStroke : focusedStroke;
        int toStroke = focused ? focusedStroke : defaultStroke;
        int fromStrokePx = focused ? hairlineStrokePx : focusStrokePx;
        int toStrokePx = focused ? focusStrokePx : hairlineStrokePx;
        int fromText = focused ? defaultText : focusedText;
        int toText = focused ? focusedText : defaultText;

        M3Motion.applyCardFocusTransition(
                view,
                focused,
                fromBg,
                toBg,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                cornerRadius,
                (fraction, currentFill) -> view.setTextColor(ColorUtils.blendARGB(fromText, toText, fraction)));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText((String) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        M3Motion.resetOnUnbind(viewHolder.view);
    }
}
