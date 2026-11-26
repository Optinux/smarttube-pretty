package com.liskovsoft.smartyoutubetv2.common.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;

import com.liskovsoft.sharedutils.helpers.KeyHelpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper.M3Palette;

/**
 * D-pad and touch friendly Material 3 Expressive accent color picker dialog.
 * Supports curated color swatches, Hue/Saturation/Lightness sliders, live M3 preview, and Hex input.
 */
public final class AccentColorPickerDialog {
    private static final int LIGHTNESS_MIN = 20;

    private static final int[] QUICK_SWATCHES = {
            0xFFFF4E45, 0xFFFF897D, 0xFFFFB74D, 0xFFFFD54F, 0xFFAED581, 0xFF66BB6A,
            0xFF56D6E8, 0xFF64B5F6, 0xFF9ECAFF, 0xFF9FA8DA, 0xFFD0BCFF, 0xFFF48FB1
    };

    public interface OnColorSelected {
        void onColorSelected(int color);
    }

    private AccentColorPickerDialog() {
    }

    public static void show(Context context, int initialColor, OnColorSelected callback) {
        if (context == null || callback == null) {
            return;
        }

        int startColor = initialColor != 0 ? (initialColor | 0xFF000000) : 0xFF56D6E8;
        final float[] hsl = new float[3];
        ColorUtils.colorToHSL(startColor, hsl);
        hsl[1] = Math.max(0.10f, hsl[1]);
        hsl[2] = Math.max(0.20f, Math.min(0.90f, hsl[2]));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialog);
        View contentView = LayoutInflater.from(context).inflate(R.layout.accent_color_picker_dialog, null);

        LinearLayout previewCard = contentView.findViewById(R.id.accent_preview_card);
        View previewSwatch = contentView.findViewById(R.id.accent_preview_swatch);
        TextView previewHex = contentView.findViewById(R.id.accent_preview_hex);
        TextView previewPill = contentView.findViewById(R.id.accent_preview_pill);

        LinearLayout row1 = contentView.findViewById(R.id.accent_swatches_row1);
        LinearLayout row2 = contentView.findViewById(R.id.accent_swatches_row2);

        SeekBar hueSeek = contentView.findViewById(R.id.accent_seek_hue);
        SeekBar satSeek = contentView.findViewById(R.id.accent_seek_sat);
        SeekBar lightSeek = contentView.findViewById(R.id.accent_seek_light);
        TextView hueVal = contentView.findViewById(R.id.accent_val_hue);
        TextView satVal = contentView.findViewById(R.id.accent_val_sat);
        TextView lightVal = contentView.findViewById(R.id.accent_val_light);
        EditText hexInput = contentView.findViewById(R.id.accent_hex_input);

        hueSeek.setKeyProgressIncrement(10);
        satSeek.setKeyProgressIncrement(5);
        lightSeek.setKeyProgressIncrement(5);
        KeyHelpers.fixShowKeyboard(hexInput);

        final boolean[] updatingFromCode = {false};

        Runnable refreshPreview = () -> {
            int currentColor = ColorUtils.HSLToColor(hsl);
            String hex = ThemePaletteHelper.formatHex(currentColor);
            M3Palette derived = new M3Palette(false);
            ThemePaletteHelper.applyAccentTokens(derived, currentColor);

            float density = context.getResources().getDisplayMetrics().density;
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(16f * density);
            cardBg.setColor(derived.secondaryContainer);
            cardBg.setStroke(Math.max(1, Math.round(1.5f * density)), derived.outline);
            previewCard.setBackground(cardBg);

            GradientDrawable swatchBg = new GradientDrawable();
            swatchBg.setShape(GradientDrawable.OVAL);
            swatchBg.setColor(currentColor);
            swatchBg.setStroke(Math.max(1, Math.round(2f * density)), 0x66FFFFFF);
            previewSwatch.setBackground(swatchBg);

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(999f);
            pillBg.setColor(derived.primary);
            previewPill.setBackground(pillBg);
            previewPill.setTextColor(derived.onPrimary);

            previewHex.setText(hex);
            hueVal.setText(Math.round(hsl[0]) + "°");
            satVal.setText(Math.round(hsl[1] * 100) + "%");
            lightVal.setText(Math.round(hsl[2] * 100) + "%");

            if (Build.VERSION.SDK_INT >= 21) {
                ColorStateList tint = ColorStateList.valueOf(derived.primary);
                hueSeek.setProgressTintList(tint);
                hueSeek.setThumbTintList(tint);
                satSeek.setProgressTintList(tint);
                satSeek.setThumbTintList(tint);
                lightSeek.setProgressTintList(tint);
                lightSeek.setThumbTintList(tint);
            } else {
                hueSeek.getProgressDrawable().setColorFilter(derived.primary, PorterDuff.Mode.SRC_IN);
                satSeek.getProgressDrawable().setColorFilter(derived.primary, PorterDuff.Mode.SRC_IN);
                lightSeek.getProgressDrawable().setColorFilter(derived.primary, PorterDuff.Mode.SRC_IN);
            }

            if (!updatingFromCode[0]) {
                updatingFromCode[0] = true;
                if (!hexInput.hasFocus()) {
                    hexInput.setText(hex);
                }
                updatingFromCode[0] = false;
            }
        };

        Runnable syncSlidersFromHsl = () -> {
            updatingFromCode[0] = true;
            hueSeek.setProgress(Math.round(hsl[0]));
            satSeek.setProgress(Math.round(hsl[1] * 100f));
            lightSeek.setProgress(Math.max(0, Math.min(70, Math.round(hsl[2] * 100f) - LIGHTNESS_MIN)));
            hexInput.setText(ThemePaletteHelper.formatHex(ColorUtils.HSLToColor(hsl)));
            updatingFromCode[0] = false;
            refreshPreview.run();
        };

