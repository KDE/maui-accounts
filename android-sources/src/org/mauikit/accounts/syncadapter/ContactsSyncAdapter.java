package org.mauikit.accounts.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.mauikit.accounts.R;
import org.mauikit.accounts.utils.Constants;
import org.mauikit.accounts.utils.Utils;

public class ContactsSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String TAG = "ContactsSyncAdapter";

  // Global variables
  private ContentResolver mContentResolver;
  private Context mContext;

  /**
   * Set up the sync adapter
   */
  public ContactsSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);

    mContentResolver = context.getContentResolver();
    mContext = context;
  }

  /**
   * Set up the sync adapter. This form of the
   * constructor maintains compatibility with Android 3.0
   * and later platform versions
   */
  public ContactsSyncAdapter(
          Context context,
          boolean autoInitialize,
          boolean allowParallelSyncs) {
    super(context, autoInitialize, allowParallelSyncs);

    mContentResolver = context.getContentResolver();
    mContext = context;
  }

  @Override
  public void onPerformSync(
          Account account,
          Bundle extras,
          String authority,
          ContentProviderClient provider,
          SyncResult syncResult) {
    Log.d(TAG, "onPerformSync: Sync Started");

    this.performSync(
        AccountManager.get(mContext).getUserData(account, Constants.ACCOUNT_USERDATA_USERNAME),
        AccountManager.get(mContext).getPassword(account),
        AccountManager.get(mContext).getUserData(account, Constants.ACCOUNT_USERDATA_URL)
    );

    Log.d(TAG, "onPerformSync: Sync Complete");
  }

  private native void performSync(String username, String password, String url);
}
