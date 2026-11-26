package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData.ColorScheme;
import com.liskovsoft.smartyoutubetv2.common.utils.AccentColorPickerDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.interfaces.SettingsSection;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings.SettingsHubHelper.RailEntry;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3Button;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3ChipBarView;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3ChipBarView.ChipItem;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3QuickToggleRow;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Material 3 Expressive Two-Pane Settings Hub.
 * Left pane presents categorized, segmented navigation items with live value summaries;
 * right pane presents interactive Quick Controls (live theme preview, accent swatches, instant switches)
 * and contextual category detail cards.
 */
public class SettingsGridFragment extends Fragment implements SettingsSection, BrowseSupportFragment.MainFragmentAdapterProvider {
    private static final float[] SCALE_STEPS = {0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f};
    private static final int[][] ACCENT_PRESETS = {
            {R.string.accent_color_default, 0},
            {R.string.accent_color_crimson, 0xFFFF4E45},
            {R.string.accent_color_coral, 0xFFFF897D},
            {R.string.accent_color_amber, 0xFFFFD54F},
            {R.string.accent_color_emerald, 0xFF66BB6A},
            {R.string.accent_color_teal, 0xFF56D6E8},
            {R.string.accent_color_cobalt, 0xFF9ECAFF},
            {R.string.accent_color_lavender, 0xFFD0BCFF},
            {R.string.accent_color_mono, 0xFFE2E2E8}
    };

    private final List<SettingsGroup> mPendingUpdates = new ArrayList<>();
    private final List<SettingsItem> mRawItems = new ArrayList<>();
    private final List<RailEntry> mRailEntries = new ArrayList<>();

    private RecyclerView mRailList;
    private RailAdapter mRailAdapter;
    private LinearLayout mDetailContainer;
    private ScrollView mDetailScroll;
    private LinearLayout mRailPane;
    private TextView mRailTitle;
    private TextView mRailBadge;

    private int mSelectedRailPosition = 0;
    private boolean mNeedsRestart;

    // Live preview card views for Quick Controls
    private LinearLayout mPreviewCard;
    private TextView mPreviewBadge;
    private TextView mPreviewTitle;
    private TextView mPreviewSubtitle;
    private View mPreviewThumb;
    private View mPreviewProgress;
    private LinearLayout mRestartBanner;

    private final BrowseSupportFragment.MainFragmentAdapter<Fragment> mMainFragmentAdapter =
            new BrowseSupportFragment.MainFragmentAdapter<>(this);

    @Override
    public BrowseSupportFragment.MainFragmentAdapter<Fragment> getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRailPane = view.findViewById(R.id.settings_rail_pane);
        mRailTitle = view.findViewById(R.id.settings_rail_title);
        mRailBadge = view.findViewById(R.id.settings_rail_badge);
        mRailList = view.findViewById(R.id.settings_rail_list);
        mDetailScroll = view.findViewById(R.id.settings_detail_scroll);
        mDetailContainer = view.findViewById(R.id.settings_detail_container);

