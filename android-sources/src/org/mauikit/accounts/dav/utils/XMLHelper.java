package org.mauikit.accounts.dav.utils;

import org.mauikit.accounts.dav.dto.CardDAVResponseItem;
import org.mauikit.accounts.dav.dto.Contact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLHelper {
  private String webdavNS = "*";
  private String cardDavNS = "urn:ietf:params:xml:ns:carddav";

  public List<CardDAVResponseItem> parseCardDAVMultiStatusResponse(String responseXml) throws ParserConfigurationException, IOException, SAXException, URISyntaxException {
    List<CardDAVResponseItem> responseTagList = new ArrayList<>();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);

    DocumentBuilder builder = factory.newDocumentBuilder();
    InputStream is = new ByteArrayInputStream(responseXml.getBytes());
    Document doc = builder.parse(is);

    NodeList responses = doc.getElementsByTagNameNS(webdavNS, "response");

    for (int i = 0; i < responses.getLength(); i++) {
      CardDAVResponseItem item = new CardDAVResponseItem();
      Element response = (Element) responses.item(i);
      String href = response.getElementsByTagNameNS(webdavNS, "href").item(0).getTextContent();

      item.setHref(new URI(href.replaceAll("/$", "")));

      NodeList resourceType = response.getElementsByTagNameNS(webdavNS, "resourcetype");

      if (resourceType.getLength() > 0 && ((Element) resourceType.item(0)).getElementsByTagNameNS(webdavNS, "collection").getLength() == 1) {
        item.setIsCollection(true);
      } else {
        item.setIsCollection(false);
      }

      if (resourceType.getLength() > 0 && ((Element) resourceType.item(0)).getElementsByTagNameNS(webdavNS, "addressbook").getLength() == 1) {
        item.setIsAddressBook(true);
      } else {
        item.setIsAddressBook(false);
      }

      if (response.getElementsByTagNameNS(cardDavNS, "address-data").getLength() == 1) {
        String vCard, eTag;

        item.setIsContact(true);

        vCard = response.getElementsByTagNameNS(cardDavNS, "address-data").item(0).getTextContent();
        eTag = response.getElementsByTagNameNS(webdavNS, "getetag").item(0).getTextContent();

        item.setContact(new Contact(vCard, eTag, new URI(href.replaceAll("/$", ""))));
      } else {
        item.setIsContact(false);
      }

      responseTagList.add(item);
    }

    return responseTagList;
  }
}
