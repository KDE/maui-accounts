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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Exception;

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

      String RAW_CONTACT_SELECTION = ContactsContract.RawContacts.ACCOUNT_TYPE + " = '" + accountType + "'";
      String DATA_SELECTION = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
      Cursor contacts = ctx.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, RAW_CONTACT_SELECTION, null, null);
      Cursor commonDataCursor;

      contacts.moveToFirst();

      while (!contacts.isAfterLast()) {
        VCard vCard = new VCard();
        String rawContactId = contacts.getString(contacts.getColumnIndex(ContactsContract.RawContacts._ID));
        String cTag;
        String DATA_SELECTION_ARGS[] = {rawContactId, ""};
        String PROJECTION[];

        // DATA: NAME
        DATA_SELECTION_ARGS[1] = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE;
        PROJECTION = new String[]{ContactsContract.CommonDataKinds.StructuredName.PREFIX, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, ContactsContract.CommonDataKinds.StructuredName.SUFFIX};
        commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
        commonDataCursor.moveToFirst();

        if (commonDataCursor.getCount() > 0) {
            StructuredName name = new StructuredName();
            name.setGiven(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)));
            name.setFamily(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)));
            name.getPrefixes().add(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)));
            name.getSuffixes().add(commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)));

            vCard.setStructuredName(name);
            vCard.setFormattedName(
                commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)) + " " +
                commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME))
            );
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
        // TODO
        DATA_SELECTION_ARGS[1] = ctx.getResources().getString(R.string.contact_item_ctag_mimetype);
        PROJECTION = new String[]{ContactsContract.Data.DATA1};
        commonDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI, PROJECTION, DATA_SELECTION, DATA_SELECTION_ARGS, null);
        commonDataCursor.moveToFirst();

        try {
            cTag = commonDataCursor.getString(commonDataCursor.getColumnIndex(ContactsContract.Data.DATA1));
        } catch (Exception e) {
            cTag = "";
        }

        // GENERATE vCard
        String vCardStr = Ezvcard.write(vCard).version(VCardVersion.V3_0).go();
        serializedData.add(new String[] {vCardStr, cTag == null ? "" : cTag, rawContactId});

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
}

