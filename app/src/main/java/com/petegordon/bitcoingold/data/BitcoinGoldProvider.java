package com.petegordon.bitcoingold.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by petegordon on 12/20/14.
 */
public class BitcoinGoldProvider extends ContentProvider {


    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private BitcoinGoldDbHelper mOpenHelper;

    private static final int BITCOINGOLD = 100;
    private static final int BITCOINGOLD_DATE = 101;

    private static final SQLiteQueryBuilder sBitcoinGoldQueryBuilder;

    static{
        sBitcoinGoldQueryBuilder = new SQLiteQueryBuilder();
        sBitcoinGoldQueryBuilder.setTables(BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME);
    }

    private static final String sBitcoinGoldWithStartDateSelection =
            BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME+
                    "." + BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT + " >= ? ";

    private static final String sBitcoinGoldAndDaySelection =
            BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME+
                    "." + BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT + " = ? ";

    private static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        //Really are regular expressions that bad?  Why do content provider URIs when you
        //don't care to share your data with other apps?

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = BitcoinGoldContract.CONTENT_AUTHORITY;



        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, BitcoinGoldContract.PATH_BITCOINGOLD, BITCOINGOLD);
        matcher.addURI(authority, BitcoinGoldContract.PATH_BITCOINGOLD + "/*", BITCOINGOLD_DATE);

        return matcher;
    }

    private Cursor getBitcoinGoldPrices(Uri uri, String[] projection, String sortOrder) {
        String startDate = BitcoinGoldContract.BitcoinGoldEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = new String(); //all bitcoingold days in the database
            selectionArgs = new String[]{};
        } else {
            selectionArgs = new String[]{startDate};
            selection = sBitcoinGoldWithStartDateSelection;
        }

        return sBitcoinGoldQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getBitcoinGoldPriceByDate(
            Uri uri, String[] projection, String sortOrder) {
        String date = BitcoinGoldContract.BitcoinGoldEntry.getDateFromUri(uri);

        return sBitcoinGoldQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sBitcoinGoldAndDaySelection,
                new String[]{date},
                null,
                null,
                sortOrder
        );
    }

    @Override
    public boolean onCreate() {
            mOpenHelper = new BitcoinGoldDbHelper(getContext());
            return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            // Here's the switch statement that, given a URI, will determine what kind of request it is,
            // and query the database accordingly.
            Cursor retCursor;
            switch (sUriMatcher.match(uri)) {
                // "bitcoingold"
                case BITCOINGOLD:
                {
                    retCursor = getBitcoinGoldPrices(uri, projection, sortOrder);
                    break;
                }
                // "bitcoingold/*"
                case BITCOINGOLD_DATE: {
                    retCursor = getBitcoinGoldPriceByDate(uri, projection, sortOrder);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
            return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case BITCOINGOLD:
                return BitcoinGoldContract.BitcoinGoldEntry.CONTENT_ITEM_TYPE;
            case BITCOINGOLD_DATE:
                return BitcoinGoldContract.BitcoinGoldEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case BITCOINGOLD: {
                long _id = db.insert(BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = BitcoinGoldContract.BitcoinGoldEntry.buildBitcoinGoldUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case BITCOINGOLD:
                rowsDeleted = db.delete(
                        BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case BITCOINGOLD:
                rowsUpdated = db.update(BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BITCOINGOLD:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(BitcoinGoldContract.BitcoinGoldEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
