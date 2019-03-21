package org.mauikit.accounts.syncadapter;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

public class MauiKitAuthenticator extends AbstractAccountAuthenticator {

  public MauiKitAuthenticator(Context context) {
    super(context);
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String s, String s2, String[] strings, Bundle bundle) throws NetworkErrorException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public String getAuthTokenLabel(String s) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
