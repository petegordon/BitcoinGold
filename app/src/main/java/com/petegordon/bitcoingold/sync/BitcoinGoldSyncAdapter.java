package com.petegordon.bitcoingold.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.petegordon.bitcoingold.MainActivity;
import com.petegordon.bitcoingold.R;
import com.petegordon.bitcoingold.Utility;
import com.petegordon.bitcoingold.data.BitcoinGoldContract;
import com.petegordon.bitcoingold.data.BitcoinGoldDbHelper;
import com.petegordon.bitcoingold.data.BitcoinGoldProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by petegordon on 12/20/14.
 */
public class BitcoinGoldSyncAdapter extends AbstractThreadedSyncAdapter {

    //&auth_token=gJ4wHyGqYVtKhvzvJcP9
    public final static String QUERY_PARAM_AUTH_TOKEN = "auth_token";
    public final static String AUTH_TOKEN = "[YOUR AUTH TOKEN]";

    public static final String LOG_TAG = BitcoinGoldSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the bitcoin and gold prices, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

    private static final int BITCOINGOLD_NOTIFICATION_ID = 3004;

    public BitcoinGoldSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {


        String bitcoinJsonString = getBitcoinJSON();
        String goldJsonString = getGoldJSON();

        // Now we have a String representing the bitcoin prices the past 10 days in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        //get weigted price at position 7 of the data
        final String DATA = "data";
        final String UPDATED_AT = "updated_at";
        final int WEIGHTED_24H_AVERAGE = 7;
        final int GOLD_AM_PRICE = 1;
        final int GOLD_PM_PRICE = 2;
        final int DATETEXT = 0;

        try {
            JSONObject goldJson = new JSONObject(goldJsonString);
            JSONObject bitcoinJson = new JSONObject(bitcoinJsonString);
            String bitcoinUpdatedAt = bitcoinJson.getString(UPDATED_AT);
            String goldUpdatedAt = goldJson.getString(UPDATED_AT);
            JSONArray bitcoinDayPriceData = bitcoinJson.getJSONArray(DATA);
            JSONArray goldDayPriceData = goldJson.getJSONArray(DATA);


            Vector<ContentValues> cVVector = new Vector<ContentValues>(bitcoinDayPriceData.length());

            int dayOffset = 0;

            for(int i=0; i<BitcoinGoldContract.NUMBER_OF_DAYS; i++){

                double bitcoinTodayPrice = bitcoinDayPriceData.getJSONArray(i).getDouble(WEIGHTED_24H_AVERAGE);
                double bitcoinYesterdayPrice = bitcoinDayPriceData.getJSONArray(i+1).getDouble(WEIGHTED_24H_AVERAGE);
                double bitcoinPercentChange = (bitcoinTodayPrice-bitcoinYesterdayPrice)/bitcoinYesterdayPrice;

                /**
                 * The Gold Day data is in order of most recent to the oldest.
                 * Current Day is at position 0
                 * Weekends do not have data so they must be skipped with the offset to find the
                 * date equal to the bitcoin date.
                 *
                 */
                double goldTodayAMPrice;
                double goldTodayPMPrice;
                Double goldTodayAveragePrice = null;
                Double goldPercentChange = null;

                String returnedBitcoinDateString = bitcoinDayPriceData.getJSONArray(i).getString(DATETEXT);
                String returnedGoldDateString = goldDayPriceData.getJSONArray(i-dayOffset).getString(DATETEXT);
                if(returnedGoldDateString.equals(returnedBitcoinDateString)) {

                    int indexGoldToday = i-dayOffset;
                    int indexGoldYesterday = indexGoldToday + 1;

                    goldTodayAMPrice = goldDayPriceData.getJSONArray(indexGoldToday).getDouble(GOLD_AM_PRICE);
                    if(goldDayPriceData.getJSONArray(indexGoldToday).isNull(GOLD_PM_PRICE)) {
                        goldTodayPMPrice = goldTodayAMPrice;
                    }else{
                        goldTodayPMPrice = goldDayPriceData.getJSONArray(indexGoldToday).getDouble(GOLD_PM_PRICE);
                    }


                    goldTodayAveragePrice = ((goldTodayAMPrice + goldTodayPMPrice) / 2);

                    if(indexGoldYesterday < goldDayPriceData.length()) {
                        double goldYesterdayAMPrice = goldDayPriceData.getJSONArray(indexGoldYesterday).getDouble(GOLD_AM_PRICE);
                        double goldYesterdayPMPrice;
                        if(goldDayPriceData.getJSONArray(indexGoldYesterday).isNull(GOLD_PM_PRICE)){
                            goldYesterdayPMPrice = goldYesterdayAMPrice;
                        } else {
                            goldYesterdayPMPrice = goldDayPriceData.getJSONArray(indexGoldYesterday).getDouble(GOLD_PM_PRICE);
                        }

                        double goldYesterdayAveragePrice = ((goldYesterdayAMPrice + goldYesterdayPMPrice) / 2);
                        goldPercentChange = (goldTodayAveragePrice - goldYesterdayAveragePrice) / goldYesterdayAveragePrice;
                    }
                }else {
                    int indexGoldTomorrow = i - dayOffset - 1;
                    if(indexGoldTomorrow < 0){
                        //The first date doesn't have a Gold value; just use the first Gold Value
                        indexGoldTomorrow = 0;
                    }
                    goldTodayAMPrice = goldDayPriceData.getJSONArray(indexGoldTomorrow).getDouble(GOLD_AM_PRICE);
                    if(goldDayPriceData.getJSONArray(indexGoldTomorrow).isNull(GOLD_PM_PRICE)) {
                        goldTodayPMPrice = goldTodayAMPrice;
                    }else{
                        goldTodayPMPrice = goldDayPriceData.getJSONArray(indexGoldTomorrow).getDouble(GOLD_PM_PRICE);
                    }
                    goldTodayAveragePrice = ((goldTodayAMPrice + goldTodayPMPrice) / 2);
                    dayOffset++;
                }

                double priceRatio = goldTodayAveragePrice/bitcoinTodayPrice;
                priceRatio = Math.round(priceRatio * 100.0) / 100.0;

                ContentValues bitcoinGoldDayPrice = new ContentValues();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                try {
                    Date returnedDate = dateFormat.parse(returnedBitcoinDateString);
                    String recordedDateString = new SimpleDateFormat(BitcoinGoldContract.DATE_FORMAT).format(returnedDate);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT, recordedDateString);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_BITCOIN_PRICE, bitcoinDayPriceData.getJSONArray(i).getDouble(WEIGHTED_24H_AVERAGE));
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_BITCOIN_PERCENT_CHANGE, bitcoinPercentChange);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_BITCOIN_DATETIME, bitcoinUpdatedAt);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_GOLD_PRICE, goldTodayAveragePrice);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_GOLD_PERCENT_CHANGE, goldPercentChange);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_GOLD_DATETIME, goldUpdatedAt);
                    bitcoinGoldDayPrice.put(BitcoinGoldContract.BitcoinGoldEntry.COLUMN_PRICE_RATIO, priceRatio);

                    cVVector.add(bitcoinGoldDayPrice);
                }catch(ParseException ex){
                    ex.printStackTrace();
                }




            }


            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(BitcoinGoldContract.BitcoinGoldEntry.CONTENT_URI, cvArray);


                notifyBitcoinGold();

            }
            Log.d(LOG_TAG, "FetchBitcoin Complete. " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // This will only happen if there was an error getting or parsing.
        return;
    }


    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

    /*
     * Add the account and account type, no password or user data
     * If successful, return the Account object, otherwise report an error.
     */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
        /*
         * If you don't set android:syncable="true" in
         * in your <provider> element in the manifest,
         * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
         * here.
         */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }


    private static void onAccountCreated(Account newAccount, Context context) {
    /*
     * Since we've created an account
     */
        BitcoinGoldSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

    /*
     * Without calling setSyncAutomatically, our periodic sync will not be enabled.
     */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

    /*
     * Finally, let's do a sync to get things started
     */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    private static String getGoldJSON(){
        Log.d(LOG_TAG, "Starting GOLD sync");
        System.out.println("Starting the GOLD sync");
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String goldJsonString = null;

        String format = "json";

        try {
            // Construct the URL for the quandl.com Bitcoin query
            final String BASE_URL =
                    "https://www.quandl.com/api/v1/datasets/LBMA/GOLD.json?";
            final String QUERY_PARAM_STARTDATE = "trim_start";  //date format is YYYY-MM-dd

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String strStartDate = dateFormat.format(new Date(new Date().getTime() - (BitcoinGoldContract.NUMBER_OF_DAYS+1) * 86400000 ) );

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM_STARTDATE, strStartDate)
                    .appendQueryParameter(QUERY_PARAM_AUTH_TOKEN, AUTH_TOKEN)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, url.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return "";
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return "";
            }
            goldJsonString = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the data, there's no point in attempting
            // to parse it.
            return "";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        System.out.println("End GOLD JSON retrieval");
        return goldJsonString;
    }

    private static String getBitcoinJSON(){
        Log.d(LOG_TAG, "Starting sync");
        System.out.println("Starting the sync");
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String bitcoinJsonString = null;

        String format = "json";

        try {
            // Construct the URL for the quandl.com Bitcoin query
            final String BASE_URL =
                    "http://www.quandl.com/api/v1/datasets/BITCOIN/BITSTAMPUSD?";
            final String QUERY_PARAM_STARTDATE = "trim_start";  //date format is YYYY-MM-dd


            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String strStartDate = dateFormat.format(new Date(new Date().getTime() - (BitcoinGoldContract.NUMBER_OF_DAYS+1) * 86400000 ));

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM_STARTDATE, strStartDate)
                    .appendQueryParameter(QUERY_PARAM_AUTH_TOKEN, AUTH_TOKEN)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, url.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return "";
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return "";
            }
            bitcoinJsonString = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the data, there's no point in attempting
            // to parse it.
            return "";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        System.out.println("End Bitcoin JSON retrieval");

        return bitcoinJsonString;
    }

    private void notifyBitcoinGold() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {

            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the bitcoin and gold price if we have a current record.

                //check the past 3 days.
                String startDateString = BitcoinGoldContract.getDbDateString(new Date(new Date().getTime() - (3) * 86400000 ));

                Cursor todayCursor = getContext().getContentResolver().query(
                        BitcoinGoldContract.BitcoinGoldEntry.buildBitcoinGoldWithStartDate(startDateString),
                        BitcoinGoldContract.BitcoinGoldEntry.BITCOINGOLD_COLUMNS,
                        null,
                        null,
                        BitcoinGoldContract.BitcoinGoldEntry.COLUMN_DATETEXT + " DESC"
                );

                if(todayCursor.moveToFirst()){
                    String priceRatio = todayCursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_PRICE_RATIO);
                    String priceBitcoin = Utility.getFormattedCurrencyValue(todayCursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_BITCOIN_PRICE));
                    String priceGold = Utility.getFormattedCurrencyValue(todayCursor.getDouble(BitcoinGoldContract.BitcoinGoldEntry.COL_GOLD_PRICE));
                    String priceDate = Utility.getFormattedMonthDay(getContext(), todayCursor.getString(BitcoinGoldContract.BitcoinGoldEntry.COL_DATE));


                    String title = context.getString(R.string.app_name);

                    // Define the text of the notification.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            priceRatio,
                            priceBitcoin,
                            priceGold);

                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setSmallIcon(R.drawable.bitcoingold_logo)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // BITCOINGOLD_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(BITCOINGOLD_NOTIFICATION_ID, mBuilder.build());


                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
            }
        }

    }


}

