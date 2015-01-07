package com.petegordon.bitcoingold.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by petegordon on 12/20/14.
 */
public class BitcoinGoldSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static BitcoinGoldSyncAdapter sBitcoinGoldSyncAdapter = null;

    @Override
    public void onCreate() {
        System.out.println("BitcoinGoldSyncService onCreate");
        Log.d("BitcoinGoldSyncService", "onCreate - BitcoinGoldSyncService");
        synchronized (sSyncAdapterLock) {
            if (sBitcoinGoldSyncAdapter == null) {
                sBitcoinGoldSyncAdapter = new BitcoinGoldSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sBitcoinGoldSyncAdapter.getSyncAdapterBinder();
    }
}