        float density = context.getResources().getDisplayMetrics().density;
        int swatchSize = Math.round(34 * density);
        int swatchMargin = Math.round(5 * density);
        for (int i = 0; i < QUICK_SWATCHES.length; i++) {
            int swatchColor = QUICK_SWATCHES[i];
            View btn = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin);
            btn.setLayoutParams(lp);
            btn.setFocusable(true);
            btn.setClickable(true);
            btn.setContentDescription(ThemePaletteHelper.formatHex(swatchColor));

            StateListDrawable states = new StateListDrawable();
            GradientDrawable focused = new GradientDrawable();
            focused.setShape(GradientDrawable.OVAL);
            focused.setColor(swatchColor);
            focused.setStroke(Math.round(3f * density), Color.WHITE);
            GradientDrawable normal = new GradientDrawable();
            normal.setShape(GradientDrawable.OVAL);
            normal.setColor(swatchColor);
            normal.setStroke(Math.max(1, Math.round(1f * density)), 0x44FFFFFF);
            states.addState(new int[]{android.R.attr.state_focused}, focused);
            states.addState(new int[]{android.R.attr.state_pressed}, focused);
            states.addState(new int[]{}, normal);
            btn.setBackground(states);

            Interpolator spatial = Build.VERSION.SDK_INT >= 21
                    ? new PathInterpolator(0.42f, 1.67f, 0.21f, 0.90f)
                    : new OvershootInterpolator(0.75f);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                float targetScale = hasFocus ? 1.18f : 1.0f;
                v.animate().cancel();
                v.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(180L)
                        .setInterpolator(spatial)
                        .start();
            });

            btn.setOnClickListener(v -> {
                ColorUtils.colorToHSL(swatchColor, hsl);
                syncSlidersFromHsl.run();
            });
            (i < 6 ? row1 : row2).addView(btn);
        }

        OnSeekBarChangeListener sliderListener = new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (updatingFromCode[0]) {
                    return;
                }
                if (seekBar == hueSeek) {
                    hsl[0] = progress;
                } else if (seekBar == satSeek) {
                    hsl[1] = progress / 100f;
                } else if (seekBar == lightSeek) {
                    hsl[2] = (progress + LIGHTNESS_MIN) / 100f;
                }
                refreshPreview.run();
                if (!hexInput.hasFocus()) {
                    updatingFromCode[0] = true;
                    hexInput.setText(ThemePaletteHelper.formatHex(ColorUtils.HSLToColor(hsl)));
                    updatingFromCode[0] = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        hueSeek.setOnSeekBarChangeListener(sliderListener);
        satSeek.setOnSeekBarChangeListener(sliderListener);
        lightSeek.setOnSeekBarChangeListener(sliderListener);

        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingFromCode[0]) {
                    return;
                }
                Integer parsed = parseHexColor(s.toString());
                if (parsed != null) {
                    ColorUtils.colorToHSL(parsed, hsl);
                    updatingFromCode[0] = true;
                    hueSeek.setProgress(Math.round(hsl[0]));
                    satSeek.setProgress(Math.round(hsl[1] * 100f));
                    lightSeek.setProgress(Math.max(0, Math.min(70, Math.round(hsl[2] * 100f) - LIGHTNESS_MIN)));
                    updatingFromCode[0] = false;
                    refreshPreview.run();
                }
            }
        });

        syncSlidersFromHsl.run();

        AlertDialog dialog = builder
                .setTitle(R.string.accent_color_picker_title)
                .setView(contentView)
                .setPositiveButton(R.string.accent_color_apply, null)
                .setNeutralButton(R.string.accent_color_reset, (d, w) -> callback.onColorSelected(0))
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        try {
            dialog.show();
            if (dialog.getWindow() != null) {
                GradientDrawable dialogBg = new GradientDrawable();
                dialogBg.setCornerRadius(28f * density);
                dialogBg.setColor(0xFF1D212A);
                dialogBg.setStroke(Math.max(1, Math.round(1f * density)), 0xFF343A48);
                dialog.getWindow().setBackgroundDrawable(dialogBg);
            }
            if (contentView != null) {
                Interpolator decelerate = Build.VERSION.SDK_INT >= 21
                        ? new PathInterpolator(0.05f, 0.70f, 0.10f, 1.00f)
                        : new DecelerateInterpolator(1.8f);
                contentView.setAlpha(0f);
                contentView.animate()
                        .alpha(1f)
                        .setDuration(180L)
                        .setInterpolator(decelerate)
                        .start();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            MessageHelpers.showMessage(context, e.getMessage());
            return;
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String rawHex = hexInput.getText() != null ? hexInput.getText().toString().trim() : "";
            if (!rawHex.isEmpty()) {
                Integer parsed = parseHexColor(rawHex);
                if (parsed == null) {
                    MessageHelpers.showMessage(context, R.string.accent_color_invalid_hex);
                    return;
                }
                callback.onColorSelected(parsed);
            } else {
                callback.onColorSelected(ColorUtils.HSLToColor(hsl));
            }
            dialog.dismiss();
        });
    }

    private static Integer parseHexColor(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (!trimmed.startsWith("#")) {
            trimmed = "#" + trimmed;
        }
        if (!trimmed.matches("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")) {
            return null;
        }
        try {
            return Color.parseColor(trimmed) | 0xFF000000;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
