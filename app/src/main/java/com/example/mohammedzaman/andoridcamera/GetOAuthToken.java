/*
 * GetOAuthToken.java
 *
 * @author Mohammed Zaman
 *
 * A class to retrieve access token for Google Cloud Vision
 *
 * this class requires a environment variable to set called GOOGLE_APPLICATION_CREDENTIALS.
 * This variable should consist of the file path of a JSON file (service account key file)
 * downloaded from Google cloud console. The SHA-1 signing-certificate fingerprint on the
 * Google cloud console needs to set up for access to the API.The package which uses the
 * API needs to be stated in the Google cloud console or the access will be denied.
 *
 */


package com.example.mohammedzaman.andoridcamera;

import android.accounts.Account;
import android.app.Activity;
import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

public class GetOAuthToken extends AsyncTask<Void, Void, Void> {
    Activity mActivity;
    Account mAccount;
    int mRequestCode;
    String mScope;

    GetOAuthToken(Activity activity, Account account, String scope, int requestCode) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mAccount = account;
        this.mRequestCode = requestCode;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            String token = fetchToken();
            if (token != null) {
                ((MainActivity) mActivity).onTokenReceived(token);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String fetchToken() throws IOException {
        String accessToken;
        try {
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope);
            GoogleAuthUtil.clearToken(mActivity, accessToken);
            accessToken = GoogleAuthUtil.getToken(mActivity, mAccount, mScope);
            return accessToken;
        } catch (UserRecoverableAuthException userRecoverableException) {
            mActivity.startActivityForResult(userRecoverableException.getIntent(), mRequestCode);
        } catch (GoogleAuthException fatalException) {
            fatalException.printStackTrace();
        }
        return null;
    }


}