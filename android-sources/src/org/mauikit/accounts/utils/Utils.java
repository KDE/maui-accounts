package org.mauikit.accounts.utils;

import org.mauikit.accounts.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.database.DatabaseUtils;
import android.content.ContentUris;
import android.util.Base64;
import android.content.ContentProviderOperation;
import android.content.ContentValues;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Exception;
import java.nio.charset.StandardCharsets;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Nickname;
import ezvcard.property.Organization;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Url;

public class Utils {
  private static final String TAG = "Utils";

  public static SharedPreferences getDefaultSharedPreferences(
          Context context) {
    return context.getSharedPreferences(
            context.getPackageName() + "_preferences",
            Context.MODE_PRIVATE);
  }

  public static String[][] serializeContacts(Context ctx, String accountType) {
    List<String[]> serializedData = new ArrayList<>();

    String RAW_CONTACT_SELECTION = ContactsContract.RawContacts.ACCOUNT_TYPE + " = '" + accountType + "' AND " +
            ContactsContract.RawContacts.DELETED + " = 0 ";
    String DATA_SELECTION = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
    Cursor contacts = ctx.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, RAW_CONTACT_SELECTION, null, null);
    Cursor commonDataCursor;

    contacts.moveToFirst();

    while (!contacts.isAfterLast()) {
      VCard vCard = new VCard();
      String rawContactId = contacts.getString(contacts.getColumnIndex(ContactsContract.RawContacts._ID));
      String cTag;
      String url;
      String DATA_SELECTION_ARGS[] = {rawContactId, ""};
      String PROJECTION[];

      // DATA: NAME
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.StructuredName.PREFIX, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, ContactsContract.CommonDataKinds.StructuredName.SUFFIX};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      if (commonDataCursor.getCount() > 0) {
        StructuredName name = new StructuredName();
        String givenName = commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
        String familyName = commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
        String fullName = "";

        name.setGiven(givenName);
        name.setFamily(familyName);
//        name.getPrefixes().add(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)));
//        name.getSuffixes().add(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)));

        if (givenName != null) {
          fullName = givenName;
        }
        if (familyName != null) {
          fullName = fullName + " " + familyName;
        }

        vCard.setStructuredName(name);
        vCard.setFormattedName(fullName);
      }

