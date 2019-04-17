package org.mauikit.accounts.dav;

import org.mauikit.accounts.dav.dto.CardDAVResponseItem;
import org.mauikit.accounts.dav.dto.Contact;
import org.mauikit.accounts.dav.utils.NetworkHelper;
import org.mauikit.accounts.dav.utils.XMLHelper;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import okhttp3.Headers;
import okhttp3.Response;

public class CardDAV {
  private String host;
  private String username;
  private String password;
  private NetworkHelper networkHelper;
  private XMLHelper xmlHelper;

  public CardDAV(String host, String username, String password) {
    this.host = host.replaceAll("/$", "");
    this.username = username;
    this.password = password;

    this.networkHelper = new NetworkHelper(host, username, password);
    this.xmlHelper = new XMLHelper();
  }

  public boolean testConnection() throws IOException, URISyntaxException, ParserConfigurationException, SAXException {
    boolean found = false;
    String responseXml = networkHelper.makeRequest("PROPFIND", null).body().string();

    List<CardDAVResponseItem> responseList = xmlHelper.parseCardDAVMultiStatusResponse(responseXml);

    String host = this.host;
    host = host.replace("://", "");
    host = host.substring(host.indexOf("/"));

    URI hostUrl = new URI(host);

    for (CardDAVResponseItem item : responseList) {
      if (item.getHref().compareTo(hostUrl) == 0 && item.isAddressBook()) {
        found = true;

        break;
      }
    }

    if (found) {
      return true;
    } else {
      return false;
    }
  }

  public List<Contact> listAllContacts() throws IOException, URISyntaxException, ParserConfigurationException, SAXException {
    String requestXml = "<card:addressbook-query xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\"><d:prop><card:address-data /><d:getetag /></d:prop></card:addressbook-query>";
    Headers.Builder headersBuilder = new Headers.Builder();
    headersBuilder.add("Depth", "1");
    headersBuilder.add("Content-Type", "application/xml");

    List<CardDAVResponseItem> responses = xmlHelper.parseCardDAVMultiStatusResponse(networkHelper.makeRequest("REPORT", headersBuilder.build(), requestXml).body().string());
    List<Contact> contacts = new ArrayList<>();

    for (CardDAVResponseItem response : responses) {
      contacts.add(response.getContact());
    }

    return contacts;
  }

  public Contact createContact(String uid, String vCard) throws URISyntaxException, IOException {
    return createContact(uid, vCard, false);
  }

  public Contact createContact(String uid, String vCard, boolean shouldOverwrite) throws URISyntaxException, IOException {
    Headers.Builder headersBuilder = new Headers.Builder();
    URI contactUrl = new URI(host + "/" + uid + ".vcf");

    if (!shouldOverwrite) {
      headersBuilder.add("If-None-Match", "*");
    }

    networkHelper.makeRequest("PUT", contactUrl, headersBuilder.build(), vCard);

    Response getResponse = networkHelper.makeRequest("GET", contactUrl, null, null);

    String responsevCard = getResponse.body().string();
    String eTag = getResponse.header("ETag");

    return new Contact(responsevCard, eTag, contactUrl);
  }

  public Contact updateContact(URI href, String vCard, String etag) throws IOException, URISyntaxException {
    Headers.Builder headersBuilder = new Headers.Builder();
    headersBuilder.add("If-Match", etag);

    networkHelper.makeRequest("PUT", href, headersBuilder.build(), vCard);

    Response getResponse = networkHelper.makeRequest("GET", href, null, null);

    String responsevCard = getResponse.body().string();
    String responseEtag = getResponse.header("ETag");

    return new Contact(responsevCard, responseEtag, href);
  }

  public void deleteContact(URI href) throws IOException, URISyntaxException {
    networkHelper.makeRequest("DELETE", href, null, null);
  }

  ////////////////////////////////////////
  public static void main(String args[]) {
    CardDAV cardDAV = new CardDAV("https://cloud.opendesktop.cc/remote.php/dav/addressbooks/users/anupamb/contacts/", "anupamb", "anupam@opendesktop");

    try {
      System.out.println(cardDAV.testConnection());

      System.out.println(cardDAV.listAllContacts());

      Contact c = cardDAV.createContact("uid1", "BEGIN:VCARD\n" +
              "VERSION:3.0\n" +
              "PRODID:ez-vcard 0.10.5\n" +
              "N:test;Test;;null;null\n" +
              "FN:Test test\n" +
              "TEL:9876543210\n" +
              "EMAIL:test@tt.com\n" +
              "END:VCARD");

      System.out.println(c.getHref());
      System.out.println(c.getVcard());
      System.out.println(c.getEtag());

      Contact updatedContact = cardDAV.updateContact(c.getHref(), "BEGIN:VCARD\n" +
              "VERSION:3.0\n" +
              "PRODID:ez-vcard 0.10.5\n" +
              "N:test;Test Updated;;null;null\n" +
              "FN:Test Updated test\n" +
              "TEL:2444666660\n" +
              "EMAIL:test1@tt.com\n" +
              "END:VCARD", c.getEtag());


      System.out.println(updatedContact.getHref());
      System.out.println(updatedContact.getVcard());
      System.out.println(updatedContact.getEtag());

      cardDAV.deleteContact(updatedContact.getHref());
    } catch (Exception e) {
      e.printStackTrace();
    }


  }
}
