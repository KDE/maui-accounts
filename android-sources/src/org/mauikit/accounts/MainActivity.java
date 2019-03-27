package org.mauikit.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentProviderOperation;
import android.content.SharedPreferences;
import android.content.ContentUris;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.os.Environment;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import org.mauikit.accounts.utils.Constants;
import org.mauikit.accounts.utils.Utils;

public class MainActivity extends org.qtproject.qt5.android.bindings.QtActivity {
  private final static String TAG = "MainActivity";
  private static MainActivity m_instance = null;

  public MainActivity() {
    m_instance = this;
  }

  public static void init() {
    Log.d(TAG, "in init()");
    int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;


    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        m_instance.requestPermissions(new String[]{android.Manifest.permission.GET_ACCOUNTS, android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }
  }

  /**
   * Create a new account for the sync adapter
   */
  public static void createSyncAccount(String accountName, String username, String password, String url) {
    String AUTHORITY = m_instance.getResources().getString(R.string.authorities);
    String ACCOUNT_TYPE = m_instance.getResources().getString(R.string.account_type);

    // Create the account type and default account
    Account newAccount = new Account(
            accountName, ACCOUNT_TYPE);
    // Get an instance of the Android account manager
    AccountManager accountManager =
            (AccountManager) m_instance.getSystemService(
                    ACCOUNT_SERVICE);

    ContentResolver.setSyncAutomatically(newAccount, ContactsContract.AUTHORITY, true);

    // Set this account periodically sync with the specified interval
    ContentResolver.addPeriodicSync(newAccount, ContactsContract.AUTHORITY, Bundle.EMPTY, 21600);

    Bundle bundle = new Bundle();
    bundle.putString(Constants.ACCOUNT_USERDATA_ACCOUNTNAME, accountName);
    bundle.putString(Constants.ACCOUNT_USERDATA_USERNAME, username);
    bundle.putString(Constants.ACCOUNT_USERDATA_URL, url);

    /*
     * Add the account and account type, no password or user data
     * If successful, return the Account object, otherwise report an error.
     */
    if (accountManager.addAccountExplicitly(newAccount, password, bundle)) {
        Log.d(TAG, "Account Added");
//      return newAccount;
    } else {
      /*
       * The account exists or some other error occurred. Log this, report it,
       * or handle it internally.
       */
       Log.d(TAG, "Account Exists");
//      return newAccount;
    }
  }

  public static String[] getAccounts() {
    List<String> accountsSerialized = new ArrayList<>();
    Account[] accounts = AccountManager.get(m_instance).getAccountsByType(m_instance.getResources().getString(R.string.account_type));

    for (Account account : accounts) {
      accountsSerialized.add(account.name);
    }

    return accountsSerialized.toArray(new String[0]);
  }

  public static void removeAccount(String accountName) {
    AccountManager m_accountManager = AccountManager.get(m_instance);
    Account[] accounts = m_accountManager.getAccountsByType(m_instance.getResources().getString(R.string.account_type));

    for (Account account : accounts) {
      if (account.name.equals(accountName)) {
        m_accountManager.removeAccount(account, null, null);

        break;
      }
    }
  }

  public static String[][] getContacts() {
    return Utils.serializeContacts(m_instance.getApplicationContext(), m_instance.getResources().getString(R.string.account_type));
  }

  public static String[][] getDeletedContacts() {
    return Utils.getDeletedContacts(m_instance.getApplicationContext(), m_instance.getResources().getString(R.string.account_type));
  }

  public static void syncContacts(String ops[][]) {
    Utils.syncContacts(ops, m_instance.getApplicationContext(), m_instance.getResources().getString(R.string.account_type));
  }
}
