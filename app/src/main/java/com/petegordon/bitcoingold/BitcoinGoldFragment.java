package com.petegordon.bitcoingold;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.petegordon.bitcoingold.data.BitcoinGoldContract;
import com.petegordon.bitcoingold.data.BitcoinGoldProvider;
import com.petegordon.bitcoingold.sync.BitcoinGoldSyncAdapter;
import java.util.Calendar;
import java.util.Date;

/**
 * A fragment for the BitcoinGold view of 14 day price history
 */

public class BitcoinGoldFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = BitcoinGoldFragment.class.getSimpleName();
    private BitcoinGoldAdapter mBitcoinGoldAdapter;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;

    private static final String SELECTED_KEY = "selected_position";

    private static final int BITCOINGOLD_LOADER = 0;

    private static final String BITCOINGOLD_HASH_TAG = " #BitcoinGoldApp";

    private ShareActionProvider mShareActionProvider;
    private String mBitcoinGoldShareMessage;

    public BitcoinGoldFragment() {
        setHasOptionsMenu(true);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(BITCOINGOLD_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        System.out.println("onCreateView");



        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mBitcoinGoldAdapter = new BitcoinGoldAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_bitcoingold);
        mListView.setAdapter(mBitcoinGoldAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mBitcoinGoldAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback)getActivity())
                            .onItemSelected(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_DATE));
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }


        mBitcoinGoldAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mBitcoinGoldShareMessage != null) {
            mShareActionProvider.setShareIntent(createShareBitcoinGoldIntent());
        }
    }

    /**
     * Query the database for the latest Bitcoin to Gold Price for the MainActivity.BitcoinGoldFragment ShareProvider
     */
    private void createShareBitcoinGold(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.roll(Calendar.DAY_OF_YEAR, -3);
        Date startDate = calendar.getTime();
        String startDateString = BitcoinGoldContract.getDbDateString(startDate);

        Cursor todayCursor = getActivity().getContentResolver().query(
                BitcoinGoldContract.BitcoinGoldEntry.buildBitcoinGoldWithStartDate(startDateString),
                BitcoinGoldContract.BitcoinGoldEntry.BITCOINGOLD_COLUMNS,
                null,
                null,
                BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT + " DESC"
        );

        if(todayCursor.moveToFirst()) {
            String priceRatio = todayCursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_PRICE_RATIO);
            String priceBitcoin = Utility.getFormattedCurrencyValue(todayCursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PRICE));
            String priceGold = Utility.getFormattedCurrencyValue(todayCursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PRICE));
            String priceDate = Utility.getFormattedMonthDay(getActivity(), todayCursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_DATE));

            // We still need this for the share intent
            mBitcoinGoldShareMessage = String.format("%s Bitcoin (%s) to Gold (%s) - %s   ", priceRatio, priceBitcoin, priceGold, priceDate);

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareBitcoinGoldIntent());
            }
        }
    }
    private Intent createShareBitcoinGoldIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mBitcoinGoldShareMessage + BITCOINGOLD_HASH_TAG);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return bitcoin pricing the past 14 days.

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.roll(Calendar.DAY_OF_YEAR, -(BitcoinGoldContract.NUMBER_OF_DAYS));
        Date startDate = calendar.getTime();
        String startDateString = BitcoinGoldContract.getDbDateString(startDate);

        // Sort order:  Ascending, by date.
        String sortOrder = BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT + " DESC";

        Uri bitcoinGoldWithStartDate = BitcoinGoldContract.BitcoinGoldEntry.buildBitcoinGoldWithStartDate(startDateString);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                bitcoinGoldWithStartDate,
                BitcoinGoldContract.BitcoinGoldEntry.BITCOINGOLD_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mBitcoinGoldAdapter.swapCursor(cursor);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }


        createShareBitcoinGold();


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBitcoinGoldAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mBitcoinGoldAdapter != null) {
            mBitcoinGoldAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

}
