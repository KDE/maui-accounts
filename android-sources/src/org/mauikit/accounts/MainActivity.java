package org.mauikit.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.mauikit.accounts.utils.Constants;
import org.mauikit.accounts.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends org.qtproject.qt5.android.bindings.QtActivity {
  private final static String TAG = "MainActivity";
  private Account mAccount = null;
  private static MainActivity m_instance = null;
  private static ProgressDialog progressDialog = null;

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
    ContentResolver.addPeriodicSync(newAccount, ContactsContract.AUTHORITY, Bundle.EMPTY, 86400);

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

  public static String[][] getAccounts() {
    List<String[]> accountsSerialized = new ArrayList<>();
    AccountManager accountManager = AccountManager.get(m_instance);

    Account[] accounts = accountManager.getAccountsByType(m_instance.getResources().getString(R.string.account_type));

    for (Account account : accounts) {
      String count = accountManager.getUserData(account, Constants.ACCOUNT_USERDATA_CONTACTS_COUNT);
      accountsSerialized.add(new String[] {account.name, count != null ? count : "0"});
    }

    return accountsSerialized.toArray(new String[0][0]);
  }

  public static void removeAccount(String accountName) {
    AccountManager m_accountManager = AccountManager.get(m_instance);
    Account[] accounts = m_accountManager.getAccountsByType(m_instance.getResources().getString(R.string.account_type));

    for (Account account : accounts) {
      if (account.name.equals(accountName.replaceFirst(" \\(\\d*\\)", ""))) {
        ContentResolver.cancelSync(account, ContactsContract.AUTHORITY);
        m_accountManager.removeAccount(account, null, null);

        break;
      }
    }
  }

  public static void syncAccount(String accountName) {
    AccountManager m_accountManager = AccountManager.get(m_instance);
    Account[] accounts = m_accountManager.getAccountsByType(m_instance.getResources().getString(R.string.account_type));

    for (Account account : accounts) {
      if (account.name.equals(accountName.replaceFirst(" \\(\\d*\\)", ""))) {
        ContentResolver.requestSync(account, ContactsContract.AUTHORITY, Bundle.EMPTY);

        break;
      }
    }
  }

  public static void showUrl(String accountName) {
    AccountManager m_accountManager = AccountManager.get(m_instance);
    Account[] accounts = m_accountManager.getAccountsByType(m_instance.getResources().getString(R.string.account_type));

    for (Account account : accounts) {
      if (account.name.equals(accountName.replaceFirst(" \\(\\d*\\)", ""))) {
        showInfoDialog("Account URL", AccountManager.get(m_instance).getUserData(account, Constants.ACCOUNT_USERDATA_URL), true);

        break;
      }
    }
  }

  public static void showToast(String text) {
    final String toastText = text;
    m_instance.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(m_instance, toastText, Toast.LENGTH_LONG).show();
      }
    });
  }

  public static void showIndefiniteProgressDialog(String message, boolean isCancelable) {
    final String dialogMessage = message;
    final boolean dialogIsCancelable = isCancelable;

    m_instance.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (progressDialog == null) {
          progressDialog = new ProgressDialog(m_instance);
          progressDialog.setIndeterminate(true);
        }

        progressDialog.setCancelable(dialogIsCancelable);

        progressDialog.setMessage(dialogMessage);
        progressDialog.show();
      }
    });
  }

  public static void hideIndefiniteProgressDialog() {
    progressDialog.dismiss();
  }

  public static void showInfoDialog(String title, String text, final boolean enableCopyText) {
    final String m_title = title;
    final String m_text = text;

    m_instance.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(m_instance);

        dialogBuilder.setTitle(m_title);
        dialogBuilder.setMessage(m_text);
        dialogBuilder.setNegativeButton("Close", null);

        if (enableCopyText) {
          dialogBuilder.setPositiveButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ClipboardManager clipboard = (ClipboardManager) m_instance.getSystemService(Context.CLIPBOARD_SERVICE);
              ClipData clip = ClipData.newPlainText("Account URL", m_text);
              clipboard.setPrimaryClip(clip);
            }
          });
        }

        dialogBuilder.create().show();
      }
    });
  }

//  public static String[][] getContacts() {
//    return Utils.serializeContacts(m_instance.getApplicationContext(), m_instance.getResources().getString(R.string.account_type));
//  }
//
//  public static String[][] getDeletedContacts() {
//    return Utils.getDeletedContacts(m_instance.getApplicationContext(), m_instance.getResources().getString(R.string.account_type));
//  }
//
//  public static void syncContacts(String ops[][]) {
//    Utils.syncContacts(ops, m_instance.getApplicationContext(), m_instance.getResources().getString(R.string.account_type));
//  }
}
