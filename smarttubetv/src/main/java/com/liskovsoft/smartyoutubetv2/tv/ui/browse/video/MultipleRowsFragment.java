package com.liskovsoft.smartyoutubetv2.tv.ui.browse.video;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.RowPresenter.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.VideoGroupPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.adapter.VideoGroupObjectAdapter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ChannelHeaderPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ChannelHeaderPresenter.ChannelHeaderCallback;
import com.liskovsoft.smartyoutubetv2.tv.presenter.HeroSpotlightPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.HeroSpotlightRow;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ShelfChipPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ShelfChipRow;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ShortsCardPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.VideoCardPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.CustomListRowPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.OnItemLongPressedListener;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.interfaces.VideoSection;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.UriBackgroundManager;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.m3e.M3ChipBarView.ChipItem;
import com.liskovsoft.smartyoutubetv2.tv.util.M3Motion;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MultipleRowsFragment extends RowsSupportFragment implements VideoSection {
    private static final String TAG = MultipleRowsFragment.class.getSimpleName();
    private UriBackgroundManager mBackgroundManager;
    private ArrayObjectAdapter mRowsAdapter;
    private ListRowPresenter mRowPresenter;
    private Map<Integer, VideoGroupObjectAdapter> mVideoGroupAdapters;
    private final List<VideoGroup> mPendingUpdates = new ArrayList<>();
    private VideoGroupPresenter mMainPresenter;
    private VideoCardPresenter mCardPresenter;
    private ShortsCardPresenter mShortsPresenter;
    private int mSelectedRowIndex = -1;
    private ChannelHeaderCallback mChannelHeaderCallback;
    private HeroSpotlightRow mHeroSpotlightRow;
    private HeroSpotlightPresenter mHeroSpotlightPresenter;
    private ShelfChipRow mShelfChipRow;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mMainPresenter = getMainPresenter();
        mCardPresenter = new VideoCardPresenter();
        mShortsPresenter = new ShortsCardPresenter();
        mBackgroundManager = ((LeanbackActivity) getActivity()).getBackgroundManager();

        setupAdapter();
        setupEventListeners();
        applyPendingUpdates();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            M3Motion.animateEnter(view, 12f * getResources().getDisplayMetrics().density, 0f);
        }
        if (getVerticalGridView() != null) {
            getVerticalGridView().setClipChildren(false);
            getVerticalGridView().addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (mHeroSpotlightPresenter != null) {
                        mHeroSpotlightPresenter.onScrollChanged();
                    }
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (mHeroSpotlightPresenter != null) {
                        mHeroSpotlightPresenter.onScrollDragging(newState == RecyclerView.SCROLL_STATE_DRAGGING);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHeroSpotlightPresenter != null) {
            mHeroSpotlightPresenter.onHostResume();
        }
    }

    @Override
    public void onPause() {
        if (mHeroSpotlightPresenter != null) {
            mHeroSpotlightPresenter.onHostPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mHeroSpotlightPresenter != null) {
            mHeroSpotlightPresenter.onHostStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mHeroSpotlightPresenter != null) {
            mHeroSpotlightPresenter.onHostDestroy();
        }
        super.onDestroyView();
    }

    protected void addHeader(ChannelHeaderCallback callback) {
        mChannelHeaderCallback = callback;
    }

    protected boolean isHeroSpotlightEnabled() {
        return false;
    }

    private int getHeaderRowCount() {
        int count = 0;
        if (mChannelHeaderCallback != null) count++;
        if (mHeroSpotlightRow != null) count++;
        if (mShelfChipRow != null) count++;
        return count;
    }

    protected abstract VideoGroupPresenter getMainPresenter();

    private void applyPendingUpdates() {
        // prevent modification within update method
        List<VideoGroup> copyArray = new ArrayList<>(mPendingUpdates);

        mPendingUpdates.clear();

        for (VideoGroup group : copyArray) {
            update(group);
        }
    }

    private void setupAdapter() {
        if (mVideoGroupAdapters == null) {
            mVideoGroupAdapters = new HashMap<>();
        }

        if (mRowsAdapter == null) {
            mRowPresenter = new CustomListRowPresenter();
            mRowPresenter.enableChildRoundedCorners(getMainUIData().isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS));

            mHeroSpotlightPresenter = new HeroSpotlightPresenter();
            ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
            presenterSelector.addClassPresenter(ListRow.class, mRowPresenter);
            presenterSelector.addClassPresenter(ChannelHeaderCallback.class, new ChannelHeaderPresenter());
            presenterSelector.addClassPresenter(HeroSpotlightRow.class, mHeroSpotlightPresenter);
            presenterSelector.addClassPresenter(ShelfChipRow.class, new ShelfChipPresenter());

            mRowsAdapter = new ArrayObjectAdapter(presenterSelector);
            setAdapter(mRowsAdapter);
        }
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
        mCardPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());
        mShortsPresenter.setOnItemViewLongPressedListener(new ItemViewLongPressedListener());
    }

    @Override
    public void clear() {
        mHeroSpotlightRow = null;
        mShelfChipRow = null;
        if (mRowsAdapter != null) {
            mRowsAdapter.clear();
            if (mChannelHeaderCallback != null) {
                mRowsAdapter.add(mChannelHeaderCallback);
            }
        }

        if (mVideoGroupAdapters != null) {
            mVideoGroupAdapters.clear();
        }

        // Reset the position (bug appeared after fragment been reused)
        setPosition(mChannelHeaderCallback != null ? 1 : 0);
    }

    private void removeByIndex(int idx) {
        if (mRowsAdapter != null && mRowsAdapter.size() > idx) {
            ListRow row = (ListRow) mRowsAdapter.get(idx);
            mRowsAdapter.remove(row);
            VideoGroupObjectAdapter group = (VideoGroupObjectAdapter) row.getAdapter();
            mVideoGroupAdapters.values().remove(group);
        }
    }

    private void removeById(int id) {
        if (mRowsAdapter != null) {
            VideoGroupObjectAdapter needed = mVideoGroupAdapters.get(id);
            for (int i = 0; i < mRowsAdapter.size(); i++) {
                Object row = mRowsAdapter.get(i);

                if (row instanceof ListRow) {
                    VideoGroupObjectAdapter adapter = (VideoGroupObjectAdapter) ((ListRow) row).getAdapter();
                    if (adapter == needed) {
                        mRowsAdapter.remove(row);
                        mVideoGroupAdapters.remove(id);
                    }
                }
            }
        }
    }

    private int findPositionById(int id) {
        if (mRowsAdapter != null) {
            VideoGroupObjectAdapter needed = mVideoGroupAdapters.get(id);
            for (int i = 0; i < mRowsAdapter.size(); i++) {
                Object row = mRowsAdapter.get(i);

                if (row instanceof ListRow) {
                    VideoGroupObjectAdapter adapter = (VideoGroupObjectAdapter) ((ListRow) row).getAdapter();
                    if (adapter == needed) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private boolean isComputingLayout(VideoGroup group) {
        int action = group.getAction();

        // Attempt to fix: IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
        if ((action == VideoGroup.ACTION_SYNC || action == VideoGroup.ACTION_REPLACE) && getVerticalGridView() != null) {
            if (getVerticalGridView().isComputingLayout()) {
                return true;
            }
            int position = findPositionById(group.getId());
            if (position != -1) {
                RecyclerView.ViewHolder viewHolder = getVerticalGridView().findViewHolderForAdapterPosition(position);
                if (viewHolder != null) {
                    Object nestedRecyclerView = Helpers.getField(viewHolder, "mNestedRecyclerView");
                    if (nestedRecyclerView instanceof WeakReference) {
                        Object recyclerView = ((WeakReference<?>) nestedRecyclerView).get();
                        return recyclerView instanceof RecyclerView && ((RecyclerView) recyclerView).isComputingLayout();
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isEmpty() {
        if (mRowsAdapter == null) {
            return mPendingUpdates.isEmpty();
        }

        return mRowsAdapter.size() == 0;
    }

    @Override
    public void update(VideoGroup group) {
        if (isComputingLayout(group)) {
            return;
        }

        if (mVideoGroupAdapters == null) {
            mPendingUpdates.add(group);
            return;
        }

        // Correct position depending on header rows presence
        int rawGroupPosition = group.getPosition();
        if (rawGroupPosition != -1 && mChannelHeaderCallback != null) {
            group.setPosition(rawGroupPosition + 1);
        }

        int action = group.getAction();

        if (action == VideoGroup.ACTION_REPLACE) {
            if (group.getPosition() == -1) {
                clear();
            } else {
                removeById(group.getId());
            }
        } else if (action == VideoGroup.ACTION_REMOVE) {
            VideoGroupObjectAdapter adapter = mVideoGroupAdapters.get(group.getId());
            if (adapter != null) {
                adapter.remove(group);
            }
            return;
        } else if (action == VideoGroup.ACTION_SYNC) {
            VideoGroupObjectAdapter adapter = mVideoGroupAdapters.get(group.getId());
            if (adapter != null) {
                freeze(true);
                adapter.sync(group);
                freeze(false);
            }
            return;
        }

        if (group.isEmpty()) {
            return;
        }

        VideoGroupObjectAdapter existingAdapter = GridFragmentHelper.findRelatedAdapter(mVideoGroupAdapters, group, this::freeze);

        if (existingAdapter == null) {
            HeaderItem rowHeader = new HeaderItem(group.getTitle());
            int videoGroupId = group.getId(); // Create unique int from category.

            VideoGroupObjectAdapter videoGroupAdapter = new VideoGroupObjectAdapter(group, group.isShorts() ? mShortsPresenter : mCardPresenter);

            mVideoGroupAdapters.put(videoGroupId, videoGroupAdapter);

            ListRow row = new ListRow(rowHeader, videoGroupAdapter);

            if (isHeroSpotlightEnabled() && mChannelHeaderCallback == null) {
                if (mHeroSpotlightRow == null) {
                    ensureHeroSpotlight(group);
                } else {
                    topUpHeroSpotlight(group);
                }
            }

            int headerOffset = mChannelHeaderCallback == null ? getHeaderRowCount() : 0;
            int targetPos = group.getPosition() == -1
                    ? mRowsAdapter.size()
                    : Math.min(mRowsAdapter.size(), Math.max(headerOffset, group.getPosition() + headerOffset));

            if (group.getPosition() == -1 || targetPos >= mRowsAdapter.size()) {
                mRowsAdapter.add(row);
            } else {
                mRowsAdapter.add(targetPos, row);
            }

            if (isHeroSpotlightEnabled() && mChannelHeaderCallback == null) {
                syncShelfChips();
            }
        } else {
            Log.d(TAG, "Continue row %s %s", group.getTitle(), System.currentTimeMillis());

            freeze(true);

            existingAdapter.add(group); // continue

            freeze(false);

            if (isHeroSpotlightEnabled() && mChannelHeaderCallback == null) {
                if (mHeroSpotlightRow == null) {
                    ensureHeroSpotlight(group);
                } else {
                    topUpHeroSpotlight(group);
                }
            }
        }

        M3Motion.animateInitialVisibleChildren(getVerticalGridView());

        restorePosition();
    }

    private void ensureHeroSpotlight(VideoGroup firstGroup) {
        if (firstGroup == null || firstGroup.getVideos() == null || firstGroup.getVideos().isEmpty()) {
            return;
        }
        String label = firstGroup.getSection() != null && firstGroup.getSection().getTitle() != null
                ? firstGroup.getSection().getTitle()
                : firstGroup.getTitle();
        HeroSpotlightRow hero = new HeroSpotlightRow(label, firstGroup.getVideos(), new HeroSpotlightRow.OnHeroActionListener() {
            @Override
            public void onWatchClicked(Video video, long previewPositionMs) {
                if (mMainPresenter != null && video != null) {
                    if (previewPositionMs >= 0L) {
                        // Seek full playback to exact preview position (floor at 1ms when frame 0 rendered
                        // so VideoStateController.restorePendingPosition overrides any older saved state)
                        video.pendingPosMs = Math.max(1L, previewPositionMs);
                    }
                    mMainPresenter.onVideoItemClicked(video);
                }
            }

            @Override
            public void onVideoHighlighted(Video video) {
                if (mBackgroundManager != null) {
                    mBackgroundManager.setBackgroundFrom(video);
                }
            }
        });
        if (!hero.getVideos().isEmpty()) {
            mHeroSpotlightRow = hero;
            mRowsAdapter.add(0, mHeroSpotlightRow);
            if (mBackgroundManager != null && mHeroSpotlightRow.getCurrentVideo() != null) {
                mBackgroundManager.setBackgroundFrom(mHeroSpotlightRow.getCurrentVideo());
            }
        }
    }

    private void topUpHeroSpotlight(VideoGroup group) {
        if (mHeroSpotlightRow == null || group == null || group.isShorts() || group.getVideos() == null) {
            return;
        }
        if (mHeroSpotlightRow.getVideos().size() >= HeroSpotlightRow.MAX_SPOTLIGHT_VIDEOS) {
            return;
        }
        if (mHeroSpotlightRow.appendVideos(group.getVideos()) && mHeroSpotlightPresenter != null) {
            mHeroSpotlightPresenter.onVideosUpdated();
        }
    }

    private void syncShelfChips() {
        if (mRowsAdapter == null || mHeroSpotlightRow == null) {
            return;
        }
        List<ChipItem> chips = new ArrayList<>();
        int listRowCount = 0;
        for (int i = 0; i < mRowsAdapter.size(); i++) {
            Object obj = mRowsAdapter.get(i);
            if (obj instanceof ListRow) {
                listRowCount++;
                HeaderItem hi = ((ListRow) obj).getHeaderItem();
                String title = hi != null && hi.getName() != null ? hi.getName() : "";
                if (!title.isEmpty()) {
                    chips.add(new ChipItem(i, title));
                }
            }
        }
        if (listRowCount < 2 || chips.isEmpty()) {
            return;
        }
        if (mShelfChipRow == null) {
            mShelfChipRow = new ShelfChipRow(targetRowPos -> {
                if (targetRowPos >= 0 && targetRowPos < mRowsAdapter.size()) {
                    Object targetRow = mRowsAdapter.get(targetRowPos);
                    if (targetRow instanceof ListRow) {
                        setSelectedPosition(targetRowPos, true, new ListRowPresenter.SelectItemViewHolderTask(0));
                    } else {
                        setSelectedPosition(targetRowPos, true);
                    }
                }
            });
            mShelfChipRow.setChips(chips, 0);
            mRowsAdapter.add(1, mShelfChipRow);
            // Recompute chip row targets now that ShelfChipRow shifted ListRow indices by +1
            syncShelfChips();
        } else {
            mShelfChipRow.setChips(chips, mShelfChipRow.getSelectedIndex());
            mRowsAdapter.notifyArrayItemRangeChanged(1, 1);
        }
    }

    private void restorePosition() {
        setPosition(mSelectedRowIndex);

        // Maybe we don't need to load next group since all rows already fetched?
    }

    @Override
    public int getPosition() {
        return getSelectedPosition();
    }

    @Override
    public void setPosition(int index) {
        if (index < 0) {
            return;
        }

        if (mRowsAdapter != null && index < mRowsAdapter.size()) {
            setSelectedPosition(index, false);
            mSelectedRowIndex = -1;
        } else {
            mSelectedRowIndex = index;
        }
    }

    @Override
    public void selectItem(Video item) {
        // NOP
    }

    /**
     * Disable scrolling on partially updated rows. This prevent cards from misbehaving.
     */
    private void freeze(boolean freeze) {
        // Disable scrolling on partially updated rows. This prevent controls from misbehaving.
        if (mRowPresenter != null) {
            ViewHolder vh = getRowViewHolder(getSelectedPosition());
            if (vh instanceof ListRowPresenter.ViewHolder) {
                mRowPresenter.freeze(vh, freeze);
            }
        }
    }

    private MainUIData getMainUIData() {
        return MainUIData.instance(getContext());
    }

    private final class ItemViewLongPressedListener implements OnItemLongPressedListener {
        @Override
        public void onItemLongPressed(Presenter.ViewHolder itemViewHolder, Object item) {

            if (item instanceof Video) {
                mMainPresenter.onVideoItemLongClicked((Video) item);
            } else {
                Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final class ItemViewClickedListener implements androidx.leanback.widget.OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                mMainPresenter.onVideoItemClicked((Video) item);
            } else {
                Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                mBackgroundManager.setBackgroundFrom((Video) item);

                mMainPresenter.onVideoItemSelected((Video) item);

                checkScrollEnd((Video)item);
            }
        }

        private void checkScrollEnd(Video item) {
            for (VideoGroupObjectAdapter adapter : mVideoGroupAdapters.values()) {
                int index = adapter.indexOf(item);

                if (index != -1) {
                    int size = adapter.size();
                    if (index > (size - ViewUtil.ROW_SCROLL_CONTINUE_NUM)) {
                        mMainPresenter.onScrollEnd((Video) adapter.get(size - 1));
                    }
                    break;
                }
            }
        }
    }
}
