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

import java.util.ArrayList;
import java.util.List;

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
        m_instance.requestPermissions(new String[]{android.Manifest.permission.GET_ACCOUNTS, android.Manifest.permission.READ_CONTACTS},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

//    ContentValues values = new ContentValues();
//    values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, m_instance.getResources().getString(R.string.account_type));
//    values.put(ContactsContract.RawContacts.ACCOUNT_NAME, m_instance.getResources().getString(R.string.account_type));
//    Uri rawContactUri = m_instance.getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
//    long rawContactId = ContentUris.parseId(rawContactUri);

//    values.clear();
//    values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
//    values.put(ContactsContract.Data.MIMETYPE, m_instance.getResources().getString(R.string.contact_item_ctag_mimetype));
//    values.put(ContactsContract.Contacts.Data.DATA1, "1234abcd");
//    m_instance.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);


//    ContentResolver resolver = m_instance.getContentResolver();
//    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
//    ops.add(ContentProviderOperation.newInsert(m_instance.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, false))
//            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, m_instance.getResources().getString(R.string.account_type))
//            .withValue(ContactsContract.Data.MIMETYPE, m_instance.getResources().getString(R.string.contact_item_ctag_mimetype))
//            .withValue(ContactsContract.Contacts.Data.DATA1, "1234abcd")
//            .withValue("raw_contact_id", 9681)
//            .build());

//    try {
//      resolver.applyBatch(ContactsContract.AUTHORITY, ops);
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//    }
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

    ContentResolver.setSyncAutomatically(newAccount, AUTHORITY, true);

    // Set this account periodically sync with the specified interval
    ContentResolver.addPeriodicSync(newAccount, AUTHORITY, Bundle.EMPTY, 21600);

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

  private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
    if (isSyncOperation) {
      return uri.buildUpon()
              .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
              .build();
    }
    return uri;
  }
}
