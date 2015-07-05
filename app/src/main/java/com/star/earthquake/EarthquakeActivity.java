package com.star.earthquake;


import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


public class EarthquakeActivity extends AppCompatActivity {

    private static final int MENU_PREFERENCES = Menu.FIRST + 1;
    private static final int MENU_UPDATE = Menu.FIRST + 2;

    private static final int SHOW_PREFERENCES = 1;

    private boolean autoUpdateChecked = false;
    private int updateFreq = 0;
    private int minMag = 0;

    private EarthquakeTabListener<EarthquakeListFragment> listTabListener;
    private EarthquakeTabListener<EarthquakeMapFragment> mapTabListener;

    private static final String ACTION_BAR_INDEX = "ACTION_BAR_INDEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake);

        updateFromPreferences();

        ActionBar actionBar = getSupportActionBar();

        View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);

        if (fragmentContainer != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setDisplayShowTitleEnabled(false);

            ActionBar.Tab listTab = actionBar.newTab();

            listTabListener = new EarthquakeTabListener<>(this,
                    R.id.EarthquakeFragmentContainer, EarthquakeListFragment.class);

            listTab.setText("List")
                    .setContentDescription("List of earthquakes")
                    .setTabListener(listTabListener);

            actionBar.addTab(listTab);

            ActionBar.Tab mapTab = actionBar.newTab();

            mapTabListener = new EarthquakeTabListener<>(this,
                    R.id.EarthquakeFragmentContainer, EarthquakeMapFragment.class);

            mapTab.setText("Map")
                    .setContentDescription("Map of earthquakes")
                    .setTabListener(mapTabListener);

            actionBar.addTab(mapTab);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);

        if (fragmentContainer != null) {
            int actionBarIndex = getSupportActionBar().getSelectedTab().getPosition();

            SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
            editor.putInt(ACTION_BAR_INDEX, actionBarIndex);
            editor.apply();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            if (listTabListener.getFragment() != null) {
                ft.detach(listTabListener.getFragment());
            }

            if (mapTabListener.getFragment() != null) {
                ft.detach(mapTabListener.getFragment());
            }

            ft.commit();
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);

        if (fragmentContainer != null) {
            listTabListener.setFragment(getSupportFragmentManager().findFragmentByTag(
                    EarthquakeListFragment.class.getName()
            ));
            mapTabListener.setFragment(getSupportFragmentManager().findFragmentByTag(
                    EarthquakeMapFragment.class.getName()
            ));

            SharedPreferences sharedPreferences = getPreferences(Activity.MODE_PRIVATE);

            getSupportActionBar().setSelectedNavigationItem(
                    sharedPreferences.getInt(ACTION_BAR_INDEX, 0));

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater menuInflater = getMenuInflater();

        menuInflater.inflate(R.menu.main_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchableInfo searchableInfo =
                searchManager.getSearchableInfo(getComponentName());

        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

        searchView.setSearchableInfo(searchableInfo);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivity(new Intent(this, EarthquakeAddActivity.class));
                return true;

            case R.id.menu_refresh:
                startService(new Intent(this, EarthquakeUpdateService.class));
                return true;

            case R.id.menu_preferences:
                Class clazz = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
                        UserPreferenceActivity.class : UserFragmentPreferenceActivity.class;

                Intent i = new Intent(this, clazz);
                startActivityForResult(i, SHOW_PREFERENCES);
                return true;
            default:
                return false;
        }
    }

    private void updateFromPreferences() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        autoUpdateChecked =
                sharedPreferences.getBoolean(UserPreferenceActivity.PREF_AUTO_UPDATE, false);

        updateFreq = Integer.parseInt(
                sharedPreferences.getString(UserPreferenceActivity.PREF_UPDATE_FREQ, "60"));

        minMag = Integer.parseInt(
                        sharedPreferences.getString(UserPreferenceActivity.PREF_MIN_MAG, "3"));

    }

    public int getMinMag() {
        return minMag;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHOW_PREFERENCES) {
            updateFromPreferences();

            startService(new Intent(this, EarthquakeUpdateService.class));
        }
    }
}
