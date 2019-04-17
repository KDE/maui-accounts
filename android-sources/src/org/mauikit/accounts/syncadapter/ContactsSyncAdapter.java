package org.mauikit.accounts.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.mauikit.accounts.R;
import org.mauikit.accounts.dav.CardDAV;
import org.mauikit.accounts.dav.dto.Contact;
import org.mauikit.accounts.utils.Constants;
import org.mauikit.accounts.utils.Utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContactsSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String TAG = "ContactsSyncAdapter";
  private static final String NOTIFICATION_CHANNEL_ID = "default";

  // Global variables
  private NotificationManager m_notificationManager;
  private Notification.Builder m_notificationBuilder;
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

    if (m_notificationManager == null) {
      m_notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Accounts Notification Channel", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("");

        NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        m_notificationBuilder = new Notification.Builder(mContext, NOTIFICATION_CHANNEL_ID);
      } else {
        m_notificationBuilder = new Notification.Builder(mContext);
      }

      m_notificationBuilder = new Notification.Builder(mContext);
      m_notificationBuilder.setSmallIcon(R.drawable.ic_launcher_background);
      m_notificationBuilder.setContentTitle("Accounts - Syncing Contacts");
      m_notificationBuilder.setOngoing(true);
    }

    m_notificationBuilder.setContentText("Syncing Contacts");
    m_notificationBuilder.setProgress(0, 0, true);
    m_notificationManager.notify(1, m_notificationBuilder.build());

    SyncManager manager = new SyncManager(AccountManager.get(mContext).getUserData(account, Constants.ACCOUNT_USERDATA_USERNAME), AccountManager.get(mContext).getPassword(account), AccountManager.get(mContext).getUserData(account, Constants.ACCOUNT_USERDATA_URL));
    manager.doSync();

    Log.d(TAG, "onPerformSync: Sync Complete");

    m_notificationBuilder.setContentText("Accounts - Sync Complete");
    m_notificationBuilder.setOngoing(false);
    m_notificationBuilder.setProgress(0, 0, false);
    m_notificationManager.notify(1, m_notificationBuilder.build());
  }

  private void updateNotificationProgress(int max, int progress) {
    Log.d(TAG, "Progress : " + max + " " + progress);
    m_notificationBuilder.setProgress(max, progress, false);
    m_notificationManager.notify(1, m_notificationBuilder.build());
  }

  private class SyncManager {
    private CardDAV cardDAV;
    private String url;

    public SyncManager(String username, String password, String url) {
      this.cardDAV = new CardDAV(url, username, password);
      this.url = url;
    }

    public void doSync() {
      int totalContacts = 0;
      int syncedContacts = 0;
      List<Contact> remoteContacts;
      String[][] localContacts;
      List<List<String>> ops = new ArrayList<>();

      try {
        remoteContacts = cardDAV.listAllContacts();

        Log.d(TAG, "doSync: Remote Contacts Length : " + remoteContacts.size());
      } catch (Exception e) {
        e.printStackTrace();
        Log.d(TAG, "onPerformSync: Error Stopping Sync");

        return;
      }

      localContacts = Utils.serializeContacts(getContext(), getContext().getResources().getString(R.string.account_type));

      totalContacts = localContacts.length;

      updateNotificationProgress(totalContacts, syncedContacts);

      for (String[] localContact : localContacts) {
        String vCard = localContact[0];
        String cTag = localContact[1];
        String url = localContact[2];
        String rawContactId = localContact[3];

        Log.d(TAG, "onPerformSync: Syncing Contact : " + cTag + " " + url + " " + rawContactId + " " + vCard);

        // TODO [X] : Upload New Local Contacts
        // TODO [X] : Upload Updated Local Contacts
        // TODO [X] : Download New Remote Contacts
        // TODO [X] : Download Updated Remote Contacts
        // TODO [ ] : Delete Local Contacts if Remote Deleted
        // TODO [X] : Delete Remote Contacts if Local Deleted

        if (url.isEmpty()) {
          // Upload New Contact
          Log.d(TAG, "onPerformSync: New Contact");

          List<String> op = createContact(rawContactId, vCard);

          if (op.size() == 5) {
            ops.add(op);
          }
        } else {
          Contact remoteContact = null;

          for (Contact c : remoteContacts) {
            if (getFilenameFromUrl(c.getHref().toString()).equals(getFilenameFromUrl(url))) {
              Log.d(TAG, "doSync: Found Remote Contact : " + getFilenameFromUrl(url));

              remoteContact = c;
              remoteContacts.remove(c);

              break;
            }
          }

          // KNOWN_BUG
          // FIXME :
          if (remoteContact != null) {
            String localContactGeneratedFilename = generateContactUuid(vCard) + ".vcf";
            String localContactSavedFilename = getFilenameFromUrl(url);

            Log.d(TAG, "doSync: Filenames : " + localContactGeneratedFilename + " " + localContactSavedFilename);

            if (localContactSavedFilename.equals(localContactGeneratedFilename)) {
              if (remoteContact.getEtag() != cTag) {
                Log.d(TAG, "doSync: Remote Contact Updated");

                List<String> op = buildOperation(Constants.SYNC_OPERATION_UPDATE, remoteContact.getVcard(), remoteContact.getEtag(), url, rawContactId);

                if (op.size() == 5) {
                  ops.add(op);
                }
              } else {
                Log.d(TAG, "doSync: Contact not Updated. Ignoring");
              }
            } else {
              Log.d(TAG, "doSync: Local Contact Updated");

              List<String> op = updateContact(rawContactId, cTag, vCard, url);

              if (op.size() == 5) {
                ops.add(op);
              }
            }
          } else {
            Log.wtf(TAG, "doSync: Control should never come here. Something is wrong.");
          }
        }

        syncedContacts++;
        updateNotificationProgress(totalContacts, syncedContacts);
      }

      String[][] deletedContacts = Utils.getDeletedContacts(getContext(), getContext().getResources().getString(R.string.account_type));

      totalContacts += deletedContacts.length;
      updateNotificationProgress(totalContacts, syncedContacts);

      Log.d(TAG, "doSync: deletedContacts length : " + deletedContacts.length);

      for (String[] deletedContact : deletedContacts) {
        String url = deletedContact[0];
        String rawContactId = deletedContact[1];

        Log.d(TAG, "doSync: Deleting Contact : " + url + " " + rawContactId);

        List<String> op = deleteContact(rawContactId, url);

        if (op.size() == 5) {
          ops.add(op);
        }

        for (Contact c : remoteContacts) {
          if (getFilenameFromUrl(c.getHref().toString()).equals(getFilenameFromUrl(url))) {
            // Contact Removed from Remote Server

            remoteContacts.remove(c);
          }
        }

        syncedContacts++;
        updateNotificationProgress(totalContacts, syncedContacts);
      }

      Log.d(TAG, "doSync: Remaining Remote Contacts Length : " + remoteContacts.size());

      totalContacts += remoteContacts.size();

      for (Contact c : remoteContacts) {
        List<String> op = buildOperation(Constants.SYNC_OPERATION_INSERT, c.getVcard(), c.getEtag(), c.getHref().toString(), "");

        if (op.size() == 5) {
          ops.add(op);
        }

        syncedContacts++;
        updateNotificationProgress(totalContacts, syncedContacts);
      }

      parseAndSendOps(ops);
    }

    private List<String> createContact(String rawContactId, String vCard) {
      String uuid = generateContactUuid(vCard);

      try {
        Contact contact = cardDAV.createContact(uuid, vCard, true);

        Log.d(TAG, "createContact: Contact Created");
        Log.d(TAG, "createContact: ETAG  : " + contact.getEtag());
        Log.d(TAG, "createContact: Href  : " + contact.getHref());
        Log.d(TAG, "createContact: vCard : " + contact.getVcard());

        return buildOperation(Constants.SYNC_OPERATION_INSERT_URL_CTAG, vCard, contact.getEtag(), this.url + "/" + uuid + ".vcf", rawContactId);
      } catch (Exception e) {
        e.printStackTrace();

        Log.d(TAG, "createContact: ERROR : Remote Contact not Created");
      }

      return null;
    }

    private List<String> updateContact(String rawContactId, String cTag, String vCard, String url) {
      try {
        Contact contact = cardDAV.updateContact(new URI(url), vCard, cTag);

        Log.d(TAG, "updateContact: Contact Updated");
        Log.d(TAG, "updateContact: ETAG  : " + contact.getEtag());
        Log.d(TAG, "updateContact: Href  : " + contact.getHref());
        Log.d(TAG, "updateContact: vCard : " + contact.getVcard());

        return buildOperation(Constants.SYNC_OPERATION_UPDATE_CTAG, contact.getVcard(), contact.getEtag(), url, rawContactId);
      } catch (Exception e) {
        e.printStackTrace();

        Log.d(TAG, "updateContact: ERROR : Remote Contact not Updated");
      }

      return null;
    }

    private List<String> deleteContact(String rawContactId, String url) {
      try {
        cardDAV.deleteContact(new URI(url));

        Log.d(TAG, "deleteContact: Contact Deleted");

        return buildOperation(Constants.SYNC_OPERATION_DELETE, "", "", url, rawContactId);
      } catch (Exception e) {
        e.printStackTrace();

        Log.d(TAG, "deleteContact: ERROR : Remote Contact not Deleted");
      }

      return null;
    }

    private void parseAndSendOps(List<List<String>> ops) {
      String[][] opsArray = new String[ops.size()][];

      for(int i=0; i < ops.size(); i++) {
        opsArray[i] = ops.get(i).toArray(new String[0]);
      }

//      Log.d(TAG, "parseAndSendOps: " + Arrays.deepToString(opsArray));

      Utils.syncContacts(opsArray, getContext(), getContext().getResources().getString(R.string.account_type));
    }

    private String getFilenameFromUrl(String url) {
      return url.substring(url.lastIndexOf("/") + 1);
    }

    private String generateContactUuid(String vCard) {
      return UUID.nameUUIDFromBytes(vCard.getBytes()).toString();
    }

    private List<String> buildOperation(String operation, String vCard, String cTag, String url, String rawContactId) {
      List<String> returnVal = new ArrayList<>();
      returnVal.add(0, operation);
      returnVal.add(1, vCard);
      returnVal.add(2, cTag);
      returnVal.add(3, url);
      returnVal.add(4, rawContactId);

      return returnVal;
    }
  }
}
