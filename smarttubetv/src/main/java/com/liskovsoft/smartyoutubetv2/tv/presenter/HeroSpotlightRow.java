package com.liskovsoft.smartyoutubetv2.tv.presenter;

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
        for (Video v : videos) {
            if (v != null && v.getTitle() != null && (v.hasVideo() || v.hasPlaylist() || v.hasChannel())) {
                mVideos.add(v);
                if (mVideos.size() >= 6) {
                    break;
                }
            }
        }
        if (mSelectedIndex >= mVideos.size()) {
            mSelectedIndex = 0;
        }
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
