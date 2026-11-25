package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.tv.R;
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

        int defaultBg = ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainer, R.color.card_default_background);
        int focusedBg = ViewUtil.getThemeColor(context, R.attr.m3PrimaryContainer, R.color.card_selected_background_white);
        int defaultStroke = ViewUtil.getThemeColor(context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int focusedStroke = ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        int defaultText = ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        int focusedText = ViewUtil.getThemeColor(context, R.attr.m3OnPrimaryContainer, R.color.m3_on_surface);

        float cornerRadius = res.getDimension(R.dimen.m3_card_corner_radius);
        int hairlineStrokePx = res.getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);
        int focusStrokePx = res.getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        int paddingPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, res.getDisplayMetrics()));

        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setBackground(ViewUtil.createFocusSelectorDrawable(
                defaultBg, focusedBg, defaultStroke, focusedStroke, hairlineStrokePx, focusStrokePx, cornerRadius));
        view.setTextColor(ViewUtil.createFocusColorStateList(defaultText, focusedText));
        ViewUtil.applyRoundedOutline(view, cornerRadius);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        view.setGravity(Gravity.CENTER);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText((String) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
