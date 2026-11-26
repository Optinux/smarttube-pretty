package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.leanback.widget.Presenter;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class SearchFieldPresenter extends Presenter {
    private static final String TAG = SearchFieldPresenter.class.getSimpleName();
    private int mWidth;
    private int mHeight;

    public static class SearchFieldCallback {
        private String mText;

        public void onTextChanged(String text) {
            mText = text;
        }

        public String getText() {
            return mText;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        updateDimensions(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View contentView = inflater.inflate(R.layout.search_field, parent, false);

        EditText editField = contentView.findViewById(R.id.simple_edit_value);
        ViewUtil.setDimensions(editField, mWidth, -1); // don't do auto height

        int bgUnfocused = ViewUtil.getThemeColor(
                context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int bgFocused = ViewUtil.getThemeColor(
                context, R.attr.m3SurfaceContainerHighest, R.color.m3_grey_surface_container_highest);
        int strokeUnfocused = ViewUtil.getThemeColor(
                context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int strokeFocused = ViewUtil.getThemeColor(
                context, R.attr.m3Outline, R.color.m3_grey_outline);
        int textColor = ViewUtil.getThemeColor(
                context, R.attr.m3OnSurface, R.color.m3_on_surface);
        int hintColor = ViewUtil.getThemeColor(
                context, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);

        float pillRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        int hairlineStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);
        int focusStrokePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2f, context.getResources().getDisplayMetrics()));

        M3Motion.cancelFocusAnimations(editField);
        M3Motion.applyCardFocusTransition(
                editField,
                false,
                bgFocused,
                bgUnfocused,
                strokeFocused,
                strokeUnfocused,
                focusStrokePx,
                hairlineStrokePx,
                pillRadius,
                null);
        editField.setTextColor(textColor);
        editField.setHintTextColor(hintColor);
        ViewUtil.applyRoundedOutline(editField, pillRadius);

        editField.setOnFocusChangeListener((v, hasFocus) -> {
            M3Motion.applyCardFocusTransition(
                    editField,
                    hasFocus,
                    hasFocus ? bgUnfocused : bgFocused,
                    hasFocus ? bgFocused : bgUnfocused,
                    hasFocus ? strokeUnfocused : strokeFocused,
                    hasFocus ? strokeFocused : strokeUnfocused,
                    hasFocus ? hairlineStrokePx : focusStrokePx,
                    hasFocus ? focusStrokePx : hairlineStrokePx,
                    pillRadius,
                    null);
            M3Motion.animateFocusScale(editField, hasFocus, 1.015f);
        });

        contentView.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "On edit field focused");
            if (hasFocus) {
                editField.requestFocus();
            }
        });

        editField.setOnClickListener(v -> {
            Log.d(TAG, "On click");
            Helpers.showKeyboardAlt(v.getContext(), v);
        });

        return new ViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        SearchFieldCallback callback = (SearchFieldCallback) item;
        EditText editField = viewHolder.view.findViewById(R.id.simple_edit_value);
        if (editField.getTag() != null) {
            editField.removeTextChangedListener((TextWatcher) editField.getTag());
        }
        editField.setText(callback.getText());
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                callback.onTextChanged(s.toString());
            }
        };
        editField.addTextChangedListener(watcher);
        editField.setTag(watcher);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }

    private Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(
                context, R.dimen.channel_card_width,
                R.dimen.channel_card_height,
                MainUIData.instance(context).getVideoGridScale(),
                true);
    }
}
