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
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
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

import java.lang.ref.WeakReference;
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

    private LinearLayout mRootLayout;
    private View mPaneSpacer;
    private RecyclerView mRailList;
    private RailAdapter mRailAdapter;
    private FrameLayout mDetailPane;
    private LinearLayout mDetailContainer;
    private ScrollView mDetailScroll;
    private LinearLayout mRailPane;
    private M3Button mBackButton;
    private TextView mRailTitle;

    private static final String STATE_SELECTED_RAIL_POS = "state_selected_rail_pos";
    private int mSelectedRailPosition = 0;
    private int mLastLayoutWidth = -1;
    private int mLastLayoutHeight = -1;
    private int mSafeInsetStart;
    private int mSafeInsetTop;
    private int mSafeInsetEnd;
    private int mSafeInsetBottom;
    private int mLastSafeInsetStart = -1;
    private int mLastSafeInsetTop = -1;
    private int mLastSafeInsetEnd = -1;
    private int mLastSafeInsetBottom = -1;
    private boolean mNeedsRestart;
    private int mDetailRequestToken;
    private Runnable mPendingCategoryOnFinish;
    private boolean mPasswordUnlocked;
    private ViewTreeObserver.OnGlobalFocusChangeListener mGlobalFocusListener;
    private boolean mLastFocusInDetail;
    private WeakReference<View> mLastFocusedDetailView = new WeakReference<>(null);

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
        if (savedInstanceState != null) {
            mSelectedRailPosition = savedInstanceState.getInt(STATE_SELECTED_RAIL_POS, 0);
        }
        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_RAIL_POS, mSelectedRailPosition);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRootLayout = (LinearLayout) view;
        mPaneSpacer = view.findViewById(R.id.settings_pane_spacer);
        mRailPane = view.findViewById(R.id.settings_rail_pane);
        mBackButton = view.findViewById(R.id.settings_back_button);
        mRailTitle = view.findViewById(R.id.settings_rail_title);
        mRailList = view.findViewById(R.id.settings_rail_list);
        mDetailPane = view.findViewById(R.id.settings_detail_pane);
        mDetailScroll = view.findViewById(R.id.settings_detail_scroll);
        mDetailContainer = view.findViewById(R.id.settings_detail_container);

        setupBackButton();
        stylePanes();
        setupRailList();
        setupResponsiveLayout();
        setupFocusRestoration(view);
        applyPendingUpdates();

        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyViewCreated(getMainFragmentAdapter());
            getMainFragmentAdapter().getFragmentHost().showTitleView(true);
        }

        if (savedInstanceState == null) {
            M3Motion.animateEnter(view, 14f * getResources().getDisplayMetrics().density, 0f);
        }
    }

    private void setupBackButton() {
        if (mBackButton == null) {
            return;
        }
        mBackButton.setButtonStyle(M3Button.STYLE_OUTLINED);
        mBackButton.setIconOnly(R.drawable.ic_m3_arrow_back);
        mBackButton.setNextFocusDownId(R.id.settings_rail_list);
        mBackButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        mBackButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
            int towardEndKey = isRtl ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                return focusFirstRailItem();
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                return true;
            }
            if (keyCode == towardEndKey) {
                if (mDetailContainer != null && findFirstFocusable(mDetailContainer) != null) {
                    focusDetailPane();
                }
                return true;
            }
            return false;
        });
    }

    private void setupResponsiveLayout() {
        if (mRootLayout == null) {
            return;
        }
        mLastLayoutWidth = -1;
        mLastLayoutHeight = -1;
        mLastSafeInsetStart = -1;
        mLastSafeInsetTop = -1;
        mLastSafeInsetEnd = -1;
        mLastSafeInsetBottom = -1;
        mRootLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int height = bottom - top;
            if (width > 0 && (width != mLastLayoutWidth || height != mLastLayoutHeight)) {
                mRootLayout.post(() -> applyResponsiveLayout(width, height));
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(mRootLayout, (v, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
            int insetStart = isRtl ? safe.right : safe.left;
            int insetEnd = isRtl ? safe.left : safe.right;
            if (insetStart != mSafeInsetStart || safe.top != mSafeInsetTop
                    || insetEnd != mSafeInsetEnd || safe.bottom != mSafeInsetBottom) {
                mSafeInsetStart = insetStart;
                mSafeInsetTop = safe.top;
                mSafeInsetEnd = insetEnd;
                mSafeInsetBottom = safe.bottom;
                if (mRootLayout.getWidth() > 0 && mRootLayout.getHeight() > 0) {
                    applyResponsiveLayout(mRootLayout.getWidth(), mRootLayout.getHeight());
                }
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(mRootLayout);
    }

    private void applyResponsiveLayout(int widthPx, int heightPx) {
        if (!isAdded() || getContext() == null || mRootLayout == null || widthPx <= 0) {
            return;
        }
        if (widthPx == mLastLayoutWidth && heightPx == mLastLayoutHeight
                && mSafeInsetStart == mLastSafeInsetStart && mSafeInsetTop == mLastSafeInsetTop
                && mSafeInsetEnd == mLastSafeInsetEnd && mSafeInsetBottom == mLastSafeInsetBottom) {
            return;
        }
        mLastLayoutWidth = widthPx;
        mLastLayoutHeight = heightPx;
        mLastSafeInsetStart = mSafeInsetStart;
        mLastSafeInsetTop = mSafeInsetTop;
        mLastSafeInsetEnd = mSafeInsetEnd;
        mLastSafeInsetBottom = mSafeInsetBottom;

        float density = getResources().getDisplayMetrics().density;
        float widthDp = widthPx / density;
        boolean stacked = widthDp < 600f;
        boolean medium = !stacked && widthDp < 840f;

        int baseSidePad = Math.round((stacked ? 16 : medium ? 24 : 44) * density);
        int baseTopPad = Math.round((stacked ? 16 : medium ? 20 : 24) * density);
        int baseBottomPad = Math.round((stacked ? 16 : medium ? 18 : 22) * density);
        int insetEdgeGap = Math.round((stacked ? 8 : 12) * density);
        int insetSideGap = Math.round((stacked ? 8 : 16) * density);

        int startPad = Math.max(baseSidePad, mSafeInsetStart + insetSideGap);
        int endPad = Math.max(baseSidePad, mSafeInsetEnd + insetSideGap);
        int topPad = Math.max(baseTopPad, mSafeInsetTop + insetEdgeGap);
        int bottomPad = Math.max(baseBottomPad, mSafeInsetBottom + insetEdgeGap);

        if (stacked) {
            mRootLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams railLp = (LinearLayout.LayoutParams) mRailPane.getLayoutParams();
            railLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            int maxRailH = Math.max(Math.round(136 * density), Math.round((heightPx - topPad - bottomPad) * 0.36f));
            railLp.height = Math.min(Math.round(196 * density), maxRailH);
            mRailPane.setLayoutParams(railLp);

            if (mPaneSpacer != null) {
                LinearLayout.LayoutParams spLp = (LinearLayout.LayoutParams) mPaneSpacer.getLayoutParams();
                spLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                spLp.height = Math.round(10 * density);
                mPaneSpacer.setLayoutParams(spLp);
            }

            if (mDetailPane != null) {
                LinearLayout.LayoutParams detLp = (LinearLayout.LayoutParams) mDetailPane.getLayoutParams();
                detLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                detLp.height = 0;
                detLp.weight = 1f;
                mDetailPane.setLayoutParams(detLp);

                int outerPad = Math.round(10 * density);
                int innerPad = Math.round(4 * density);
                mDetailPane.setPadding(outerPad, outerPad, outerPad, outerPad);
                mDetailContainer.setPadding(innerPad, innerPad, innerPad, innerPad);
            }
        } else {
            int railWidthPx = Math.round((medium ? 216 : 260) * density);
            int spacerWidthPx = Math.round((medium ? 12 : 18) * density);
            int maxDetailPx = Math.round(860 * density);

            mRootLayout.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams railLp = (LinearLayout.LayoutParams) mRailPane.getLayoutParams();
            railLp.width = railWidthPx;
            railLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mRailPane.setLayoutParams(railLp);

            if (mPaneSpacer != null) {
                LinearLayout.LayoutParams spLp = (LinearLayout.LayoutParams) mPaneSpacer.getLayoutParams();
                spLp.width = spacerWidthPx;
                spLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mPaneSpacer.setLayoutParams(spLp);
            }

            if (mDetailPane != null) {
                int availDetailPx = widthPx - startPad - endPad - railWidthPx - spacerWidthPx;
                LinearLayout.LayoutParams detLp = (LinearLayout.LayoutParams) mDetailPane.getLayoutParams();
                detLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                if (availDetailPx > maxDetailPx) {
                    detLp.width = maxDetailPx;
                    detLp.weight = 0f;
                } else {
                    detLp.width = 0;
                    detLp.weight = 1f;
                }
                mDetailPane.setLayoutParams(detLp);

                int outerPad = Math.round((medium ? 12 : 16) * density);
                int innerPad = Math.round(4 * density);
                mDetailPane.setPadding(outerPad, outerPad, outerPad, outerPad);
                mDetailContainer.setPadding(innerPad, innerPad, innerPad, innerPad);
            }
        }

        mRootLayout.setPaddingRelative(startPad, topPad, endPad, bottomPad);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRailAdapter != null && mRailList != null && !mRailList.hasFocus()) {
            mRailAdapter.notifyDataSetChanged();
        }
        renderSelectedDetail(false);
    }

    private void setupFocusRestoration(@NonNull View rootView) {
        if (rootView instanceof SettingsHubRootLayout) {
            ((SettingsHubRootLayout) rootView).setOnRestoreFocusListener((direction, previouslyFocusedRect) -> {
                if (mLastFocusInDetail && mDetailContainer != null) {
                    View last = mLastFocusedDetailView.get();
                    if (last != null && last.isAttachedToWindow() && last.isFocusable() && last.getVisibility() == View.VISIBLE) {
                        return last.requestFocus();
                    }
                    View first = findFirstFocusable(mDetailContainer);
                    if (first != null) {
                        return first.requestFocus();
                    }
                }
                if (mRailList != null && !mRailEntries.isEmpty()) {
                    int targetPos = (mSelectedRailPosition >= 0 && mSelectedRailPosition < mRailEntries.size()
                            && mRailEntries.get(mSelectedRailPosition).type != RailEntry.TYPE_HEADER)
                            ? mSelectedRailPosition
                            : findFirstFocusableRailPosition();
                    RecyclerView.ViewHolder vh = mRailList.findViewHolderForAdapterPosition(targetPos);
                    if (vh != null && vh.itemView.requestFocus()) {
                        return true;
                    }
                    if (mRailList.requestFocus()) {
                        return true;
                    }
                    // Defer focus until the rail finishes layout so the back arrow never steals initial focus.
                    mRailList.scrollToPosition(targetPos);
                    mRailList.post(() -> {
                        if (mRailList != null && isAdded()) {
                            RecyclerView.ViewHolder delayed = mRailList.findViewHolderForAdapterPosition(targetPos);
                            if (delayed != null) {
                                delayed.itemView.requestFocus();
                            }
                        }
                    });
                    return true;
                }
                return false;
            });
        }

        mGlobalFocusListener = (oldFocus, newFocus) -> {
            if (newFocus == null || mRootLayout == null) {
                return;
            }
            if (isDescendant(newFocus, mDetailContainer)) {
                mLastFocusInDetail = true;
                mLastFocusedDetailView = new WeakReference<>(newFocus);
            } else if (newFocus == mBackButton || isDescendant(newFocus, mRailList)) {
                mLastFocusInDetail = false;
            }
        };
        rootView.getViewTreeObserver().addOnGlobalFocusChangeListener(mGlobalFocusListener);
    }

    private static boolean isDescendant(@Nullable View child, @Nullable View parent) {
        if (child == null || parent == null) {
            return false;
        }
        ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        if (mGlobalFocusListener != null && mRootLayout != null && mRootLayout.getViewTreeObserver().isAlive()) {
            mRootLayout.getViewTreeObserver().removeOnGlobalFocusChangeListener(mGlobalFocusListener);
            mGlobalFocusListener = null;
        }
        if (mRootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mRootLayout, null);
        }
        if (getContext() != null) {
            AppDialogPresenter.instance(getContext()).captureNextDialog(null);
        }
        flushPendingCategoryFinish();
        super.onDestroyView();
    }

    private void stylePanes() {
        Context ctx = requireContext();
        int surfaceLow = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerLow, R.color.m3_grey_surface_container_low);
        int surfaceContainer = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainer, R.color.m3_grey_surface_container);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);

        float railRadius = ctx.getResources().getDimension(R.dimen.m3_dialog_corner_radius);
        float detailRadius = ctx.getResources().getDimension(R.dimen.m3_dialog_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        mRailPane.setBackground(ViewUtil.createRoundedDrawable(surfaceLow, outlineVariant, hairlinePx, railRadius));
        ViewUtil.applyRoundedOutline(mRailPane, railRadius);
        if (mDetailPane != null) {
            mDetailPane.setBackground(ViewUtil.createRoundedDrawable(surfaceContainer, outlineVariant, hairlinePx, detailRadius));
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                mDetailPane.setForeground(ViewUtil.createRoundedDrawable(Color.TRANSPARENT, outlineVariant, hairlinePx, detailRadius));
            }
            ViewUtil.applyRoundedOutline(mDetailPane, detailRadius);
        }
        mRailTitle.setTextColor(onSurface);
        if (mBackButton != null) {
            mBackButton.refreshThemeColors();
        }
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
            if (isPasswordProtected()) {
                promptPasswordAndUnlock(true);
            } else {
                focusDetailPane();
            }
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

    private int findFirstFocusableRailPosition() {
        for (int i = 0; i < mRailEntries.size(); i++) {
            if (mRailEntries.get(i).type != RailEntry.TYPE_HEADER) {
                return i;
            }
        }
        return 0;
    }

    private boolean focusFirstRailItem() {
        if (mRailList == null || mRailEntries.isEmpty()) {
            return false;
        }
        int firstPos = findFirstFocusableRailPosition();
        RecyclerView.ViewHolder vh = mRailList.findViewHolderForAdapterPosition(firstPos);
        if (vh != null && vh.itemView.requestFocus()) {
            return true;
        }
        mRailList.scrollToPosition(firstPos);
        mRailList.post(() -> {
            if (mRailList != null && isAdded()) {
                RecyclerView.ViewHolder delayed = mRailList.findViewHolderForAdapterPosition(firstPos);
                if (delayed != null) {
                    delayed.itemView.requestFocus();
                }
            }
        });
        return true;
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
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
                int towardStartKey = isRtl ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
                if (keyCode == towardStartKey) {
                    returnFocusToRail();
                    return true;
                }
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
        boolean restoreDetailFocus = !animate && mDetailContainer.hasFocus();
        mDetailRequestToken++;
        AppDialogPresenter.instance(getContext()).captureNextDialog(null);
        flushPendingCategoryFinish();

        RailEntry entry = mRailEntries.get(mSelectedRailPosition);
        mDetailContainer.removeAllViews();
        if (animate && mDetailScroll != null) {
            if (M3Motion.areAnimationsEnabled(getContext())) {
                mDetailScroll.smoothScrollTo(0, 0);
            } else {
                mDetailScroll.scrollTo(0, 0);
            }
        }

        if (entry.type == RailEntry.TYPE_QUICK_CONTROLS) {
            buildQuickControlsPane();
            if (restoreDetailFocus) {
                focusDetailPane();
            }
        } else if (entry.type == RailEntry.TYPE_CATEGORY && entry.settingsItem != null) {
            buildCategoryDetailPane(entry.settingsItem, restoreDetailFocus);
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
        if (mRestartBanner != null && mRestartBanner.getVisibility() != View.VISIBLE) {
            mRestartBanner.setVisibility(View.VISIBLE);
            if (getContext() != null) {
                float slidePx = M3Motion.SLIDE_OFFSET_COMPACT_DP * getResources().getDisplayMetrics().density;
                M3Motion.animateEnter(mRestartBanner, 0f, slidePx);
            }
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
    // Mode B: Inline Category Settings Panel
    // =========================================================================

    private void buildCategoryDetailPane(SettingsItem item, boolean restoreFocus) {
        Context ctx = requireContext();

        if (isPasswordProtected()) {
            renderLockedDetailPane(ctx, item, restoreFocus);
            return;
        }

        if (item.onClick == null) {
            InlineOptionsRenderer.renderCategoryHeader(ctx, mDetailContainer, item, null, null);
            InlineOptionsRenderer.addInfoMessage(ctx, mDetailContainer, getString(R.string.settings_empty_options));
            return;
        }

        final int requestToken = mDetailRequestToken;
        final boolean[] deliveredSync = {false};
        final boolean[] asyncPlaceholderShown = {false};
        final TextView[] headerSubtitle = new TextView[1];

        AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(ctx);
        dialogPresenter.captureNextDialog((title, categories, onFinish) -> {
            deliveredSync[0] = true;
            if (!isAdded() || getContext() == null || mDetailContainer == null || requestToken != mDetailRequestToken) {
                if (onFinish != null) {
                    onFinish.run();
                }
                return;
            }

            mPendingCategoryOnFinish = onFinish;
            mDetailContainer.removeAllViews();
            InlineOptionsRenderer.renderCategoryHeader(getContext(), mDetailContainer, item, title, headerSubtitle);
            InlineOptionsRenderer.renderCategories(
                    getContext(),
                    mDetailContainer,
                    categories != null ? categories : java.util.Collections.emptyList(),
                    new InlineOptionsRenderer.Host() {
                        @Override
                        public void attachReturnToRailOnLeft(@NonNull View view) {
                            SettingsGridFragment.this.attachReturnToRailOnLeft(view);
                        }

                        @Override
                        public void onOptionsMutated() {
                            refreshRailAndHeaderSummary(item, headerSubtitle[0]);
                        }
                    });
            if (asyncPlaceholderShown[0]) {
                float slidePx = M3Motion.SLIDE_OFFSET_COMPACT_DP * getResources().getDisplayMetrics().density;
                M3Motion.animateEnter(mDetailContainer, slidePx, 0f);
            }
            if (restoreFocus) {
                focusDetailPane();
            }
        });

        item.onClick.run();

        if (!deliveredSync[0] && isAdded() && getContext() != null && mDetailContainer != null && requestToken == mDetailRequestToken) {
            asyncPlaceholderShown[0] = true;
            InlineOptionsRenderer.renderCategoryHeader(ctx, mDetailContainer, item, null, headerSubtitle);
            InlineOptionsRenderer.addInfoMessage(ctx, mDetailContainer, getString(R.string.settings_loading_options));
        }
    }

    private void renderLockedDetailPane(Context ctx, SettingsItem item, boolean restoreFocus) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int surfaceHigh = ViewUtil.getThemeColor(ctx, R.attr.m3SurfaceContainerHigh, R.color.m3_grey_surface_container_high);
        int outlineVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OutlineVariant, R.color.m3_grey_outline_variant);
        int onSurface = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurface, R.color.m3_on_surface);
        int onSurfaceVariant = ViewUtil.getThemeColor(ctx, R.attr.m3OnSurfaceVariant, R.color.m3_on_surface_variant);
        float cardRadius = ctx.getResources().getDimension(R.dimen.m3_card_corner_radius);
        int hairlinePx = ctx.getResources().getDimensionPixelSize(R.dimen.m3_hairline_stroke_width);

        InlineOptionsRenderer.renderCategoryHeader(ctx, mDetailContainer, item, null, null);

        LinearLayout lockCard = new LinearLayout(ctx);
        lockCard.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(18 * density);
        lockCard.setPadding(pad, pad, pad, pad);
        lockCard.setClipChildren(false);
        lockCard.setClipToPadding(false);
        lockCard.setBackground(ViewUtil.createRoundedDrawable(surfaceHigh, outlineVariant, hairlinePx, cardRadius));

        TextView titleView = new TextView(ctx);
        titleView.setText(R.string.settings_locked_title);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTextColor(onSurface);
        lockCard.addView(titleView);

        TextView msgView = new TextView(ctx);
        msgView.setText(R.string.settings_locked_message);
        msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        msgView.setTextColor(onSurfaceVariant);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.topMargin = Math.round(6 * density);
        msgLp.bottomMargin = Math.round(14 * density);
        lockCard.addView(msgView, msgLp);

        M3Button unlockBtn = new M3Button(ctx);
        unlockBtn.setButtonStyle(M3Button.STYLE_FILLED);
        unlockBtn.setIcon(R.drawable.ic_m3_chevron_right);
        unlockBtn.setText(getString(R.string.settings_unlock));
        unlockBtn.setOnClickListener(v -> promptPasswordAndUnlock(true));
        attachReturnToRailOnLeft(unlockBtn);
        lockCard.addView(unlockBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSpacedRow(lockCard, Math.round(14 * density));
        if (restoreFocus) {
            focusDetailPane();
        }
    }

    private boolean isPasswordProtected() {
        if (getContext() == null || mPasswordUnlocked) {
            return false;
        }
        return GeneralData.instance(getContext()).getSettingsPassword() != null;
    }

    private void promptPasswordAndUnlock(boolean focusAfterUnlock) {
        if (getContext() == null) {
            return;
        }
        String password = GeneralData.instance(getContext()).getSettingsPassword();
        if (password == null) {
            mPasswordUnlocked = true;
            renderSelectedDetail(true);
            if (focusAfterUnlock) {
                focusDetailPane();
            }
            return;
        }
        SimpleEditDialog.showPassword(
                getContext(),
                getContext().getString(R.string.enter_settings_password),
                null,
                newValue -> {
                    if (Utils.passwordMatch(password, newValue)) {
                        mPasswordUnlocked = true;
                        renderSelectedDetail(true);
                        if (focusAfterUnlock) {
                            focusDetailPane();
                        }
                        return true;
                    }
                    return false;
                });
    }

    private void refreshRailAndHeaderSummary(SettingsItem item, @Nullable TextView subtitleView) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        if (subtitleView != null) {
            String summary = SettingsHubHelper.getSubtitleSummary(getContext(), item);
            if (!TextUtils.isEmpty(summary)) {
                subtitleView.setText(summary);
                subtitleView.setVisibility(View.VISIBLE);
            } else {
                subtitleView.setVisibility(View.GONE);
            }
        }
        if (mRailAdapter != null && mRailList != null && !mRailList.hasFocus()
                && mSelectedRailPosition >= 0 && mSelectedRailPosition < mRailEntries.size()) {
            mRailAdapter.notifyItemChanged(mSelectedRailPosition);
        }
    }

    private void flushPendingCategoryFinish() {
        if (mPendingCategoryOnFinish != null) {
            Runnable callback = mPendingCategoryOnFinish;
            mPendingCategoryOnFinish = null;
            callback.run();
        }
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
                    M3Motion.animateFocusScale(v, focused, M3Motion.FOCUS_SCALE_SUBTLE);
                });

                int firstFocusablePos = findFirstFocusableRailPosition();
                itemView.setNextFocusUpId(position == firstFocusablePos ? R.id.settings_back_button : View.NO_ID);
                itemView.setOnClickListener(v -> onRailEntryClicked(position));
                itemView.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        boolean isRtl = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_RTL;
                        int towardEndKey = isRtl ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && position == firstFocusablePos && mBackButton != null) {
                            return mBackButton.requestFocus();
                        }
                        if (keyCode == towardEndKey) {
                            focusDetailPane();
                            return true;
                        }
                    }
                    return false;
                });
            }
        }
    }
}
