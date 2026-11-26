package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.leanback.app.BackgroundManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper.M3Palette;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class UriBackgroundManager {
    private static final String TAG = UriBackgroundManager.class.getSimpleName();
    private static final int BACKGROUND_UPDATE_DELAY_MS = 300;
    private static final int AMBIENT_UPDATE_DELAY_MS = 260;
    private Uri mBackgroundURI;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Runnable mAmbientTask;
    private String mPendingAmbientUrl;
    private String mCurrentAmbientUrl;
    private BackgroundManager mBackgroundManager;
    private final Activity mActivity;
    private final Handler mHandler;
    private int mBackgroundColor = -1;

    public UriBackgroundManager(Activity activity) {
        mActivity = activity;
        mHandler = new Handler();
        prepareBackgroundManager();
        setDefaultBackground();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(mActivity);
        mBackgroundManager.attach(mActivity.getWindow());
        M3Palette palette = ThemePaletteHelper.getActivePalette(mActivity);
        if (palette != null && palette.overrideSurfaces) {
            mDefaultBackground = new ColorDrawable(palette.surface);
        } else {
            mDefaultBackground = ContextCompat.getDrawable(mActivity, Helpers.getThemeAttr(mActivity, R.attr.shelfBackground));
        }
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void startBackgroundTimer(Uri backgroundURI) {
        mBackgroundURI = backgroundURI;
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY_MS);
    }

    public void startBackgroundTimer(String bgImageUrl) {
        if (bgImageUrl != null) {
            startBackgroundTimer(Uri.parse(bgImageUrl));
        }
    }

    public void setBackgroundFrom(@Nullable Video item) {
        if (mBackgroundManager == null || mActivity == null || mActivity.isFinishing()) {
            return;
        }
        String imageUrl = item != null
                ? (!TextUtils.isEmpty(item.bgImageUrl) ? item.bgImageUrl : item.getCardImageUrl())
                : null;
        if (TextUtils.isEmpty(imageUrl)) {
            mPendingAmbientUrl = null;
            if (mAmbientTask != null) {
                mHandler.removeCallbacks(mAmbientTask);
            }
            if (mCurrentAmbientUrl != null) {
                mCurrentAmbientUrl = null;
                setDefaultBackground();
            }
            return;
        }
        if (TextUtils.equals(imageUrl, mCurrentAmbientUrl) || TextUtils.equals(imageUrl, mPendingAmbientUrl)) {
            return;
        }
        mPendingAmbientUrl = imageUrl;
        if (mAmbientTask != null) {
            mHandler.removeCallbacks(mAmbientTask);
        }
        mAmbientTask = () -> applyAmbientBackdrop(imageUrl);
        mHandler.postDelayed(mAmbientTask, AMBIENT_UPDATE_DELAY_MS);
    }

    private void applyAmbientBackdrop(String imageUrl) {
        if (mActivity == null || mActivity.isFinishing() || mBackgroundManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 17 && mActivity.isDestroyed()) {
            return;
        }
        if (mActivity.findViewById(R.id.video_surface) != null) {
            return;
        }

        Glide.with(mActivity.getApplicationContext())
                .asBitmap()
                .load(imageUrl)
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .skipMemoryCache(false)
                        .override(160, 90)
                        .centerCrop())
                .into(new SimpleTarget<Bitmap>(160, 90) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (mBackgroundManager == null || mActivity == null || mActivity.isFinishing()) {
                            return;
                        }
                        if (Build.VERSION.SDK_INT >= 17 && mActivity.isDestroyed()) {
                            return;
                        }
                        if (!TextUtils.equals(imageUrl, mPendingAmbientUrl)) {
                            return;
                        }
                        mCurrentAmbientUrl = imageUrl;
                        int accentColor = extractAmbientColor(resource);
                        mBackgroundManager.setDrawable(buildAmbientDrawable(accentColor));
                    }
                });
    }

    private int extractAmbientColor(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return ViewUtil.getThemeColor(mActivity, R.attr.m3Primary, R.color.m3_grey_primary);
        }

        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        long weightSum = 0;
        float[] hsl = new float[3];

        int stepX = Math.max(1, width / 16);
        int stepY = Math.max(1, height / 9);
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int px = bitmap.getPixel(x, y);
                ColorUtils.colorToHSL(px, hsl);
                // Prefer colorful mid-tone pixels over pure shadows or blown highlights
                if (hsl[2] < 0.08f || hsl[2] > 0.92f) {
                    continue;
                }
                int weight = Math.max(1, Math.round(hsl[1] * 10f));
                sumR += (long) Color.red(px) * weight;
                sumG += (long) Color.green(px) * weight;
                sumB += (long) Color.blue(px) * weight;
                weightSum += weight;
            }
        }

        if (weightSum == 0) {
            return ViewUtil.getThemeColor(mActivity, R.attr.m3Primary, R.color.m3_grey_primary);
        }
        int avg = Color.rgb(
                (int) (sumR / weightSum),
                (int) (sumG / weightSum),
                (int) (sumB / weightSum));
        ColorUtils.colorToHSL(avg, hsl);
        hsl[1] = Math.min(0.85f, Math.max(0.32f, hsl[1]));
        hsl[2] = Math.min(0.62f, Math.max(0.28f, hsl[2]));
        return ColorUtils.HSLToColor(hsl);
    }

    private Drawable buildAmbientDrawable(int accentColor) {
        int baseSurface = ViewUtil.getThemeColor(mActivity, R.attr.m3Surface, R.color.m3_grey_surface);
        int themePrimary = ViewUtil.getThemeColor(mActivity, R.attr.m3Primary, R.color.m3_grey_primary);
        boolean isOled = MainUIData.instance(mActivity).isOledScheme();

        ColorDrawable baseLayer = new ColorDrawable(baseSurface);

        int glowAlpha = isOled ? 0x26 : 0x4C;
        int midGlowAlpha = isOled ? 0x10 : 0x22;
        GradientDrawable rightGlow = new GradientDrawable(
                GradientDrawable.Orientation.TR_BL,
                new int[]{
                        ColorUtils.setAlphaComponent(accentColor, glowAlpha),
                        ColorUtils.setAlphaComponent(accentColor, midGlowAlpha),
                        Color.TRANSPARENT
                });
        rightGlow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        rightGlow.setGradientCenter(0.82f, 0.14f);
        rightGlow.setGradientRadius(Math.max(mMetrics.widthPixels, mMetrics.heightPixels) * 0.78f);

        int subtleThemeAlpha = isOled ? 0x10 : 0x1A;
        GradientDrawable leftUndertone = new GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                new int[]{
                        ColorUtils.setAlphaComponent(themePrimary, subtleThemeAlpha),
                        Color.TRANSPARENT
                });

        return new LayerDrawable(new Drawable[]{baseLayer, leftUndertone, rightGlow});
    }

    public void onStart() {
        if (mBackgroundURI != null) {
            showBackground(mBackgroundURI.toString());
        } else if (mBackgroundColor != -1) {
            setBackgroundColor(mBackgroundColor);
        } else {
            setDefaultBackground();
        }
    }

    public void onStop() {
        mBackgroundManager.release();
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        if (mAmbientTask != null) {
            mHandler.removeCallbacks(mAmbientTask);
        }
        mBackgroundManager = null;
    }

    public void removeBackground() {
        mBackgroundManager.setDrawable(null);
    }

    public void setDefaultBackground() {
        mBackgroundManager.setDrawable(mDefaultBackground);
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundManager.setColor(color);
    }

    private class UpdateBackgroundTask implements Runnable {
        @Override
        public void run() {
            if (mBackgroundURI != null) {
                showBackground(mBackgroundURI.toString());
            }
        }
    }

    public BackgroundManager getBackgroundManager() {
        return mBackgroundManager;
    }

    public void showBackground(String uri) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(uri == null ? View.VISIBLE : View.INVISIBLE);
        }

        if (uri == null) {
            removeBackground();
            return;
        }

        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = ViewUtil.glideOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(mActivity)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            @NonNull Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    public void showBackgroundColor(int colorResId) {
        try {
            showBackgroundColorInternal(colorResId);
        } catch (NullPointerException e) {
            // Likely caused by OOM in View.setVisibility
            Log.w(TAG, "showBackgroundColor: NPE swallowed", e);
        }
    }

    private void showBackgroundColorInternal(int colorResId) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(colorResId == -1 ? View.VISIBLE : View.INVISIBLE);
        }

        if (colorResId == -1) {
            removeBackground();
            return;
        }

        if (mBackgroundManager != null) {
            mBackgroundManager.setColor(ContextCompat.getColor(mActivity, colorResId));
        }
    }
}
