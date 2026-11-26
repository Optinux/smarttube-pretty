/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package androidx.leanback.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.leanback.R;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DividerPresenter;
import androidx.leanback.widget.DividerRow;
import androidx.leanback.widget.FocusHighlightHelper;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowHeaderPresenter;
import androidx.leanback.widget.SectionRow;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * An fragment containing a list of row headers. Implementation must support three types of rows:
 * <ul>
 *     <li>{@link DividerRow} rendered by {@link DividerPresenter}.</li>
 *     <li>{@link Row} rendered by {@link RowHeaderPresenter}.</li>
 *     <li>{@link SectionRow} rendered by {@link RowHeaderPresenter}.</li>
 * </ul>
 * Use {@link #setPresenterSelector(PresenterSelector)} in subclass constructor to customize
 * Presenters. App may override {@link BrowseSupportFragment#onCreateHeadersSupportFragment()}.
 */
public class HeadersSupportFragment extends BaseRowSupportFragment {

    /**
     * Interface definition for a callback to be invoked when a header item is clicked.
     */
    public interface OnHeaderClickedListener {
        /**
         * Called when a header item has been clicked.
         *
         * @param viewHolder Row ViewHolder object corresponding to the selected Header.
         * @param row Row object corresponding to the selected Header.
         */
        void onHeaderClicked(RowHeaderPresenter.ViewHolder viewHolder, Row row);
    }

    /**
     * Interface definition for a callback to be invoked when a header item is selected.
     */
    public interface OnHeaderViewSelectedListener {
        /**
         * Called when a header item has been selected.
         *
         * @param viewHolder Row ViewHolder object corresponding to the selected Header.
         * @param row Row object corresponding to the selected Header.
         */
        void onHeaderSelected(RowHeaderPresenter.ViewHolder viewHolder, Row row);
    }

    private OnHeaderViewSelectedListener mOnHeaderViewSelectedListener;
    OnHeaderClickedListener mOnHeaderClickedListener;
    private boolean mHeadersEnabled = true;
    private boolean mHeadersGone = false;
    private int mBackgroundColor;
    private boolean mBackgroundColorSet;

    private static final PresenterSelector sHeaderPresenter = new ClassPresenterSelector()
            .addClassPresenter(DividerRow.class, new DividerPresenter())
            .addClassPresenter(SectionRow.class,
                    new RowHeaderPresenter(R.layout.lb_section_header, false))
            .addClassPresenter(Row.class, new RowHeaderPresenter(R.layout.lb_header));

    public HeadersSupportFragment() {
        setPresenterSelector(sHeaderPresenter);
        FocusHighlightHelper.setupHeaderItemFocusHighlight(getBridgeAdapter());
    }

    public void setOnHeaderClickedListener(OnHeaderClickedListener listener) {
        mOnHeaderClickedListener = listener;
    }

    public void setOnHeaderViewSelectedListener(OnHeaderViewSelectedListener listener) {
        mOnHeaderViewSelectedListener = listener;
    }

    @Override
    VerticalGridView findGridViewFromRoot(View view) {
        return (VerticalGridView) view.findViewById(R.id.browse_headers);
    }

    @Override
    void onRowSelected(RecyclerView parent, RecyclerView.ViewHolder viewHolder,
            int position, int subposition) {
        if (mOnHeaderViewSelectedListener != null) {
            if (viewHolder != null && position >= 0) {
                ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder) viewHolder;
                mOnHeaderViewSelectedListener.onHeaderSelected(
                        (RowHeaderPresenter.ViewHolder) vh.getViewHolder(), (Row) vh.getItem());
            } else {
                mOnHeaderViewSelectedListener.onHeaderSelected(null, null);
            }
        }
    }

    private final ItemBridgeAdapter.AdapterListener mAdapterListener =
            new ItemBridgeAdapter.AdapterListener() {
        @Override
        public void onCreate(final ItemBridgeAdapter.ViewHolder viewHolder) {
            View headerView = viewHolder.getViewHolder().view;
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnHeaderClickedListener != null) {
                        mOnHeaderClickedListener.onHeaderClicked(
                                (RowHeaderPresenter.ViewHolder) viewHolder.getViewHolder(),
                                (Row) viewHolder.getItem());
                    }
                }
            });
            if (mWrapper != null) {
                viewHolder.itemView.addOnLayoutChangeListener(sLayoutChangeListener);
            } else {
                headerView.addOnLayoutChangeListener(sLayoutChangeListener);
            }
        }

    };

    static OnLayoutChangeListener sLayoutChangeListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {
            v.setPivotX(v.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? v.getWidth() : 0);
            v.setPivotY(v.getMeasuredHeight() / 2);
        }
    };

    @Override
    int getLayoutResourceId() {
        return R.layout.lb_headers_fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final VerticalGridView listView = getVerticalGridView();
        if (listView == null) {
            return;
        }
        listView.setBackground(null);
        if (mBackgroundColorSet) {
            updateFadingEdgeToBrandColor(mBackgroundColor);
        } else {
            Drawable d = listView.getBackground();
            if (d instanceof ColorDrawable) {
                updateFadingEdgeToBrandColor(((ColorDrawable) d).getColor());
            } else {
                updateFadingEdgeToBrandColor(0xFF161920);
            }
        }
        updateListViewVisibility();
    }

    private void updateListViewVisibility() {
        final VerticalGridView listView = getVerticalGridView();
        if (listView != null) {
            getView().setVisibility(mHeadersGone ? View.GONE : View.VISIBLE);
            View fadingView = getView().findViewById(R.id.fade_out_edge);
            if (!mHeadersGone) {
                if (mHeadersEnabled) {
                    listView.setChildrenVisibility(View.VISIBLE);
                    if (fadingView != null) {
                        fadingView.setVisibility(View.VISIBLE);
                    }
                } else {
                    listView.setChildrenVisibility(View.INVISIBLE);
                    if (fadingView != null) {
                        fadingView.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
    }

    void setHeadersEnabled(boolean enabled) {
        mHeadersEnabled = enabled;
        updateListViewVisibility();
    }

    void setHeadersGone(boolean gone) {
        mHeadersGone = gone;
        updateListViewVisibility();
    }

    static class NoOverlappingFrameLayout extends FrameLayout {

        public NoOverlappingFrameLayout(Context context) {
            super(context);
        }

        /**
         * Avoid creating hardware layer for header dock.
         */
        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
    }

    // Wrapper needed because of conflict between RecyclerView's use of alpha
    // for ADD animations, and RowHeaderPresenter's use of alpha for selected level.
    final ItemBridgeAdapter.Wrapper mWrapper = new ItemBridgeAdapter.Wrapper() {
        @Override
        public void wrap(View wrapper, View wrapped) {
            ((FrameLayout) wrapper).addView(wrapped);
        }

        @Override
        public View createWrapper(View root) {
            return new NoOverlappingFrameLayout(root.getContext());
        }
    };
    @Override
    void updateAdapter() {
        super.updateAdapter();
        ItemBridgeAdapter adapter = getBridgeAdapter();
        adapter.setAdapterListener(mAdapterListener);
        adapter.setWrapper(mWrapper);
    }

    void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundColorSet = true;

        if (getVerticalGridView() != null) {
            getVerticalGridView().setBackground(null);
            updateFadingEdgeToBrandColor(mBackgroundColor);
        }
    }

    private void updateFadingEdgeToBrandColor(int backgroundColor) {
        View root = getView();
        if (root == null) {
            return;
        }
        View fadingView = root.findViewById(R.id.fade_out_edge);
        if (fadingView == null) {
            return;
        }
        int panelWidthPx = root.getResources().getDimensionPixelSize(R.dimen.lb_browse_headers_width);
        int fadeLengthPx = root.getResources().getDimensionPixelSize(R.dimen.lb_browse_header_fading_length);
        int totalWidthPx = panelWidthPx + fadeLengthPx;
        ViewGroup.LayoutParams lp = fadingView.getLayoutParams();
        if (lp != null && lp.width != totalWidthPx) {
            lp.width = totalWidthPx;
            fadingView.setLayoutParams(lp);
        }
        boolean initialRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
        fadingView.setBackground(new SidebarScrimDrawable(backgroundColor, panelWidthPx, fadeLengthPx, initialRtl));
    }

    /**
     * Draws a continuous overlaid dense frosted-glass sidebar surface that stays readable over
     * bright backdrops and fades smoothly across its trailing edge via a multi-stop eased curve.
     */
    private static final class SidebarScrimDrawable extends Drawable {
        // Dense frosted-glass surface alpha (~0.94 in solid area; within 0.85-0.95, not a flat slab)
        private static final int SURFACE_SOLID_ALPHA = 240;
        private static final float SURFACE_FROST_TINT_RATIO = 0.035f;
        private static final int SURFACE_FROST_TINT_COLOR = 0xFFFFFFFF;
        private static final float BRIGHT_SURFACE_LUMINANCE_THRESHOLD = 0.25f;
        private static final int DARK_FALLBACK_SURFACE_COLOR = 0xFF0F1115;
        private static final float DARK_FALLBACK_BLEND_RATIO = 0.72f;

        // 5-stop smoothstep-eased trailing edge fade starting gently from SURFACE_SOLID_ALPHA
        private static final float[] EDGE_FADE_PROGRESS_STOPS = {0.00f, 0.18f, 0.38f, 0.58f, 0.78f, 1.00f};
        private static final int[] EDGE_FADE_ALPHAS = {
                SURFACE_SOLID_ALPHA,
                219,
                162,
                91,
                30,
                0
        };

        // Subtle dark backing scrim behind sidebar labels for contrast over bright backdrops
        private static final int LABEL_SCRIM_START_ALPHA = 28;
        private static final int LABEL_SCRIM_EDGE_ALPHA = 18;
        private static final int LABEL_SCRIM_MID_ALPHA = 6;
        private static final float LABEL_SCRIM_MID_PROGRESS = 0.35f;
        private static final float LABEL_SCRIM_END_PROGRESS = 0.72f;

        // Whisper-thin 1px specular hairline along the trailing panel edge (never a hard border)
        private static final float HAIRLINE_WIDTH_PX = 1f;
        private static final int HAIRLINE_COLOR = 0xFFFFFFFF;
        private static final int HAIRLINE_PEAK_ALPHA = 18;
        private static final float HAIRLINE_VERTICAL_FADE_STOP = 0.14f;

        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mLabelScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mHairlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int mBaseColor;
        private final int mPanelWidthPx;
        private final int mFadeLengthPx;
        private final boolean mInitialRtl;
        private float mHairlineX;
        private int mLastWidth = -1;
        private int mLastHeight = -1;
        private int mLastLayoutDirection = -1;

        SidebarScrimDrawable(int brandColor, int panelWidthPx, int fadeLengthPx, boolean initialRtl) {
            // Ensure dark surface readability even if a light/bright surface token is active
            int opaque = ColorUtils.setAlphaComponent(brandColor, 255);
            if (ColorUtils.calculateLuminance(opaque) > BRIGHT_SURFACE_LUMINANCE_THRESHOLD) {
                opaque = ColorUtils.blendARGB(opaque, DARK_FALLBACK_SURFACE_COLOR, DARK_FALLBACK_BLEND_RATIO);
            }
            // Subtle frosted-glass lift derived from the theme surface color
            mBaseColor = ColorUtils.blendARGB(opaque, SURFACE_FROST_TINT_COLOR, SURFACE_FROST_TINT_RATIO);
            mPanelWidthPx = Math.max(1, panelWidthPx);
            mFadeLengthPx = Math.max(1, fadeLengthPx);
            mInitialRtl = initialRtl;
            mHairlinePaint.setStyle(Paint.Style.STROKE);
            mHairlinePaint.setStrokeWidth(HAIRLINE_WIDTH_PX);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty()) {
                return;
            }
            int dir = getLayoutDirectionCompat();
            if (b.width() != mLastWidth || b.height() != mLastHeight || dir != mLastLayoutDirection) {
                mLastWidth = b.width();
                mLastHeight = b.height();
                mLastLayoutDirection = dir;
                rebuildShaders(b, dir == ViewCompat.LAYOUT_DIRECTION_RTL);
            }
            canvas.drawRect(b, mPaint);
            canvas.drawRect(b, mLabelScrimPaint);
            canvas.drawLine(mHairlineX, b.top, mHairlineX, b.bottom, mHairlinePaint);
        }

        private int getLayoutDirectionCompat() {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                return getLayoutDirection();
            }
            return mInitialRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR;
        }

        private void rebuildShaders(Rect b, boolean rtl) {
            float total = Math.max(1f, (float) (mPanelWidthPx + mFadeLengthPx));
            float edgeStop = Math.min(0.95f, mPanelWidthPx / total);
            float fadeSpan = 1f - edgeStop;

            int[] colors = new int[EDGE_FADE_PROGRESS_STOPS.length + 1];
            float[] stops = new float[EDGE_FADE_PROGRESS_STOPS.length + 1];
            colors[0] = ColorUtils.setAlphaComponent(mBaseColor, SURFACE_SOLID_ALPHA);
            stops[0] = 0f;
            for (int i = 0; i < EDGE_FADE_PROGRESS_STOPS.length; i++) {
                int alpha = EDGE_FADE_ALPHAS[i];
                colors[i + 1] = alpha == 0 ? Color.TRANSPARENT : ColorUtils.setAlphaComponent(mBaseColor, alpha);
                stops[i + 1] = edgeStop + fadeSpan * EDGE_FADE_PROGRESS_STOPS[i];
            }

            float startX = rtl ? b.right : b.left;
            float endX = rtl ? b.left : b.right;
            mPaint.setShader(new LinearGradient(
                    startX, 0f, endX, 0f, colors, stops, Shader.TileMode.CLAMP));

            int[] labelScrimColors = new int[]{
                    Color.argb(LABEL_SCRIM_START_ALPHA, 0, 0, 0),
                    Color.argb(LABEL_SCRIM_EDGE_ALPHA, 0, 0, 0),
                    Color.argb(LABEL_SCRIM_MID_ALPHA, 0, 0, 0),
                    Color.TRANSPARENT
            };
            float[] labelScrimStops = new float[]{
                    0f,
                    edgeStop,
                    edgeStop + fadeSpan * LABEL_SCRIM_MID_PROGRESS,
                    Math.min(1f, edgeStop + fadeSpan * LABEL_SCRIM_END_PROGRESS)
            };
            mLabelScrimPaint.setShader(new LinearGradient(
                    startX, 0f, endX, 0f, labelScrimColors, labelScrimStops, Shader.TileMode.CLAMP));

            mHairlineX = rtl ? (b.right - mPanelWidthPx) : (b.left + mPanelWidthPx);
            int hairlinePeak = ColorUtils.setAlphaComponent(HAIRLINE_COLOR, HAIRLINE_PEAK_ALPHA);
            mHairlinePaint.setShader(new LinearGradient(
                    0f, b.top, 0f, b.bottom,
                    new int[]{Color.TRANSPARENT, hairlinePeak, hairlinePeak, Color.TRANSPARENT},
                    new float[]{0f, HAIRLINE_VERTICAL_FADE_STOP, 1f - HAIRLINE_VERTICAL_FADE_STOP, 1f},
                    Shader.TileMode.CLAMP));
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
            mLabelScrimPaint.setAlpha(alpha);
            mHairlinePaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
            mLabelScrimPaint.setColorFilter(colorFilter);
            mHairlinePaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Override
    public void onTransitionStart() {
        super.onTransitionStart();
        if (!mHeadersEnabled) {
            // When enabling headers fragment,  the RowHeaderView gets a focus but
            // isShown() is still false because its parent is INVISIBLE, accessibility
            // event is not sent.
            // Workaround is: prevent focus to a child view during transition and put
            // focus on it after transition is done.
            final VerticalGridView listView = getVerticalGridView();
            if (listView != null) {
                listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                if (listView.hasFocus()) {
                    listView.requestFocus();
                }
            }
        }
    }

    @Override
    public void onTransitionEnd() {
        if (mHeadersEnabled) {
            final VerticalGridView listView = getVerticalGridView();
            if (listView != null) {
                listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                if (listView.hasFocus()) {
                    listView.requestFocus();
                }
            }
        }
        super.onTransitionEnd();
    }

    public boolean isScrolling() {
        return getVerticalGridView().getScrollState()
                != HorizontalGridView.SCROLL_STATE_IDLE;
    }
}
