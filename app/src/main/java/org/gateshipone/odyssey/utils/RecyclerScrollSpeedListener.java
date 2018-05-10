/*
 * Copyright (C) 2018 Team Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.utils;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.viewitems.GenericImageViewItem;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class RecyclerScrollSpeedListener extends RecyclerView.OnScrollListener {

    private long mLastTime = 0;

    private int mLastFirstVisibleItem = 0;

    private int mScrollSpeed = 0;

    private final ScrollSpeedAdapter mAdapter;

    public RecyclerScrollSpeedListener(final ScrollSpeedAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
        if (newState == SCROLL_STATE_IDLE) {
            // if idle load images for all visible items
            mScrollSpeed = 0;
            mAdapter.setScrollSpeed(0);

            final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

            if (layoutManager instanceof LinearLayoutManager) {
                final int firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                final int lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();

                final int visibleItemCount = lastVisibleItemPosition - firstVisibleItemPosition;

                for (int i = 0; i <= visibleItemCount; i++) {
                    GenericImageViewItem item = (GenericImageViewItem) recyclerView.getChildAt(i);
                    item.startCoverImageTask();
                }
            }
        }
    }

    @Override
    public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (dx == 0 && dy == 0) {
            /*
             * This callback will also be called if visible item range changes after a layout
             * calculation. In that case, dx and dy will be 0.
             */
            mScrollSpeed = 0;
            mAdapter.setScrollSpeed(0);

            if (layoutManager instanceof LinearLayoutManager) {
                final int firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                final int lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();

                final int visibleItemCount = lastVisibleItemPosition - firstVisibleItemPosition;

                for (int i = 0; i <= visibleItemCount; i++) {
                    GenericImageViewItem item = (GenericImageViewItem) recyclerView.getChildAt(i);
                    item.startCoverImageTask();
                }
            }

            return;
        }

        int firstVisibleItem;
        int visibleItemCount;

        if (layoutManager instanceof LinearLayoutManager) {
            firstVisibleItem = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
            visibleItemCount = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition() - firstVisibleItem;
        } else {
            return;
        }

        // New row started if this is true.
        if (firstVisibleItem != mLastFirstVisibleItem) {
            final long currentTime = System.currentTimeMillis();
            if (currentTime == mLastTime) {
                return;
            }
            // Calculate the duration of scroll per line
            final long timeScrollPerRow = currentTime - mLastTime;

            if (layoutManager instanceof GridLayoutManager) {
                mScrollSpeed = (int) (1000 / timeScrollPerRow) * ((GridLayoutManager) layoutManager).getSpanCount();
            } else {
                mScrollSpeed = (int) (1000 / timeScrollPerRow);
            }

            // Calculate how many items per second of loading images is possible
            final int possibleItems = (int) (1000 / mAdapter.getAverageImageLoadTime());

            // Set the scrollspeed in the adapter
            mAdapter.setScrollSpeed(mScrollSpeed);

            // Save values for next comparsion
            mLastFirstVisibleItem = firstVisibleItem;
            mLastTime = currentTime;
            // Start the grid image loader task only if scroll speed is slow enough:
            // The devices is able to render the images needed for the scroll speed
            if (mScrollSpeed < possibleItems) {
                for (int i = 0; i < visibleItemCount; i++) {
                    GenericImageViewItem item = (GenericImageViewItem) recyclerView.getChildAt(i);
                    item.startCoverImageTask();
                }
            }
        }
    }
}
