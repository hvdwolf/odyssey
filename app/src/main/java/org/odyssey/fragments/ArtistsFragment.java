/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

package org.odyssey.fragments;

import android.content.Context;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.odyssey.R;
import org.odyssey.adapter.ArtistsGridViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.ArtistLoader;
import org.odyssey.models.ArtistModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.ScrollSpeedListener;
import org.odyssey.utils.ThemeUtils;

import java.util.List;

public class ArtistsFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<ArtistModel>>, AdapterView.OnItemClickListener {
    /**
     * GridView adapter object used for this GridView
     */
    private ArtistsGridViewAdapter mArtistsGridViewAdapter;

    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    /**
     * Save the root GridView for later usage.
     */
    private GridView mRootGrid;

    /**
     * Save the swipe layout for later usage
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * Save the last scroll position to resume there
     */
    private int mLastPosition;

    /**
     * ServiceConnection object to communicate with the PlaybackService
     */
    private PlaybackServiceConnection mServiceConnection;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.grid_refresh, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.grid_refresh_gridview);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.grid_refresh_swipe_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mArtistsGridViewAdapter = new ArtistsGridViewAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mArtistsGridViewAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mArtistsGridViewAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);

        return rootView;
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mArtistSelectedCallback = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    /**
     * Called when the fragment resumes.
     * Reload the data and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        // change refresh state
        mSwipeRefreshLayout.setRefreshing(true);
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<ArtistModel>> onCreateLoader(int id, Bundle bundle) {
        return new ArtistLoader(getActivity());
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param model  Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<ArtistModel>> loader, List<ArtistModel> model) {
        // Set the actual data to the adapter.
        mArtistsGridViewAdapter.swapModel(model);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }

        // change refresh state
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<ArtistModel>> loader) {
        // Clear the model data of the adapter.
        mArtistsGridViewAdapter.swapModel(null);
    }

    /**
     * generic method to reload the dataset displayed by the fragment
     */
    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    /**
     * Callback when an item in the ListView was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Save scroll position
        mLastPosition = position;

        // identify current artist
        ArtistModel currentArtist = (ArtistModel) mArtistsGridViewAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artist, artistID);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artists_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_artist_action_enqueue:
                enqueueArtist(info.position);
                return true;
            case R.id.fragment_artist_action_play:
                playArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Apply the given filter to the model of the adapter.
     */
    @Override
    public void applyFilter(String filter) {
        mArtistsGridViewAdapter.applyFilter(filter);
    }

    /**
     * Remove a previous set filter.
     */
    @Override
    public void removeFilter() {
        mArtistsGridViewAdapter.removeFilter();
    }

    /**
     * Call the PBS to enqueue the selected artist
     *
     * @param position the position of the selected artist in the adapter
     */
    private void enqueueArtist(int position) {

        // identify current artist
        ArtistModel currentArtist = (ArtistModel) mArtistsGridViewAdapter.getItem(position);

        String artist = currentArtist.getArtistName();
        long artistID = currentArtist.getArtistID();

        if (artistID == -1) {
            // Try to get the artistID manually because it seems to be missing
            artistID = MusicLibraryHelper.getArtistIDFromName(artist, getActivity());
        }

        // enqueue artist
        try {
            mServiceConnection.getPBS().enqueueArtist(artistID);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play the selected artist.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected artist in the adapter
     */
    private void playArtist(int position) {

        // Remove old tracks
        try {
            mServiceConnection.getPBS().clearPlaylist();
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // get and enqueue all albums of the current artist
        enqueueArtist(position);

        // play album
        try {
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
