package com.petegordon.bitcoingold.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.petegordon.bitcoingold.data.BitcoinGoldContract.BitcoinGoldEntry;
/**
 * Created by petegordon on 12/20/14.
 */
public class BitcoinGoldDbHelper extends SQLiteOpenHelper {


    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "bitcoingold.db";



    public BitcoinGoldDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold bitcoingold records.

        final String SQL_CREATE_BITCOINGOLD_TABLE = "CREATE TABLE " + BitcoinGoldEntry.TABLE_NAME + " (" +

                BitcoinGoldEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the location entry associated
                BitcoinGoldEntry.COLUMN_DATETEXT + " TEXT NOT NULL, " +
                BitcoinGoldEntry.COLUMN_BITCOIN_PRICE + " REAL NOT NULL, " +
                BitcoinGoldEntry.COLUMN_BITCOIN_PERCENT_CHANGE + " REAL NOT NULL, " +
                BitcoinGoldEntry.COLUMN_BITCOIN_DATETIME + " TEXT NOT NULL," +
                BitcoinGoldEntry.COLUMN_GOLD_PRICE + " REAL, " +
                BitcoinGoldEntry.COLUMN_GOLD_PERCENT_CHANGE + " REAL, " +
                BitcoinGoldEntry.COLUMN_GOLD_DATETIME + " TEXT," +

                BitcoinGoldEntry.COLUMN_PRICE_RATIO + " REAL, " +
                BitcoinGoldEntry.COLUMN_PRICE_RATIO_PERCENT_CHANGE + " REAL, " +
                BitcoinGoldEntry.COLUMN_200DAY_MOVING_AVERAGE + " REAL, " +
                BitcoinGoldEntry.COLUMN_50DAY_MOVING_AVERAGE + " REAL, " +
                BitcoinGoldEntry.COLUMN_10DAY_MOVING_AVERAGE + " REAL, " +

                // To assure the application have just one entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + BitcoinGoldEntry.COLUMN_DATETEXT + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_BITCOINGOLD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + BitcoinGoldEntry.TABLE_NAME);
//        onCreate(sqLiteDatabase);
    }

}