        stylePanes();
        setupRailList();
        applyPendingUpdates();

        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyViewCreated(getMainFragmentAdapter());
            getMainFragmentAdapter().getFragmentHost().showTitleView(true);
        }

        if (savedInstanceState == null) {
            M3Motion.animateEnter(view, 14f * getResources().getDisplayMetrics().density, 0f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRailAdapter != null && mRailList != null && !mRailList.hasFocus()) {
            mRailAdapter.notifyDataSetChanged();
        }
        renderSelectedDetail(false);
    }

    private void stylePanes() {
        Context ctx = requireContext();
        int surfaceLow = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerLow, R.color.m3_grey_surface_container_low);
        int surfaceContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);

        float railRadius = ctx.getResources().getDimension(R.dimen.m3_dialog_corner_radius);
        float detailRadius = ctx.getResources().getDimension(R.dimen.m3_dialog_corner_radius);
        float pillRadius = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        mRailPane.setBackground(ViewUtil.createRoundedDrawable(surfaceLow, outlineVariant, hairlinePx, railRadius));
        ViewUtil.applyRoundedOutline(mRailPane, railRadius);
        mDetailScroll.setBackground(ViewUtil.createRoundedDrawable(surfaceContainer, outlineVariant, hairlinePx, detailRadius));
        ViewUtil.applyRoundedOutline(mDetailScroll, detailRadius);
        mRailTitle.setTextColor(onSurface);

        Account account = MediaServiceManager.instance().getSelectedAccount();
        String badgeText = account != null && !TextUtils.isEmpty(account.getName())
                ? account.getName()
                : getString(R.string.settings_not_signed_in);
        mRailBadge.setText(badgeText);
        mRailBadge.setTextColor(onPrimaryContainer);
        mRailBadge.setBackground(ViewUtil.createRoundedDrawable(primaryContainer, 0, 0, pillRadius));
    }

    private void setupRailList() {
        mRailAdapter = new RailAdapter();
        mRailList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        mRailList.setItemAnimator(null);
        mRailList.setAdapter(mRailAdapter);
    }

    private void applyPendingUpdates() {
        if (mPendingUpdates.isEmpty()) {
            rebuildRail();
            return;
        }
        List<SettingsGroup> copy = new ArrayList<>(mPendingUpdates);
        mPendingUpdates.clear();
        for (SettingsGroup group : copy) {
            update(group);
        }
    }

    @Override
    public void clear() {
        mRawItems.clear();
        rebuildRail();
    }

    @Override
    public boolean isEmpty() {
        if (mRawItems.isEmpty() && !mPendingUpdates.isEmpty()) {
            return false;
        }
        return false; // Quick Controls is always available
    }

    @Override
    public void update(SettingsGroup group) {
        if (mRailList == null) {
            mPendingUpdates.add(group);
            return;
        }
        mRawItems.clear();
        if (group != null && group.getItems() != null) {
            mRawItems.addAll(group.getItems());
        }
        rebuildRail();
    }

    private void rebuildRail() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        mRailEntries.clear();
        mRailEntries.addAll(SettingsHubHelper.buildRailEntries(getContext(), mRawItems));
        if (mSelectedRailPosition >= mRailEntries.size()) {
            mSelectedRailPosition = 0;
        }
        if (mRailAdapter != null) {
            mRailAdapter.notifyDataSetChanged();
        }
        renderSelectedDetail(false);
    }

    private void onRailEntryFocused(int position) {
        if (position < 0 || position >= mRailEntries.size()) {
            return;
        }
        RailEntry entry = mRailEntries.get(position);
        if (entry.type == RailEntry.TYPE_HEADER) {
            return;
        }
        boolean changed = mSelectedRailPosition != position;
        mSelectedRailPosition = position;
        if (changed) {
            renderSelectedDetail(true);
        }
    }

    private void onRailEntryClicked(int position) {
        if (position < 0 || position >= mRailEntries.size()) {
            return;
        }
        RailEntry entry = mRailEntries.get(position);
        if (entry.type == RailEntry.TYPE_QUICK_CONTROLS) {
            focusDetailPane();
        } else if (entry.type == RailEntry.TYPE_CATEGORY && entry.settingsItem != null) {
            launchSettingsItem(entry.settingsItem);
        }
    }

    private void focusDetailPane() {
        if (mDetailContainer == null) {
            return;
        }
        View target = findFirstFocusable(mDetailContainer);
        if (target != null) {
            target.requestFocus();
        }
    }

    private void returnFocusToRail() {
        if (mRailList == null) {
            return;
        }
        RecyclerView.ViewHolder vh = mRailList.findViewHolderForAdapterPosition(mSelectedRailPosition);
        if (vh != null && vh.itemView != null) {
            vh.itemView.requestFocus();
        } else {
            mRailList.requestFocus();
        }
    }

    @Nullable
    private View findFirstFocusable(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            if (child.isFocusable()) {
                return child;
            }
            if (child instanceof ViewGroup) {
                View nested = findFirstFocusable((ViewGroup) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private void attachReturnToRailOnLeft(View view) {
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                returnFocusToRail();
                return true;
            }
            return false;
        });
    }

    private void renderSelectedDetail(boolean animate) {
        if (!isAdded() || getContext() == null || mDetailContainer == null) {
            return;
        }
        if (mSelectedRailPosition < 0 || mSelectedRailPosition >= mRailEntries.size()) {
            return;
        }
        RailEntry entry = mRailEntries.get(mSelectedRailPosition);
        mDetailContainer.removeAllViews();

        if (entry.type == RailEntry.TYPE_QUICK_CONTROLS) {
            buildQuickControlsPane();
        } else if (entry.type == RailEntry.TYPE_CATEGORY && entry.settingsItem != null) {
            buildCategoryDetailPane(entry.settingsItem);
        }

        if (animate) {
            M3Motion.animateEnter(mDetailContainer, 10f * getResources().getDisplayMetrics().density, 0f);
        }
    }

    // =========================================================================
    // Mode A: Interactive Quick Controls & Live Theme Studio
    // =========================================================================

    private void buildQuickControlsPane() {
        Context ctx = requireContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        MainUIData uiData = MainUIData.instance(ctx);
        GeneralData generalData = GeneralData.instance(ctx);
        MediaServiceData mediaData = MediaServiceData.instance();

        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);

        // Header
        TextView heading = new TextView(ctx);
        heading.setText(R.string.settings_quick_controls);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        heading.setTextColor(onSurface);
        mDetailContainer.addView(heading);

        TextView sub = new TextView(ctx);
        sub.setText(R.string.settings_quick_controls_subtitle);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(onSurfaceVariant);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Math.round(3 * density);
        subLp.bottomMargin = Math.round(16 * density);
        mDetailContainer.addView(sub, subLp);

        // 1. Live Theme & Card Preview Card
        buildLivePreviewCard(ctx, density);

        // 2. Restart Hint Banner (shown when layout/theme needs restart)
        buildRestartBanner(ctx, density);

        // 3. Color Scheme Cycler
        M3QuickToggleRow schemeRow = new M3QuickToggleRow(ctx);
        schemeRow.setIcon(R.drawable.ic_m3_palette);
        schemeRow.setTitle(getString(R.string.settings_cycle_scheme));
        schemeRow.setSubtitle(getString(R.string.settings_main_ui));
        schemeRow.setSegmentedPosition(true, false);
        Runnable syncSchemeLabel = () -> {
            ColorScheme active = uiData.getColorScheme();
            schemeRow.bindValuePill(active != null ? getString(active.nameResId) : "");
        };
        syncSchemeLabel.run();
        Runnable cycleScheme = () -> {
            List<ColorScheme> schemes = uiData.getColorSchemes();
            if (schemes.isEmpty()) return;
            int idx = schemes.indexOf(uiData.getColorScheme());
            int nextIdx = (idx + 1) % schemes.size();
            uiData.setColorScheme(schemes.get(nextIdx));
            syncSchemeLabel.run();
            markNeedsRestart();
            refreshLivePreview();
        };
        schemeRow.setOnClickListener(v -> cycleScheme.run());
        schemeRow.setOnStepListener(delta -> cycleScheme.run());
        attachReturnToRailOnLeft(schemeRow);
        addSpacedRow(schemeRow, Math.round(4 * density));

        // 4. Accent Color Swatches Strip (M3ChipBarView)
        TextView accentLabel = new TextView(ctx);
        accentLabel.setText(R.string.accent_color);
        accentLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        accentLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        accentLabel.setTextColor(onSurfaceVariant);
        LinearLayout.LayoutParams accLblLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        accLblLp.topMargin = Math.round(12 * density);
        accLblLp.bottomMargin = Math.round(6 * density);
        mDetailContainer.addView(accentLabel, accLblLp);

        M3ChipBarView accentChips = new M3ChipBarView(ctx);
        List<ChipItem> swatchItems = new ArrayList<>();
        int currentAccent = uiData.getAccentColor();
        int selectedChip = 0;
        for (int i = 0; i < ACCENT_PRESETS.length; i++) {
            int color = ACCENT_PRESETS[i][1];
            String name = getString(ACCENT_PRESETS[i][0]);
            if (color == 0) {
                swatchItems.add(new ChipItem(0, getString(R.string.accent_color_reset)));
            } else {
                swatchItems.add(new ChipItem(color, name, color));
            }
            if (currentAccent == color) {
                selectedChip = i;
            }
        }
        swatchItems.add(new ChipItem(-1, getString(R.string.accent_color_custom)));
        if (currentAccent != 0 && selectedChip == 0) {
            selectedChip = swatchItems.size() - 1;
        }
        accentChips.setChips(swatchItems, selectedChip);
        accentChips.setOnChipSelectedListener((index, item) -> {
            if (item.id == -1) {
                AccentColorPickerDialog.show(ctx, uiData.getAccentColor(), newColor -> {
                    uiData.setAccentColor(newColor);
                    markNeedsRestart();
                    refreshLivePreview();
                });
            } else {
                uiData.setAccentColor(item.id);
                markNeedsRestart();
                refreshLivePreview();
            }
        });
        View firstAccentChip = accentChips.getFirstChipView();
        if (firstAccentChip != null) {
            attachReturnToRailOnLeft(firstAccentChip);
        }
        LinearLayout.LayoutParams chipBarLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipBarLp.bottomMargin = Math.round(12 * density);
        mDetailContainer.addView(accentChips, chipBarLp);

        // 5. Card Previews segmented cycle (Off / Muted / Full)
        M3QuickToggleRow previewRow = new M3QuickToggleRow(ctx);
        previewRow.setTitle(getString(R.string.card_preview));
        previewRow.setSegmentedPosition(false, false);
        Runnable syncPreviewLabel = () -> {
            int mode = uiData.getCardPreviewType();
            int res = mode == MainUIData.CARD_PREVIEW_FULL ? R.string.card_preview_full
                    : mode == MainUIData.CARD_PREVIEW_MUTED ? R.string.card_preview_muted
                    : R.string.option_disabled;
            previewRow.bindValuePill(getString(res));
        };
        syncPreviewLabel.run();
        Runnable cyclePreview = () -> {
            int next = (uiData.getCardPreviewType() + 1) % 3;
            uiData.setCardPreviewType(next);
            uiData.persistNow();
            syncPreviewLabel.run();
        };
        previewRow.setOnClickListener(v -> cyclePreview.run());
        previewRow.setOnStepListener(delta -> cyclePreview.run());
        attachReturnToRailOnLeft(previewRow);
        addSpacedRow(previewRow, Math.round(4 * density));

        // 6. Global Clock M3Switch
        M3QuickToggleRow clockRow = new M3QuickToggleRow(ctx);
        clockRow.setTitle(getString(R.string.player_show_global_clock));
        clockRow.setSegmentedPosition(false, false);
        clockRow.bindSwitch(generalData.isGlobalClockEnabled(), false);
        clockRow.setOnClickListener(v -> {
            boolean next = !generalData.isGlobalClockEnabled();
            generalData.setGlobalClockEnabled(next);
            clockRow.bindSwitch(next, true);
            uiData.onDataChange();
        });
        attachReturnToRailOnLeft(clockRow);
        addSpacedRow(clockRow, Math.round(4 * density));

        // 7. Rounded Card Corners M3Switch
        M3QuickToggleRow cornersRow = new M3QuickToggleRow(ctx);
        cornersRow.setTitle(getString(R.string.settings_rounded_corners));
        cornersRow.setSegmentedPosition(false, false);
        cornersRow.bindSwitch(uiData.isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS), false);
        cornersRow.setOnClickListener(v -> {
            boolean next = !uiData.isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS);
            if (next) {
                uiData.setUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS);
            } else {
                uiData.setUiTweakDisabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS);
            }
            uiData.persistNow();
            cornersRow.bindSwitch(next, true);
            markNeedsRestart();
            refreshLivePreview();
        });
        attachReturnToRailOnLeft(cornersRow);
        addSpacedRow(cornersRow, Math.round(4 * density));

        // 8. Multiline Titles M3Switch
        M3QuickToggleRow multilineRow = new M3QuickToggleRow(ctx);
        multilineRow.setTitle(getString(R.string.card_multiline_title));
        multilineRow.setSegmentedPosition(false, false);
        multilineRow.bindSwitch(uiData.isCardMultilineTitleEnabled(), false);
        multilineRow.setOnClickListener(v -> {
            boolean next = !uiData.isCardMultilineTitleEnabled();
            uiData.setCardMultilineTitleEnabled(next);
            uiData.persistNow();
            multilineRow.bindSwitch(next, true);
        });
        attachReturnToRailOnLeft(multilineRow);
        addSpacedRow(multilineRow, Math.round(4 * density));

        // 9. Hide Shorts Everywhere M3Switch
        M3QuickToggleRow shortsRow = new M3QuickToggleRow(ctx);
        shortsRow.setTitle(getString(R.string.hide_shorts_everywhere));
        shortsRow.setSegmentedPosition(false, false);
        shortsRow.bindSwitch(mediaData.isContentHidden(MediaServiceData.CONTENT_SHORTS_ALL), false);
        shortsRow.setOnClickListener(v -> {
            boolean next = !mediaData.isContentHidden(MediaServiceData.CONTENT_SHORTS_ALL);
            mediaData.setContentHidden(MediaServiceData.CONTENT_SHORTS_ALL, next);
            BrowsePresenter.instance(ctx).enableSection(MediaGroup.TYPE_SHORTS, !next);
            shortsRow.bindSwitch(next, true);
        });
        attachReturnToRailOnLeft(shortsRow);
        addSpacedRow(shortsRow, Math.round(4 * density));

        // 10. Video Grid Scale stepper
        M3QuickToggleRow gridScaleRow = new M3QuickToggleRow(ctx);
        gridScaleRow.setTitle(getString(R.string.video_grid_scale));
        gridScaleRow.setSegmentedPosition(false, false);
        gridScaleRow.bindValuePill(String.format(Locale.US, "%.2gx", uiData.getVideoGridScale()));
        Runnable stepGridScale = () -> {
            float next = nextScaleStep(uiData.getVideoGridScale());
            uiData.setVideoGridScale(next);
            uiData.persistNow();
            gridScaleRow.bindValuePill(String.format(Locale.US, "%.2gx", next));
            markNeedsRestart();
        };
        gridScaleRow.setOnClickListener(v -> stepGridScale.run());
        gridScaleRow.setOnStepListener(delta -> stepGridScale.run());
        attachReturnToRailOnLeft(gridScaleRow);
        addSpacedRow(gridScaleRow, Math.round(4 * density));

        // 11. UI Scale stepper (last in segmented group)
        M3QuickToggleRow uiScaleRow = new M3QuickToggleRow(ctx);
        uiScaleRow.setTitle(getString(R.string.settings_ui_scale));
        uiScaleRow.setSegmentedPosition(false, true);
        uiScaleRow.bindValuePill(String.format(Locale.US, "%.2gx", uiData.getUIScale()));
        Runnable stepUiScale = () -> {
            float next = nextScaleStep(uiData.getUIScale());
            uiData.setUIScale(next);
            uiData.persistNow();
            uiScaleRow.bindValuePill(String.format(Locale.US, "%.2gx", next));
            markNeedsRestart();
        };
        uiScaleRow.setOnClickListener(v -> stepUiScale.run());
        uiScaleRow.setOnStepListener(delta -> stepUiScale.run());
        attachReturnToRailOnLeft(uiScaleRow);
        addSpacedRow(uiScaleRow, Math.round(6 * density));

        refreshLivePreview();
    }

    private void buildLivePreviewCard(Context ctx, float density) {
        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        float cardRadius = ctx.getResources().getDimension(R.dimen.m3_card_large_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        mPreviewCard = new LinearLayout(ctx);
        mPreviewCard.setOrientation(LinearLayout.HORIZONTAL);
        mPreviewCard.setGravity(Gravity.CENTER_VERTICAL);
        mPreviewCard.setPadding(Math.round(16 * density), Math.round(14 * density), Math.round(16 * density), Math.round(14 * density));
        mPreviewCard.setBackground(ViewUtil.createRoundedDrawable(surfaceHigh, outlineVariant, hairlinePx, cardRadius));

        FrameLayout thumbWrapper = new FrameLayout(ctx);
        int thumbW = Math.round(112 * density);
        int thumbH = Math.round(63 * density);
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(thumbW, thumbH);
        thumbLp.setMarginEnd(Math.round(14 * density));

        mPreviewThumb = new View(ctx);
        thumbWrapper.addView(mPreviewThumb, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mPreviewProgress = new View(ctx);
        FrameLayout.LayoutParams progLp = new FrameLayout.LayoutParams(
                Math.round(92 * density), Math.round(4 * density));
        progLp.gravity = Gravity.BOTTOM | Gravity.START;
        thumbWrapper.addView(mPreviewProgress, progLp);
        mPreviewCard.addView(thumbWrapper, thumbLp);

        LinearLayout infoCol = new LinearLayout(ctx);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        mPreviewBadge = new TextView(ctx);
        mPreviewBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mPreviewBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        mPreviewBadge.setText(R.string.settings_preview_label);
        mPreviewBadge.setIncludeFontPadding(false);
        mPreviewBadge.setPadding(Math.round(10 * density), Math.round(4 * density), Math.round(10 * density), Math.round(4 * density));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.bottomMargin = Math.round(6 * density);
        infoCol.addView(mPreviewBadge, badgeLp);

        mPreviewTitle = new TextView(ctx);
        mPreviewTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mPreviewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mPreviewTitle.setText(R.string.settings_preview_title);
        infoCol.addView(mPreviewTitle);

        mPreviewSubtitle = new TextView(ctx);
        mPreviewSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mPreviewSubtitle.setText(R.string.settings_preview_subtitle);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Math.round(3 * density);
        infoCol.addView(mPreviewSubtitle, subLp);

        mPreviewCard.addView(infoCol, infoLp);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = Math.round(14 * density);
        mDetailContainer.addView(mPreviewCard, cardLp);
    }

    private void buildRestartBanner(Context ctx, float density) {
        int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        float radius = ctx.getResources().getDimension(R.dimen.m3_card_corner_radius);

        mRestartBanner = new LinearLayout(ctx);
        mRestartBanner.setOrientation(LinearLayout.HORIZONTAL);
        mRestartBanner.setGravity(Gravity.CENTER_VERTICAL);
        mRestartBanner.setPadding(Math.round(14 * density), Math.round(10 * density), Math.round(14 * density), Math.round(10 * density));
        mRestartBanner.setBackground(ViewUtil.createRoundedDrawable(primaryContainer, 0, 0, radius));
        mRestartBanner.setVisibility(mNeedsRestart ? View.VISIBLE : View.GONE);

        TextView msg = new TextView(ctx);
        msg.setText(R.string.settings_apply_restart_hint);
        msg.setTextColor(onPrimaryContainer);
        msg.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mRestartBanner.addView(msg, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        M3Button restartBtn = new M3Button(ctx);
        restartBtn.setButtonStyle(M3Button.STYLE_FILLED);
        restartBtn.setText(getString(R.string.settings_restart_now));
        restartBtn.setOnClickListener(v -> Utils.restartTheApp(ctx));
        attachReturnToRailOnLeft(restartBtn);
        mRestartBanner.addView(restartBtn);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Math.round(12 * density);
        mDetailContainer.addView(mRestartBanner, lp);
    }

    private void markNeedsRestart() {
        mNeedsRestart = true;
        if (mRestartBanner != null) {
            mRestartBanner.setVisibility(View.VISIBLE);
        }
    }

    private void refreshLivePreview() {
        if (!isAdded() || getContext() == null || mPreviewCard == null) {
            return;
        }
        Context ctx = getContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        MainUIData uiData = MainUIData.instance(ctx);

        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int primary = ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary);
        int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);

        boolean rounded = uiData.isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS);
        float cardRadius = rounded ? ctx.getResources().getDimension(R.dimen.m3_card_large_corner_radius) : 4f * density;
        float thumbRadius = rounded ? 12f * density : 2f * density;
        float pillRadius = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        mPreviewCard.setBackground(ViewUtil.createRoundedDrawable(surfaceHigh, outlineVariant, hairlinePx, cardRadius));

        GradientDrawable thumbGrad = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{primaryContainer, surfaceHigh});
        thumbGrad.setCornerRadius(thumbRadius);
        mPreviewThumb.setBackground(thumbGrad);

        mPreviewProgress.setBackground(ViewUtil.createRoundedDrawable(primary, 0, 0, pillRadius));
        mPreviewBadge.setBackground(ViewUtil.createRoundedDrawable(primaryContainer, 0, 0, pillRadius));
        mPreviewBadge.setTextColor(onPrimaryContainer);
        mPreviewTitle.setTextColor(onSurface);
        mPreviewSubtitle.setTextColor(onSurfaceVariant);
    }

    // =========================================================================
    // Mode B: Category Detail Panel
    // =========================================================================

    private void buildCategoryDetailPane(SettingsItem item) {
        Context ctx = requireContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        int primaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
        int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
        int secondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
        int onSecondaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        float pillRadius = ctx.getResources().getDimension(R.dimen.m3_pill_corner_radius);
        float heroRadius = ctx.getResources().getDimension(R.dimen.m3_card_large_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        // Hero badge + icon
        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(Math.round(20 * density), Math.round(20 * density), Math.round(20 * density), Math.round(20 * density));
        headerRow.setBackground(ViewUtil.createRoundedDrawable(surfaceHigh, outlineVariant, hairlinePx, heroRadius));

        if (item.imageResId > 0) {
            FrameLayout iconBox = new FrameLayout(ctx);
            int boxSize = Math.round(60 * density);
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(boxSize, boxSize);
            boxLp.setMarginEnd(Math.round(18 * density));
            iconBox.setBackground(ViewUtil.createRoundedDrawable(primaryContainer, 0, 0, Math.round(18 * density)));

            ImageView icon = new ImageView(ctx);
            int iconSize = Math.round(30 * density);
            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
            icon.setImageDrawable(ContextCompat.getDrawable(ctx, item.imageResId));
            icon.setColorFilter(onPrimaryContainer, PorterDuff.Mode.SRC_IN);
            iconBox.addView(icon, iconLp);
            headerRow.addView(iconBox, boxLp);
        }

        LinearLayout titleCol = new LinearLayout(ctx);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(ctx);
        titleView.setText(item.title);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        titleView.setTextColor(onSurface);
        titleCol.addView(titleView);

        String summary = SettingsHubHelper.getSubtitleSummary(ctx, item);
        if (!TextUtils.isEmpty(summary)) {
            TextView summaryView = new TextView(ctx);
            summaryView.setText(summary);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            summaryView.setTextColor(onSurfaceVariant);
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sLp.topMargin = Math.round(4 * density);
            titleCol.addView(summaryView, sLp);
        }
        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.bottomMargin = Math.round(16 * density);
        mDetailContainer.addView(headerRow, headerLp);

        // Description
        String desc = SettingsHubHelper.getCategoryDescription(ctx, item);
        if (!TextUtils.isEmpty(desc)) {
            TextView descView = new TextView(ctx);
            descView.setText(desc);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            descView.setLineSpacing(0f, 1.25f);
            descView.setTextColor(onSurfaceVariant);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dLp.bottomMargin = Math.round(16 * density);
            mDetailContainer.addView(descView, dLp);
        }

        // Status chips row
        List<String> chips = SettingsHubHelper.getDetailChips(ctx, item);
        if (!chips.isEmpty()) {
            LinearLayout chipsRow = new LinearLayout(ctx);
            chipsRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < chips.size(); i++) {
                TextView chip = new TextView(ctx);
                chip.setText(chips.get(i));
                chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                chip.setTextColor(onSecondaryContainer);
                chip.setIncludeFontPadding(false);
                chip.setPadding(Math.round(12 * density), Math.round(6 * density), Math.round(12 * density), Math.round(6 * density));
                chip.setBackground(ViewUtil.createRoundedDrawable(secondaryContainer, 0, 0, pillRadius));
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) cLp.setMarginStart(Math.round(8 * density));
                chipsRow.addView(chip, cLp);
            }
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = Math.round(22 * density);
            mDetailContainer.addView(chipsRow, rLp);
        }

        // Primary & Secondary CTAs
        LinearLayout actionsRow = new LinearLayout(ctx);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setClipChildren(false);
        actionsRow.setClipToPadding(false);

        M3Button openBtn = new M3Button(ctx);
        openBtn.setButtonStyle(M3Button.STYLE_FILLED);
        openBtn.setIcon(R.drawable.ic_m3_chevron_right);
        openBtn.setText(getString(R.string.settings_open_category, item.title));
        openBtn.setOnClickListener(v -> launchSettingsItem(item));
        attachReturnToRailOnLeft(openBtn);
        actionsRow.addView(openBtn);

        if (item.imageResId == R.drawable.settings_account) {
            M3Button switchAcc = new M3Button(ctx);
            switchAcc.setButtonStyle(M3Button.STYLE_TONAL);
            switchAcc.setText(getString(R.string.settings_switch_account));
            switchAcc.setOnClickListener(v -> AccountSelectionPresenter.instance(ctx).nextAccountOrDialog());
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sLp.setMarginStart(Math.round(12 * density));
            actionsRow.addView(switchAcc, sLp);
        } else if (item.imageResId == R.drawable.settings_main_ui) {
            M3Button pickAccent = new M3Button(ctx);
            pickAccent.setButtonStyle(M3Button.STYLE_TONAL);
            pickAccent.setIcon(R.drawable.ic_m3_palette);
            pickAccent.setText(getString(R.string.accent_color));
            pickAccent.setOnClickListener(v -> {
                MainUIData uiData = MainUIData.instance(ctx);
                AccentColorPickerDialog.show(ctx, uiData.getAccentColor(), newColor -> {
                    uiData.setAccentColor(newColor);
                    Utils.post(() -> Toast.makeText(ctx, R.string.msg_restart_app, Toast.LENGTH_LONG).show());
                });
            });
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sLp.setMarginStart(Math.round(12 * density));
            actionsRow.addView(pickAccent, sLp);
        }

        mDetailContainer.addView(actionsRow);
    }

    private void addSpacedRow(View view, int bottomMarginPx) {
        int sideMarginPx = Math.round(3 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(sideMarginPx);
        lp.setMarginEnd(sideMarginPx);
        lp.bottomMargin = bottomMarginPx;
        mDetailContainer.addView(view, lp);
    }

    private float nextScaleStep(float current) {
        for (int i = 0; i < SCALE_STEPS.length; i++) {
            if (SCALE_STEPS[i] > current + 0.03f) {
                return SCALE_STEPS[i];
            }
        }
        return SCALE_STEPS[0];
    }

    private void launchSettingsItem(SettingsItem item) {
        if (getContext() == null || item == null) {
            return;
        }
        String password = GeneralData.instance(getContext()).getSettingsPassword();
        if (password == null) {
            item.onClick.run();
        } else {
            SimpleEditDialog.showPassword(
                    getContext(),
                    getContext().getString(R.string.enter_settings_password),
                    null,
                    newValue -> {
                        if (Utils.passwordMatch(password, newValue)) {
                            item.onClick.run();
                            return true;
                        }
                        return false;
                    });
        }
    }

    // =========================================================================
    // Left Rail Adapter (Segmented M3 Expressive List)
    // =========================================================================

    private final class RailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return mRailEntries.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == RailEntry.TYPE_HEADER) {
                View v = inflater.inflate(R.layout.settings_hub_section_header, parent, false);
                return new HeaderVH(v);
            }
            View v = inflater.inflate(R.layout.settings_hub_rail_item, parent, false);
            return new ItemVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RailEntry entry = mRailEntries.get(position);
            Context ctx = holder.itemView.getContext();

            if (holder instanceof HeaderVH) {
                TextView tv = (TextView) holder.itemView;
                tv.setText(entry.headerTitle);
                tv.setTextColor(ViewUtil.getThemeColor(ctx, R.attr.m3Primary, R.color.m3_grey_primary));
                return;
            }

            if (holder instanceof ItemVH) {
                ItemVH vh = (ItemVH) holder;
                vh.bind(entry, position);
            }
        }

        @Override
        public int getItemCount() {
            return mRailEntries.size();
        }

        final class HeaderVH extends RecyclerView.ViewHolder {
            HeaderVH(View itemView) {
                super(itemView);
            }
        }

        final class ItemVH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title;
            final TextView subtitle;
            final ImageView chevron;

            ItemVH(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.settings_rail_icon);
                title = itemView.findViewById(R.id.settings_rail_item_title);
                subtitle = itemView.findViewById(R.id.settings_rail_item_subtitle);
                chevron = itemView.findViewById(R.id.settings_rail_chevron);
            }

            void bind(RailEntry entry, int position) {
                Context ctx = itemView.getContext();
                int restingBg = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
                int activeBg = ViewUtil.getThemeColor(ctx, R.attr.m3PrimaryContainer, R.color.m3_grey_primary_container);
                int focusedBg = ViewUtil.getThemeColor(ctx, R.attr.m3SecondaryContainer, R.color.m3_grey_secondary_container);
                int outline = ViewUtil.getThemeColor(ctx, R.attr.m3Outline, R.color.m3_grey_outline);
                int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
                int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
                int onSecondary = ViewUtil.getThemeColor(ctx, R.attr.m3OnSecondaryContainer, R.color.m3_grey_on_secondary_container);
                int onPrimaryContainer = ViewUtil.getThemeColor(ctx, R.attr.m3OnPrimaryContainer, R.color.m3_grey_on_primary_container);
                float outerRadius = ctx.getResources().getDimension(R.dimen.m3_card_corner_radius);
                float innerRadius = ctx.getResources().getDimension(R.dimen.m3_segment_inner_corner_radius);
                int focusStrokePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_focus_stroke_width);

                float[] radii = M3Motion.buildSegmentedRadii(outerRadius, innerRadius, entry.isFirstInGroup, entry.isLastInGroup);

                if (entry.type == RailEntry.TYPE_QUICK_CONTROLS) {
                    title.setText(R.string.settings_quick_controls);
                    subtitle.setText(R.string.settings_quick_controls_subtitle);
                    subtitle.setVisibility(View.VISIBLE);
                    icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_m3_tune));
                } else if (entry.settingsItem != null) {
                    title.setText(entry.settingsItem.title);
                    String sub = SettingsHubHelper.getSubtitleSummary(ctx, entry.settingsItem);
                    if (!TextUtils.isEmpty(sub)) {
                        subtitle.setText(sub);
                        subtitle.setVisibility(View.VISIBLE);
                    } else {
                        subtitle.setVisibility(View.GONE);
                    }
                    if (entry.settingsItem.imageResId > 0) {
                        icon.setImageDrawable(ContextCompat.getDrawable(ctx, entry.settingsItem.imageResId));
                    } else {
                        icon.setImageDrawable(null);
                    }
                }

                boolean isSelected = position == mSelectedRailPosition;
                boolean hasFocus = itemView.hasFocus();
                int baseFill = (entry.type == RailEntry.TYPE_QUICK_CONTROLS && !hasFocus && isSelected) ? activeBg : restingBg;
                int baseTitle = (entry.type == RailEntry.TYPE_QUICK_CONTROLS && !hasFocus && isSelected) ? onPrimaryContainer : onSurface;

                M3Motion.cancelFocusAnimations(itemView);
                M3Motion.applyCardFocusTransition(
                        itemView,
                        hasFocus,
                        hasFocus ? baseFill : focusedBg,
                        hasFocus ? focusedBg : baseFill,
                        hasFocus ? Color.TRANSPARENT : outline,
                        hasFocus ? outline : Color.TRANSPARENT,
                        hasFocus ? 0 : focusStrokePx,
                        hasFocus ? focusStrokePx : 0,
                        outerRadius,
                        radii,
                        (fraction, currentFill) -> {
                            int tColor = ColorUtils.blendARGB(baseTitle, onSecondary, hasFocus ? fraction : 1f - fraction);
                            int sColor = ColorUtils.blendARGB(onSurfaceVariant, ColorUtils.setAlphaComponent(onSecondary, 200), hasFocus ? fraction : 1f - fraction);
                            title.setTextColor(tColor);
                            subtitle.setTextColor(sColor);
                            M3Motion.setImageTint(icon, tColor);
                            M3Motion.setImageTint(chevron, sColor);
                        });

                itemView.setOnFocusChangeListener((v, focused) -> {
                    if (focused) {
                        onRailEntryFocused(position);
                    }
                    M3Motion.applyCardFocusTransition(
                            v,
                            focused,
                            focused ? baseFill : focusedBg,
                            focused ? focusedBg : baseFill,
                            focused ? Color.TRANSPARENT : outline,
                            focused ? outline : Color.TRANSPARENT,
                            focused ? 0 : focusStrokePx,
                            focused ? focusStrokePx : 0,
                            outerRadius,
                            radii,
                            (fraction, currentFill) -> {
                                int tColor = ColorUtils.blendARGB(baseTitle, onSecondary, focused ? fraction : 1f - fraction);
                                int sColor = ColorUtils.blendARGB(onSurfaceVariant, ColorUtils.setAlphaComponent(onSecondary, 200), focused ? fraction : 1f - fraction);
                                title.setTextColor(tColor);
                                subtitle.setTextColor(sColor);
                                M3Motion.setImageTint(icon, tColor);
                                M3Motion.setImageTint(chevron, sColor);
                            });
                    M3Motion.animateFocusScale(v, focused, 1.015f);
                });

                itemView.setOnClickListener(v -> onRailEntryClicked(position));
                itemView.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        focusDetailPane();
                        return true;
                    }
                    return false;
                });
            }
        }
    }
}
