package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Row;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leanback Row model backing the Home / Browse Hero Spotlight stage.
 */
public class HeroSpotlightRow extends Row {
    public static final int MAX_SPOTLIGHT_VIDEOS = 4;

    public interface OnHeroActionListener {
        void onWatchClicked(Video video, long previewPositionMs);
        void onVideoHighlighted(Video video);
    }

    private final List<Video> mVideos = new ArrayList<>();
    private final String mSectionLabel;
    private OnHeroActionListener mListener;
    private int mSelectedIndex;

    public HeroSpotlightRow(String sectionLabel, @NonNull List<Video> videos, @NonNull OnHeroActionListener listener) {
        mSectionLabel = sectionLabel;
        mListener = listener;
        setVideos(videos);
    }

    public void setVideos(@NonNull List<Video> videos) {
        mVideos.clear();
        appendVideos(videos);
        if (mSelectedIndex >= mVideos.size()) {
            mSelectedIndex = 0;
        }
    }

    /**
     * Appends unique playable items up to {@link #MAX_SPOTLIGHT_VIDEOS}.
     *
     * @return true if at least one item was added
     */
    public boolean appendVideos(@NonNull List<Video> videos) {
        if (mVideos.size() >= MAX_SPOTLIGHT_VIDEOS) {
            return false;
        }
        boolean added = false;
        for (Video v : videos) {
            if (mVideos.size() >= MAX_SPOTLIGHT_VIDEOS) {
                break;
            }
            if (isEligible(v) && !containsVideo(v)) {
                mVideos.add(v);
                added = true;
            }
        }
        return added;
    }

    private static boolean isEligible(Video v) {
        return v != null
                && !v.isShorts
                && v.getTitle() != null
                && (v.hasVideo() || v.hasPlaylist() || v.hasChannel());
    }

    private boolean containsVideo(@NonNull Video candidate) {
        for (Video existing : mVideos) {
            if (existing == candidate) {
                return true;
            }
            if (!TextUtils.isEmpty(candidate.videoId)
                    && TextUtils.equals(existing.videoId, candidate.videoId)) {
                return true;
            }
        }
        return false;
    }

    public List<Video> getVideos() {
        return Collections.unmodifiableList(mVideos);
    }

    public String getSectionLabel() {
        return mSectionLabel;
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        if (mVideos.isEmpty()) {
            mSelectedIndex = 0;
            return;
        }
        mSelectedIndex = ((selectedIndex % mVideos.size()) + mVideos.size()) % mVideos.size();
    }

    public Video getCurrentVideo() {
        if (mVideos.isEmpty()) {
            return null;
        }
        return mVideos.get(Math.max(0, Math.min(mSelectedIndex, mVideos.size() - 1)));
    }

    public OnHeroActionListener getListener() {
        return mListener;
    }

    public void setListener(OnHeroActionListener listener) {
        mListener = listener;
    }
}