      // DATA: PHONE
      // TODO: Include Type in vCard
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      while (!commonDataCursor.isAfterLast()) {
        Telephone tel = new Telephone(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
        //        ContactsContract.CommonDataKinds.Phone.getTypeLabel(ctx.getResources(), commonDataCursor.getInt(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)), "").toString()
        //        tel.getTypes().add();

        vCard.addTelephoneNumber(tel);
        commonDataCursor.moveToNext();
      }

      // DATA: EMAIL
      // TODO: Include Type in vCard
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      while (!commonDataCursor.isAfterLast()) {
        Email email = new Email(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)));
        vCard.addEmail(email);
        commonDataCursor.moveToNext();
      }

      // DATA: PHOTO
      // TODO

      // DATA: ORGANIZATION
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.Organization.COMPANY, ContactsContract.CommonDataKinds.Organization.DEPARTMENT, ContactsContract.CommonDataKinds.Organization.TITLE};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      if (commonDataCursor.getCount() > 0) {
        vCard.setOrganization(
                commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)),
                commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE))
        );
      }

      // DATA: IM
      // TODO

      // DATA: NICKNAME
      // TODO: Include Type in vCard
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.Nickname.NAME};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      if (commonDataCursor.getCount() > 0) {
        vCard.setNickname(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)));
      }

      // DATA: NOTE
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.Note.NOTE};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      if (commonDataCursor.getCount() > 0) {
        vCard.addNote(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)));
      }

      // DATA: GROUP_MEMBERSHIP
      // TODO

      // DATA: POSTAL
      // TODO: Include Type in vCard
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      while (!commonDataCursor.isAfterLast()) {
        Address addr = new Address();
        addr.setLabel(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)));

        vCard.addAddress(addr);
        commonDataCursor.moveToNext();
      }

      // DATA: WEBSITE
      // TODO: Include Type in vCard
      DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE;
      PROJECTION = new String[]{ContactsContract.CommonDataKinds.Website.URL};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      while (!commonDataCursor.isAfterLast()) {
        vCard.addUrl(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)));
        commonDataCursor.moveToNext();
      }

      // DATA: EVENT
      // TODO

      // DATA: RELATIONSHIP
      // TODO

      // DATA: SIP_ADDRESS
      // TODO

      // Data: CTAG
      DATA_SELECTION_ARGS[1] = ctx.getResources().getString(R.string.contact_item_ctag_mimetype);
      PROJECTION = new String[]{ContactsContract.Data.DATA1};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      try {
        cTag = commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.Data.DATA1));
      } catch (Exception e) {
        cTag = "";
      }

      // DATA: URL
      DATA_SELECTION_ARGS[1] = ctx.getResources().getString(R.string.contact_item_url_mimetype);
      PROJECTION = new String[]{ContactsContract.Data.DATA1};
      commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
      commonDataCursor.moveToFirst();

      try {
        url = commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.Data.DATA1));
      } catch (Exception e) {
        url = "";
      }

      // GENERATE vCard
      String vCardStr = Ezvcard.write(vCard).version(VCardVersion.V3_0).go();
      serializedData.add(new String[]{vCardStr, cTag == null ? "" : cTag, url == null ? "" : url, rawContactId});

      contacts.moveToNext();
    }

    return serializedData.toArray(new String[0][0]);
  }

  public static void syncContacts(String ops[][], Context ctx, String accountType) {
    Log.d(TAG, "syncContacts: Applying local changes");

    for (int i = 0; i < ops.length; i++) {
      for (int j = 0; j < ops[i].length; j++) {
        byte[] data = Base64.decode(ops[i][j], Base64.DEFAULT);
        String text = new String(data, StandardCharsets.UTF_8);

        ops[i][j] = text;
      }

      String operation = ops[i][0];
      String vCard = ops[i][1];
      String cTag = ops[i][2];
      String url = ops[i][3];
      String rawContactId = ops[i][4];

      String DATA_SELECTION = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
      String DATA_SELECTION_ARGS[] = {rawContactId, ""};

      switch (operation) {
        case Constants.SYNC_OPERATION_INSERT: {
          Log.d(TAG, "syncContacts: SYNC_OPERATION_INSERT");

          insertContact(ctx, accountType, vCard, url, cTag);

          break;
        }

        case Constants.SYNC_OPERATION_UPDATE: {
          Log.d(TAG, "syncContacts: SYNC_OPERATION_UPDATE");

          String DELETE_DATA_SELECTION = ContactsContract.RawContacts._ID + " = ? ";
          String DELETE_DATA_SELECTION_ARGS[] = {rawContactId};

          ctx.getContentResolver().delete(Utils.addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true), DELETE_DATA_SELECTION, DELETE_DATA_SELECTION_ARGS);

          insertContact(ctx, accountType, vCard, url, cTag);

          break;
        }

        case Constants.SYNC_OPERATION_INSERT_URL_CTAG: {
          Log.d(TAG, "syncContacts: SYNC_OPERATION_INSERT_URL_CTAG : " + cTag + ", " + url);

          //            DATA_SELECTION_ARGS[1] = ctx.getResources().getString(R.string.contact_item_ctag_mimetype);
          ContentValues values = new ContentValues();
          values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
          values.put(ContactsContract.Data.MIMETYPE, ctx.getResources().getString(R.string.contact_item_ctag_mimetype));
          values.put(ContactsContract.Data.DATA1, cTag);
          ctx.getContentResolver().insert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true), values);//, DATA_SELECTION, DATA_SELECTION_ARGS);

          //            DATA_SELECTION_ARGS[1] = ctx.getResources().getString(R.string.contact_item_url_mimetype);
          values.clear();
          values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
          values.put(ContactsContract.Data.MIMETYPE, ctx.getResources().getString(R.string.contact_item_url_mimetype));
          values.put(ContactsContract.Data.DATA1, url);
          ctx.getContentResolver().insert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true), values);//, DATA_SELECTION, DATA_SELECTION_ARGS);

          String _DATA_SELECTION = ContactsContract.RawContacts._ID + " = ? ";
          String _DATA_SELECTION_ARGS[] = {rawContactId};
          values.clear();
          values.put(ContactsContract.RawContacts.SYNC1, url);
          ctx.getContentResolver().update(Utils.addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true), values, _DATA_SELECTION, _DATA_SELECTION_ARGS);

          break;
        }

        case Constants.SYNC_OPERATION_UPDATE_CTAG: {
          Log.d(TAG, "syncContacts: SYNC_OPERATION_UPDATE_CTAG : " + cTag);

          DATA_SELECTION_ARGS[1] = ctx.getResources().getString(R.string.contact_item_ctag_mimetype);
          ContentValues values = new ContentValues();
          values.put(ContactsContract.Data.DATA1, cTag);
          ctx.getContentResolver().update(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true), values, DATA_SELECTION, DATA_SELECTION_ARGS);

          break;
        }

        case Constants.SYNC_OPERATION_DELETE: {
          Log.d(TAG, "syncContacts: SYNC_OPERATION_DELETE : " + rawContactId);

          String DELETE_DATA_SELECTION = ContactsContract.RawContacts._ID + " = ? ";
          String DELETE_DATA_SELECTION_ARGS[] = {rawContactId};

          ctx.getContentResolver().delete(Utils.addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true), DELETE_DATA_SELECTION, DELETE_DATA_SELECTION_ARGS);
          break;
        }
      }
    }
  }

  // Returns String[][2]
  public static String[][] getDeletedContacts(Context ctx, String accountType) {
    List<String[]> serializedData = new ArrayList<>();
    String RAW_CONTACT_SELECTION = ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " +
            ContactsContract.RawContacts.DELETED + " <> 0 ";
    String RAW_CONTACT_SELECTION_ARGS[] = {accountType};
    String PROJECTION[] = {ContactsContract.RawContacts._ID, ContactsContract.RawContacts.SYNC1};
    Cursor contacts = ctx.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, PROJECTION, RAW_CONTACT_SELECTION, RAW_CONTACT_SELECTION_ARGS, null);
    contacts.moveToFirst();

    while (!contacts.isAfterLast()) {
      String rawContactId = contacts.getString(contacts.getColumnIndex(ContactsContract.RawContacts._ID));
      String url;

      try {
        url = contacts.getString(contacts.getColumnIndex(ContactsContract.RawContacts.SYNC1));
      } catch (Exception e) {
        e.printStackTrace();
        url = "";
      }

      serializedData.add(new String[]{url, rawContactId});

      contacts.moveToNext();
    }

    return serializedData.toArray(new String[0][0]);
  }

  public static void log(String TAG, String message) {
    // Split by line, then ensure each line can fit into Log's maximum length.
    for (int i = 0, length = message.length(); i < length; i++) {
      int newline = message.indexOf('\n', i);
      newline = newline != -1 ? newline : length;
      do {
        int end = Math.min(newline, i + 1000);
        Log.d(TAG, message.substring(i, end));
        i = end;
      } while (i < newline);
    }
  }

  public static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
    if (isSyncOperation) {
      return uri.buildUpon()
              .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
              .build();
    }
    return uri;
  }

  private static void insertContact(Context ctx, String accountType, String vCard, String url, String cTag) {
      VCard card = Ezvcard.parse(vCard).first();
      ArrayList<ContentProviderOperation> insertOps =
              new ArrayList<ContentProviderOperation>();

      int rawContactInsertIndex = insertOps.size();
      insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true))
              .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
              .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountType)
              .withValue(ContactsContract.RawContacts.SYNC1, url)
              .build());

      // DATA: NAME
      ContentProviderOperation.Builder nameBuilder = ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
              .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
              .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
              .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, card.getStructuredName().getGiven())
              .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, card.getStructuredName().getFamily());

    //          if (!card.getStructuredName().getPrefixes().isEmpty()) {
    //            nameBuilder.withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, card.getStructuredName().getPrefixes().get(0));
    //          }
    //          if (!card.getStructuredName().getSuffixes().isEmpty()) {
    //            nameBuilder.withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, card.getStructuredName().getSuffixes().get(0));
    //          }

      insertOps.add(nameBuilder.build());

      // DATA: PHONE
      for (Telephone number : card.getTelephoneNumbers()) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number.getText())
                .build());
      }

      // DATA: EMAIL
      for (Email email : card.getEmails()) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getValue())
                .build());
      }

      // DATA: PHOTO
      // TODO

      // DATA: ORGANIZATION
      if (card.getOrganization() != null) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, card.getOrganization().getValues().get(0))
                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, card.getOrganization().getValues().get(1))
                .build());
      }

      // DATA: IM
      // TODO

      // DATA: NICKNAME
      if (card.getNickname() != null) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, card.getNickname().getValues().get(0))
                .build());
      }

      // DATA: NOTE
      if (!card.getNotes().isEmpty()) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, card.getNotes().get(0))
                .build());
      }

      // DATA: GROUP_MEMBERSHIP
      // TODO

      // DATA: POSTAL
      for (Address address : card.getAddresses()) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getLabel())
                .build());
      }

      // DATA: WEBSITE
      for (Url website : card.getUrls()) {
        insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Website.URL, website.getValue())
                .build());
      }

      // DATA: EVENT
      // TODO

      // DATA: RELATIONSHIP
      // TODO

      // DATA: SIP_ADDRESS
      // TODO

      // Data: CTAG
      insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
              .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
              .withValue(ContactsContract.Data.MIMETYPE, ctx.getResources().getString(R.string.contact_item_ctag_mimetype))
              .withValue(ContactsContract.Data.DATA1, cTag)
              .build());

      // DATA: URL
      insertOps.add(ContentProviderOperation.newInsert(Utils.addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
              .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
              .withValue(ContactsContract.Data.MIMETYPE, ctx.getResources().getString(R.string.contact_item_url_mimetype))
              .withValue(ContactsContract.Data.DATA1, url)
              .build());

      try {
        ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, insertOps);
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
}

