package com.star.earthquake;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.Date;

public class EarthquakeListFragment extends ListFragment {

    private static final int EARTHQUAKE_LOADER = 0;

    private SimpleCursorAdapter simpleCursorAdapter;

    private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks =
            new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            String[] projection = new String[] {
                    EarthquakeProvider.KEY_ID,
                    EarthquakeProvider.KEY_SUMMARY
            };

            EarthquakeActivity earthquakeActivity = (EarthquakeActivity) getActivity();

            String selection = EarthquakeProvider.KEY_MAGNITUDE + " > " +
                    earthquakeActivity.getMinMag();

            CursorLoader cursorLoader = new CursorLoader(getActivity(),
                    EarthquakeProvider.CONTENT_URI, projection, selection, null, null);

            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            simpleCursorAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            simpleCursorAdapter.swapCursor(null);
        }
    };

    private static final String TAG = "EARTHQUAKE";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        simpleCursorAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null,
                new String[] { EarthquakeProvider.KEY_SUMMARY },
                new int[] { android.R.id.text1 }, 0);

        setListAdapter(simpleCursorAdapter);

        getLoaderManager().initLoader(EARTHQUAKE_LOADER, null, loaderCallbacks);

        refreshEarthquakes();

    }

    public void refreshEarthquakes() {

        getLoaderManager().restartLoader(EARTHQUAKE_LOADER, null, loaderCallbacks);

        getActivity().startService(new Intent(getActivity(), EarthquakeUpdateService.class));

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        ContentResolver contentResolver = getActivity().getContentResolver();

        Cursor cursor = contentResolver.query(ContentUris.
                withAppendedId(EarthquakeProvider.CONTENT_URI, id), null, null, null, null);

        if (cursor.moveToFirst()) {
            Date date = new Date(cursor.getLong(
                    cursor.getColumnIndex(EarthquakeProvider.KEY_DATE)));

            String details = cursor.getString(
                    cursor.getColumnIndex(EarthquakeProvider.KEY_DETAILS));

            double magnitude = cursor.getDouble(
                    cursor.getColumnIndex(EarthquakeProvider.KEY_MAGNITUDE));

            String link = cursor.getString(
                    cursor.getColumnIndex(EarthquakeProvider.KEY_LINK));

            double latitude = cursor.getDouble(
                    cursor.getColumnIndex(EarthquakeProvider.KEY_LOCATION_LAT));

            double longitude = cursor.getDouble(
                    cursor.getColumnIndex(EarthquakeProvider.KEY_LOCATION_LON));

            Location location = new Location("db");
            location.setLatitude(latitude);
            location.setLongitude(longitude);

            Quake quake = new Quake(date, details, location, magnitude, link);

            DialogFragment dialogFragment = EarthquakeDialog.newInstance(getActivity(), quake);

            dialogFragment.show(getFragmentManager(), "dialog");
        }
    }
}
