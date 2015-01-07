package com.petegordon.bitcoingold.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by petegordon on 12/20/14.
 */
public class BitcoinGoldContract {


    public static final int NUMBER_OF_DAYS = 14;
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.petegordon.bitcoingold";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.petegordon.bitcoingold/bitcoingold is a valid path for
    // looking at bitcoingold price data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_BITCOINGOLD = "bitcoingold";

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    /**
     * Converts Date class to a string representation, used for easy comparison and database lookup.
     * @param date The input date
     * @return a DB-friendly representation of the date, using the format defined in DATE_FORMAT.
     */
    public static String getDbDateString(Date date){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(date);
    }

    /**
     * Converts a dateText to a long Unix time representation
     * @param dateText the input date string
     * @return the Date object
     */
    public static Date getDateFromDb(String dateText) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dbDateFormat.parse(dateText);
        } catch ( ParseException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /* Inner class that defines the table contents of the location table */
    public static final class BitcoinGoldEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BITCOINGOLD).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BITCOINGOLD;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BITCOINGOLD;

        // Table name
        public static final String TABLE_NAME = "bitcoingold_price";

        public static final String COLUMN_DATETEXT = "date";
        public static final String COLUMN_BITCOIN_PRICE = "bitcoin_price";
        public static final String COLUMN_BITCOIN_DATETIME = "bitcoin_datetime";
        public static final String COLUMN_BITCOIN_PERCENT_CHANGE = "bitcoin_daily_percent_change";
        public static final String COLUMN_GOLD_PRICE = "gold_price";
        public static final String COLUMN_GOLD_DATETIME = "gold_datetime";
        public static final String COLUMN_GOLD_PERCENT_CHANGE = "gold_daily_percent_change";
        public static final String COLUMN_PRICE_RATIO = "bitcoingold_price_ratio";
        public static final String COLUMN_PRICE_RATIO_PERCENT_CHANGE = "bitcoingold_price_ratio_percent_change";
        public static final String COLUMN_200DAY_MOVING_AVERAGE = "bitcoingold_200day_moving_average";
        public static final String COLUMN_50DAY_MOVING_AVERAGE = "bitcoingold_50day_moving_average";
        public static final String COLUMN_10DAY_MOVING_AVERAGE = "bitcoingold_10day_moving_average";

        public static final String[] BITCOINGOLD_COLUMNS = {
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

        public static final int COL_ID = 0;
        public static final int COL_DATE = 1;
        public static final int COL_BITCOIN_PRICE = 2;
        public static final int COL_BITCOIN_DATETIME = 3;
        public static final int COL_BITCOIN_PERCENT_CHANGE = 4;
        public static final int COL_GOLD_PRICE = 5;
        public static final int COL_GOLD_DATETIME = 6;
        public static final int COL_GOLD_PERCENT_CHANGE = 7;
        public static final int COL_PRICE_RATIO = 8;


        public static Uri buildBitcoinGoldUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        public static Uri buildBitcoinGoldWithStartDate(String startDate) {
            return CONTENT_URI.buildUpon()
                    .appendQueryParameter(COLUMN_DATETEXT, startDate).build();
        }

        public static Uri buildBitcoinGoldWithDate(String date) {
            return CONTENT_URI.buildUpon().appendPath(date).build();
        }

        public static String getDateFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getStartDateFromUri(Uri uri) {
            return uri.getQueryParameter(COLUMN_DATETEXT);
        }

    }

}
