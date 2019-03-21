package org.mauikit.accounts.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MauikitAuthenticatorService extends Service {
  @Override
  public IBinder onBind(Intent intent) {
    MauiKitAuthenticator authenticator = new MauiKitAuthenticator(this);
    return authenticator.getIBinder();
  }
}
