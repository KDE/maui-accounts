package org.mauikit.accounts.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ContactsSyncService extends Service {
  // Object to use as a thread-safe lock
  private static final Object sSyncAdapterLock = new Object();
  // Storage for an instance of the sync adapter
  private static ContactsSyncAdapter sSyncAdapter = null;

  /*
   * Instantiate the sync adapter object.
   */
  @Override
  public void onCreate() {
    /*
     * Create the sync adapter as a singleton.
     * Set the sync adapter as syncable
     * Disallow parallel syncs
     */
    synchronized (sSyncAdapterLock) {
      if (sSyncAdapter == null) {
        sSyncAdapter = new ContactsSyncAdapter(getApplicationContext(), true);
      }
    }
  }

  /**
   * Return an object that allows the system to invoke
   * the sync adapter.
   */
  @Override
  public IBinder onBind(Intent intent) {
    /*
     * Get the object that allows external processes
     * to call onPerformSync(). The object is created
     * in the base class code when the SyncAdapter
     * constructors call super()
     */
    return sSyncAdapter.getSyncAdapterBinder();
  }
}
