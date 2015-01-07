package com.petegordon.bitcoingold.sync;

import android.content.Intent;
import android.os.IBinder;
import android.app.Service;

/**
 * Created by petegordon on 12/20/14.
 */
public class BitcoinGoldAuthenticatorService extends Service{

    // Instance field that stores the authenticator object
    private BitcoinGoldAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new BitcoinGoldAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
