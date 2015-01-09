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
import android.widget.ImageView;
import android.widget.TextView;

import com.petegordon.bitcoingold.data.BitcoinGoldContract;

/**
 * Created by petegordon on 12/27/14.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();


    private String mDateStr;

    private static final int DETAIL_LOADER = 0;

    private static final String[] BITCOINGOLD_COLUMNS = {
            BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME + "." + BitcoinGoldContract.BitcoinGoldEntry._ID,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_BITCOIN_PRICE,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_BITCOIN_DATETIME,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_BITCOIN_PERCENT_CHANGE,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_GOLD_PRICE,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_GOLD_DATETIME,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_GOLD_PERCENT_CHANGE,
            BitcoinGoldContract.BitcoinGoldEntry.COLUMN_PRICE_RATIO
    };

    private TextView bitcoinGoldRatioView;
    private ImageView bitcoinGoldArrowView;
    private TextView bitcoinGoldDateView;

    private ImageView bitcoinArrowView;
    private ImageView goldArrowView;

    private TextView goldPriceView;
    private TextView bitcoinPriceView;

    private TextView bitcoinDateTextView;
    private TextView goldDateTextView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        outState.putString(LOCATION_KEY, mLocation);
//        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mDateStr = arguments.getString(DetailActivity.DATE_KEY);
        }

        if (savedInstanceState != null) {
//            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }

        View rootView = inflater.inflate(R.layout.bitcoingold_day_detail, container, false);
        bitcoinGoldRatioView = (TextView) rootView.findViewById(R.id.list_item_bitcoingold_ratio_textview);
        bitcoinGoldDateView = (TextView) rootView.findViewById(R.id.list_item_bitcoingold_date_textview);
        bitcoinArrowView = (ImageView) rootView.findViewById(R.id.list_item_bitcoin_direction_icon);
        goldArrowView = (ImageView) rootView.findViewById(R.id.list_item_gold_direction_icon);
        goldPriceView = (TextView) rootView.findViewById(R.id.list_item_gold_price_textview);
        bitcoinPriceView = (TextView) rootView.findViewById(R.id.list_item_bitcoin_price_textview);
        bitcoinDateTextView = (TextView) rootView.findViewById(R.id.list_item_bitcoin_datetime_textview);
        goldDateTextView = (TextView) rootView.findViewById(R.id.list_item_gold_datetime_textview);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.DATE_KEY) ) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
  //          mLocation = savedInstanceState.getString(LOCATION_KEY);
        }

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.DATE_KEY)) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Sort order:  Ascending, by date.
        String sortOrder = BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT + " DESC";

//        mLocation = Utility.getPreferredLocation(getActivity());


        Uri bitcoinGoldWithDateURI = BitcoinGoldContract.BitcoinGoldEntry.buildBitcoinGoldWithDate(mDateStr);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                bitcoinGoldWithDateURI,
                BITCOINGOLD_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {

            // Read date from cursor
            String dateString = Utility.getFriendlyDayString(getActivity(), cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_DATE));
            String priceRatio = null;
            String priceBitcoin = null;
            String priceGold = null;

            if(dateString != null) {
                if(bitcoinGoldDateView != null) {
                    bitcoinGoldDateView.setText(dateString);
                }

                if(bitcoinGoldRatioView != null && !cursor.isNull(BitcoinGoldContract.BitcoinGoldEntry.COL_PRICE_RATIO)){
                    priceRatio = Utility.getFormattedCurrencyValue(cursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_PRICE_RATIO));
                    bitcoinGoldRatioView.setText(priceRatio);
                }

                if(bitcoinDateTextView != null && !cursor.isNull(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_DATETIME)){
                    bitcoinDateTextView.setText(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_DATETIME));
                }
                if(goldDateTextView != null && !cursor.isNull(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_DATETIME)){
                    goldDateTextView.setText(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_DATETIME));
                }
                priceBitcoin = Utility.getFormattedCurrencyValue(cursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PRICE));
                bitcoinPriceView.setText(priceBitcoin);
                if (cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PRICE) != null) {
                    priceGold = Utility.getFormattedCurrencyValue(cursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PRICE));
                    goldPriceView.setText(priceGold);
                } else {
                    goldPriceView.setText("");
                }

                if (Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PERCENT_CHANGE)) > 0) {
                    bitcoinArrowView.setImageResource(R.drawable.uparrow);
                } else {
                    bitcoinArrowView.setImageResource(R.drawable.downarrow);
                }

                boolean noChange = false;
                if (cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PERCENT_CHANGE) != null) {
                    if (Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PERCENT_CHANGE)) > 0) {
                        goldArrowView.setImageResource(R.drawable.uparrow);
                    } else if(Float.parseFloat(cursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PERCENT_CHANGE)) < 0) {
                        goldArrowView.setImageResource(R.drawable.downarrow);
                    } else {
                        noChange = true;
                    }
                }else{
                    noChange = true;
                }

                if(noChange) {
                    goldArrowView.setImageResource(R.drawable.nochange);
                }


            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }
}
