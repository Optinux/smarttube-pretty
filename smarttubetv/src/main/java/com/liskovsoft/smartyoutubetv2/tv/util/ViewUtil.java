package com.liskovsoft.smartyoutubetv2.tv.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build.VERSION;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper.M3Palette;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.adapter.VideoGroupObjectAdapter;
import com.stfalcon.chatkit.messages.MessagesList;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.marqueetextview.MarqueeTextView;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.marqueetextviewcompat.MarqueeTextViewCompat;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.speedmarquee.SpeedMarquee;

public class ViewUtil {
    /**
     * Focused card zoom factor
     */
    public static final int FOCUS_ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
    //public static final int FOCUS_ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_NONE;
    /**
     * Dim focused card?
     */
    public static final boolean FOCUS_DIMMER_ENABLED = false;
    /**
     * Dim other rows in {@link RowPresenter}
     */
    public static final boolean ROW_SELECT_EFFECT_ENABLED = false;
    /**
     * Scroll continue threshold
     */
    public static final int GRID_SCROLL_CONTINUE_NUM = 10;
    public static final int ROW_SCROLL_CONTINUE_NUM = 4;

    /**
     * Checks whether text is truncated (e.g. has ... at the end)
     */
    public static boolean isTruncated(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            int lines = layout.getLineCount();
            if (lines > 0) {
                int ellipsisCount = layout.getEllipsisCount(lines - 1);
                if (ellipsisCount > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void disableMarquee(TextView... textViews) {
        if (VERSION.SDK_INT <= 19 || textViews == null) { // Android 4: Broken grid layout fix
            return;
        }

        for (TextView textView : textViews) {
            textView.setEllipsize(TruncateAt.END);
            // Line below cause broken grid layout on Android 4 and older
            textView.setHorizontallyScrolling(false);

            applyMarqueeRtlParams(textView, false);
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/3332924/textview-marquee-not-working">More info</a>
     */
    public static void enableMarquee(TextView... textViews) {
        if (VERSION.SDK_INT <= 19 || textViews == null) { // Android 4: Broken grid layout fix
            return;
        }

        for (TextView textView : textViews) {
            if (ViewUtil.isTruncated(textView)) { // multiline scroll fix
                textView.setEllipsize(TruncateAt.MARQUEE);
                textView.setMarqueeRepeatLimit(-1);
                textView.setHorizontallyScrolling(true);

                // App dialog title fix.
                //textView.setSelected(true);

                applyMarqueeRtlParams(textView, true);
            }
        }
    }

    public static void applyMarqueeRtlParams(TextView textView, boolean scroll) {
        if (!Helpers.isTextRTL(textView.getText())) {
            // TextView may be reused from rtl context. Do reset.
            // NOTE: don't enable commented options because Setting item's text won't be centered.
            //textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            textView.setTextDirection(View.TEXT_DIRECTION_LTR);
            textView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            //textView.setGravity(Gravity.TOP | Gravity.START);
            return;
        }

        if (scroll) {
            // Fix: right scrolling on rtl languages
            // Fix: text disappear on rtl languages
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            textView.setTextDirection(View.TEXT_DIRECTION_RTL);
            textView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            textView.setGravity(Gravity.START);
        } else {
            // Fix: text disappear on rtl languages
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
    }

    public static void setTextScrollSpeed(TextView textView, float speed) {
        if (VERSION.SDK_INT <= 19) { // Android 4: Broken grid layout fix
            return;
        }

        if (textView instanceof MarqueeTextViewCompat) {
            ((MarqueeTextViewCompat) textView).setMarqueeSpeedFactor(speed);
        } else if (textView instanceof MarqueeTextView) {
            ((MarqueeTextView) textView).setMarqueeSpeedFactor(speed);
        } else if (textView instanceof SpeedMarquee) {
            ((SpeedMarquee) textView).setSpeed(speed);
        }
    }

    public static void enableView(View view, boolean enabled) {
        if (view != null) {
            view.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }

    public static void setDimensions(View view, int width, int height) {
        if (view != null) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();

            if (lp != null) {
                if (width > 0) {
                    lp.width = width;
                }
                if (height > 0) {
                    lp.height = height;
                }
                view.setLayoutParams(lp);
            }
        }
    }

    public static boolean isListRowEmpty(Object obj) {
        if (obj instanceof ListRow) {
            ListRow row = (ListRow) obj;
            VideoGroupObjectAdapter adapter = (VideoGroupObjectAdapter) row.getAdapter();
            return adapter == null || adapter.isEmpty();
        }

        return true;
    }

    public static RequestOptions glideOptions() {
        return new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE) // ensure start animation from beginning
                .skipMemoryCache(true); // ensure start animation from beginning
    }

    public static void enableTransparentDialog(Context context, View rootView) {
        if (context == null || rootView == null || VERSION.SDK_INT <= 19) {
            return;
        }

        // Usually null. Present only on parent fragment.
        View mainContainer = rootView.findViewById(R.id.settings_preference_fragment_container);
        View mainFrame = rootView.findViewById(R.id.main_frame);
        View itemsContainer = rootView.findViewById(R.id.list);
        View title = rootView.findViewById(R.id.decor_title_container);
        int transparent = ContextCompat.getColor(context, R.color.transparent);
        int semiTransparent = ContextCompat.getColor(context, R.color.semi_grey);

        // Disable shadow outline and dim backdrop on parent fragment
        if (mainContainer instanceof FrameLayout && VERSION.SDK_INT >= 21) {
            // ViewOutlineProvider: NoClassDefFoundError on API 19
            rootView.setBackgroundColor(transparent);
            mainContainer.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            if (VERSION.SDK_INT >= 23) {
                mainContainer.setForeground(null);
            }
        }
        if (mainFrame instanceof LinearLayout) {
            mainFrame.setBackgroundColor(transparent);
        }
        if (itemsContainer instanceof VerticalGridView) {
            float itemCornerRadius = context.getResources().getDimension(R.dimen.m3_card_small_corner_radius);
            int focusStrokePx = context.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);
            int restingFill = androidx.core.graphics.ColorUtils.setAlphaComponent(
                    getThemeColor(context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high), 140);
            int focusedFill = androidx.core.graphics.ColorUtils.setAlphaComponent(
                    getThemeColor(context, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container), 235);
            int focusedStroke = getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
            ((VerticalGridView) itemsContainer).setVerticalSpacing(
                    Math.round(3f * context.getResources().getDisplayMetrics().density));
            // Set background for individual buttons in the list.
            // This is the only way to do this because items haven't been added yet to the container.
            ((VerticalGridView) itemsContainer).setOnChildLaidOutListener((parent, view, position, id) -> {
                if (view == null) {
                    return;
                }
                boolean initiallyFocused = view.hasFocus();
                M3Motion.cancelFocusAnimations(view);
                M3Motion.applyCardFocusTransition(
                        view,
                        initiallyFocused,
                        initiallyFocused ? restingFill : focusedFill,
                        initiallyFocused ? focusedFill : restingFill,
                        initiallyFocused ? transparent : focusedStroke,
                        initiallyFocused ? focusedStroke : transparent,
                        initiallyFocused ? 0 : focusStrokePx,
                        initiallyFocused ? focusStrokePx : 0,
                        itemCornerRadius,
                        null);
                View.OnFocusChangeListener prev = view.getOnFocusChangeListener();
                view.setOnFocusChangeListener((v, hasFocus) -> {
                    if (prev != null) {
                        prev.onFocusChange(v, hasFocus);
                    }
                    M3Motion.applyCardFocusTransition(
                            v,
                            hasFocus,
                            hasFocus ? restingFill : focusedFill,
                            hasFocus ? focusedFill : restingFill,
                            hasFocus ? transparent : focusedStroke,
                            hasFocus ? focusedStroke : transparent,
                            hasFocus ? 0 : focusStrokePx,
                            hasFocus ? focusStrokePx : 0,
                            itemCornerRadius,
                            null);
                    M3Motion.animateFocusScale(v, hasFocus, 1.015f);
                });
            });
        }
        if (title instanceof FrameLayout) {
            title.setBackgroundColor(transparent);
            title.setVisibility(View.GONE);
        }
    }

    public static void enableLeftDialog(Context context, View rootView) {
        if (context == null || rootView == null || VERSION.SDK_INT <= 19) {
            return;
        }

        // Usually null. Present only on parent fragment.
        View mainContainer = rootView.findViewById(R.id.settings_preference_fragment_container);

        if (mainContainer instanceof FrameLayout) {
            ((FrameLayout.LayoutParams) mainContainer.getLayoutParams()).gravity = Gravity.START;
        }
    }

    public static void makeMonochrome(ImageView iconView) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        iconView.setColorFilter(filter);
    }

    public static void setGravity(View view, int gravity) {
        if (view == null) {
            return;
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
            flp.gravity = gravity;
            view.setLayoutParams(flp);
        }
    }

    public static void setWidth(View view, int width) {
        if (view == null) {
            return;
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            lp.width = width;
            view.setLayoutParams(lp);
        }
    }

    public static void setPadding(View view, int padding) {
        if (view == null) {
            return;
        }

        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
    }

    /**
     * Fix SDK 28+ GridLayoutManager broken navigation when using Japanese fonts
     */
    public static void fixApi28BrokenGridNavigation(TextView textView) {
        if (VERSION.SDK_INT >= 28) {
            // 1. Disable dynamic line spacing for special characters (prevents expansion for CJK glyphs)
            textView.setFallbackLineSpacing(false);
        }

        // 2. Remove system font padding to ensure consistent baseline and height
        textView.setIncludeFontPadding(false);

        // 3. Add fixed internal padding to create a "safe zone" for both Latin and Japanese text
        int paddingExtra = (int) (4 * textView.getResources().getDisplayMetrics().density);
        textView.setPadding(textView.getPaddingLeft(), paddingExtra, textView.getPaddingRight(), paddingExtra);
    }

    /**
     * Resolves a theme color attribute safely with Material You / custom accent override and fallback color resource.
     */
    public static int getThemeColor(Context context, int attrResId, int fallbackColorResId) {
        if (context == null) {
            return Color.TRANSPARENT;
        }
        Integer dynamicColor = resolveDynamicThemeColor(context, attrResId);
        if (dynamicColor != null) {
            return dynamicColor;
        }
        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrResId, outValue, true)) {
            if (outValue.resourceId != 0) {
                return ContextCompat.getColor(context, outValue.resourceId);
            }
            return outValue.data;
        }
        return ContextCompat.getColor(context, fallbackColorResId);
    }

    private static Integer resolveDynamicThemeColor(Context context, int attrResId) {
        M3Palette palette = ThemePaletteHelper.getActivePalette(context);
        if (palette == null) {
            return null;
        }
        if (attrResId == R.attr.m3Primary) return palette.primary;
        if (attrResId == R.attr.m3OnPrimary) return palette.onPrimary;
        if (attrResId == R.attr.m3PrimaryContainer || attrResId == R.attr.brandAccentColor
                || attrResId == R.attr.defaultSearchBrightColor) return palette.primaryContainer;
        if (attrResId == R.attr.m3OnPrimaryContainer) return palette.onPrimaryContainer;
        if (attrResId == R.attr.m3SecondaryContainer || attrResId == R.attr.cardSelectedBackground) return palette.secondaryContainer;
        if (attrResId == R.attr.m3OnSecondaryContainer) return palette.onSecondaryContainer;
        if (attrResId == R.attr.m3Outline) return palette.outline;
        if (attrResId == R.attr.m3Error) return palette.error;
        if (attrResId == R.attr.m3OnError) return palette.onError;
        if (attrResId == R.attr.m3ErrorContainer) return palette.errorContainer;
        if (attrResId == R.attr.m3OnErrorContainer) return palette.onErrorContainer;
        if (attrResId == R.attr.m3OrbFocusedBackground) return palette.orbFocusedBackground;
        if (attrResId == R.attr.playbackProgressPrimaryColor) return palette.progressPrimary;
        if (attrResId == R.attr.playbackControlsIconHighlightColor) return palette.primary;

        if (!palette.overrideSurfaces) {
            return null;
        }
        if (attrResId == R.attr.m3Surface || attrResId == R.attr.shelfBackground) return palette.surface;
        if (attrResId == R.attr.m3SurfaceContainerLow || attrResId == R.attr.brandColor
                || attrResId == R.attr.defaultBrandColor) return palette.surfaceContainerLow;
        if (attrResId == R.attr.m3SurfaceContainer || attrResId == R.attr.cardDefaultBackground) return palette.surfaceContainer;
        if (attrResId == R.attr.m3SurfaceContainerHigh || attrResId == R.attr.defaultSearchColor
                || attrResId == R.attr.m3OrbBackground) return palette.surfaceContainerHigh;
        if (attrResId == R.attr.m3SurfaceContainerHighest) return palette.surfaceContainerHighest;
        if (attrResId == R.attr.m3OnSurface || attrResId == R.attr.m3OrbIconColor) return palette.onSurface;
        if (attrResId == R.attr.m3OnSurfaceVariant) return palette.onSurfaceVariant;
        if (attrResId == R.attr.m3OutlineVariant) return palette.outlineVariant;
        return null;
    }

    /**
     * Creates a rounded Material 3 surface/pill drawable with optional stroke.
     */
    public static GradientDrawable createRoundedDrawable(int fillColor, int strokeColor, int strokeWidthPx, float cornerRadiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        if (cornerRadiusPx > 0) {
            drawable.setCornerRadius(cornerRadiusPx);
        }
        if (strokeWidthPx > 0) {
            drawable.setStroke(strokeWidthPx, strokeColor);
        }
        return drawable;
    }

    /**
     * Creates a focus/selected state-list rounded drawable for M3 Expressive TV items.
     */
    public static Drawable createFocusSelectorDrawable(
            int defaultFillColor,
            int focusedFillColor,
            int defaultStrokeColor,
            int focusedStrokeColor,
            int defaultStrokeWidthPx,
            int focusedStrokeWidthPx,
            float cornerRadiusPx) {
        StateListDrawable states = new StateListDrawable();
        Drawable focused = createRoundedDrawable(focusedFillColor, focusedStrokeColor, focusedStrokeWidthPx, cornerRadiusPx);
        Drawable normal = createRoundedDrawable(defaultFillColor, defaultStrokeColor, defaultStrokeWidthPx, cornerRadiusPx);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_selected}, focused);
        states.addState(new int[]{android.R.attr.state_activated}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    /**
     * Applies rounded outline clipping on API 21+ without breaking older APIs.
     */
    public static void applyRoundedOutline(View view, float cornerRadiusPx) {
        if (view == null || VERSION.SDK_INT < 21) {
            return;
        }
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), cornerRadiusPx);
            }
        });
        view.setClipToOutline(true);
    }

    /**
     * Updates a FrameLayout-based card's M3 Expressive focus ring foreground.
     */
    public static void updateCardFocusForeground(FrameLayout cardView, boolean focused, int outlineColor, int hairlineColor, int focusStrokePx, int hairlineStrokePx, float cornerRadiusPx) {
        if (cardView == null) {
            return;
        }
        if (focused) {
            cardView.setForeground(createRoundedDrawable(Color.TRANSPARENT, outlineColor, focusStrokePx, cornerRadiusPx));
        } else if (hairlineStrokePx > 0) {
            cardView.setForeground(createRoundedDrawable(Color.TRANSPARENT, hairlineColor, hairlineStrokePx, cornerRadiusPx));
        } else {
            cardView.setForeground(null);
        }
    }

    /**
     * Creates a ColorStateList switching between focused/selected and default text/icon colors.
     */
    public static ColorStateList createFocusColorStateList(int defaultColor, int focusedColor) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_selected},
                new int[]{android.R.attr.state_activated},
                new int[]{}
        };
        int[] colors = new int[]{focusedColor, focusedColor, focusedColor, defaultColor};
        return new ColorStateList(states, colors);
    }

    /**
     * Applies runtime Material 3 Expressive surface, focus, and text colors to ChatKit message lists.
     */
    public static void applyM3MessagesListStyle(MessagesList messagesList, boolean translucentOverlay) {
        if (messagesList == null) {
            return;
        }
        Context context = messagesList.getContext();
        int surfaceHigh = getThemeColor(context, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int normalBg = translucentOverlay ? ColorUtils.setAlphaComponent(surfaceHigh, 195) : surfaceHigh;
        int secondaryContainer = getThemeColor(context, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        int focusStroke = getThemeColor(context, R.attr.m3Outline, R.color.m3_grey_outline);
        int onSurface = getThemeColor(context, R.attr.m3OnSurface, R.color.m3_on_surface);
        int primary = getThemeColor(context, R.attr.m3Primary, R.color.m3_grey_primary);
        messagesList.setIncomingM3Colors(normalBg, secondaryContainer, focusStroke, onSurface, primary);
    }
}
