package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class ChannelCardPresenter extends LongClickPresenter {
    private static final String TAG = ChannelCardPresenter.class.getSimpleName();
    private int mDefaultBackgroundColor;
    private int mDefaultTextColor;
    private int mSelectedBackgroundColor;
    private int mSelectedTextColor;
    private int mNewContentBackgroundColor;
    private int mNewContentTextColor;
    private int mPrimaryColor;
    private int mOutlineColor;
    private int mHairlineColor;
    private float mCornerRadius;
    private int mFocusStrokePx;
    private int mNewStrokePx;
    private int mHairlineStrokePx;
    private int mWidth;
    private int mHeight;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
        mSelectedBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        mNewContentBackgroundColor =
                ViewUtil.getThemeColor(context, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        mDefaultTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        mSelectedTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnSecondaryContainer, R.color.m3_on_surface);
        mNewContentTextColor =
                ViewUtil.getThemeColor(context, R.attr.m3OnPrimaryContainer, R.color.m3_on_surface);
        mPrimaryColor =
                ViewUtil.getThemeColor(context, R.attr.m3Primary, R.color.m3_grey_primary);
        mOutlineColor =
                ViewUtil.getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        mHairlineColor =
                ViewUtil.getThemeColor(context, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);

        mCornerRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        mFocusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
        mNewStrokePx = 0;
        mHairlineStrokePx = 0;

        updateDimensions(context);

        @SuppressLint("InflateParams")
        FrameLayout container = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.channel_card, null);
        ViewUtil.applyRoundedOutline(container, mCornerRadius);

        ImageView imageView = container.findViewById(R.id.channel_image);
        float avatarRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        ViewUtil.applyRoundedOutline(imageView, avatarRadius);

        View newDot = container.findViewById(R.id.channel_new_indicator);
        if (newDot != null) {
            float dotRadius = context.getResources().getDimension(R.dimen.m3_pill_corner_radius);
            newDot.setBackground(ViewUtil.createRoundedDrawable(mPrimaryColor, 0, 0, dotRadius));
        }

        TextView textView = container.findViewById(R.id.channel_title);
        updateCardStyle(container, textView, false, false, false);

        boolean autoScrollEnabled = isCardTextAutoScrollEnabled(context);
        if (autoScrollEnabled) {
            ViewUtil.setTextScrollSpeed(textView, getCardTextScrollSpeed(context));
        }

        container.setOnFocusChangeListener((v, hasFocus) -> {
            boolean hasNew = textView.getTag(R.id.channel_new_content) != null;
            updateCardStyle(container, textView, hasFocus, hasNew, true);

            if (!autoScrollEnabled) {
                return;
            }

            if (hasFocus) {
                ViewUtil.enableMarquee(textView);
            } else {
                ViewUtil.disableMarquee(textView);
            }
        });

        return new ViewHolder(container);
    }

    private void updateCardStyle(FrameLayout container, TextView textView, boolean hasFocus, boolean hasNew, boolean allowAnimation) {
        if (!allowAnimation) {
            M3Motion.cancelFocusAnimations(container);
        }
        int unfocusedBg = hasNew ? mNewContentBackgroundColor : mDefaultBackgroundColor;
        int unfocusedText = hasNew ? mNewContentTextColor : mDefaultTextColor;
        int unfocusedStrokeColor = hasNew ? mPrimaryColor : mHairlineColor;
        int unfocusedStrokeWidth = hasNew ? mNewStrokePx : mHairlineStrokePx;

        int fromBg = hasFocus ? unfocusedBg : mSelectedBackgroundColor;
        int toBg = hasFocus ? mSelectedBackgroundColor : unfocusedBg;
        int fromStroke = hasFocus ? unfocusedStrokeColor : mOutlineColor;
        int toStroke = hasFocus ? mOutlineColor : unfocusedStrokeColor;
        int fromStrokePx = hasFocus ? unfocusedStrokeWidth : mFocusStrokePx;
        int toStrokePx = hasFocus ? mFocusStrokePx : unfocusedStrokeWidth;
        int fromText = hasFocus ? unfocusedText : mSelectedTextColor;
        int toText = hasFocus ? mSelectedTextColor : unfocusedText;

        M3Motion.applyCardFocusTransition(
                container,
                hasFocus,
                fromBg,
                toBg,
                fromStroke,
                toStroke,
                fromStrokePx,
                toStrokePx,
                mCornerRadius,
                (fraction, currentFill) -> textView.setTextColor(ColorUtils.blendARGB(fromText, toText, fraction)));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Context context = viewHolder.view.getContext();
        Video video = (Video) item;

        ViewUtil.setDimensions(viewHolder.view.findViewById(R.id.channel_card_wrapper), mWidth, -1); // don't do auto height

        TextView textView = viewHolder.view.findViewById(R.id.channel_title);
        textView.setText(video.getTitle());

        // We should setup props each time because object may be reused by the underlying RecyclerView
        textView.setTag(R.id.channel_new_content, video.hasNewContent ? true : null);
        updateCardStyle((FrameLayout) viewHolder.view, textView, viewHolder.view.hasFocus(), video.hasNewContent, false);

        View newDot = viewHolder.view.findViewById(R.id.channel_new_indicator);
        if (newDot != null) {
            newDot.setVisibility(video.hasNewContent ? View.VISIBLE : View.GONE);
        }

        ImageView imageView = viewHolder.view.findViewById(R.id.channel_image);
        imageView.setVisibility(View.VISIBLE);

        Glide.with(context)
                .load(video.cardImageUrl)
                .apply(ViewUtil.glideOptions())
                .listener(mErrorListener)
                //.error(R.drawable.card_placeholder) // R.color.lb_grey
                .into(imageView);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        M3Motion.cancelFocusAnimations(viewHolder.view);
        // Remove references to images so that the garbage collector can free up memory.
        ImageView imageView = viewHolder.view.findViewById(R.id.channel_image);
        imageView.setImageDrawable(null);
    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }

    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(
                context, R.dimen.channel_card_width,
                R.dimen.channel_card_height,
                MainUIData.instance(context).getVideoGridScale(),
                true);
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
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
