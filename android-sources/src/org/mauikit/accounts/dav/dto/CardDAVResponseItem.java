package org.mauikit.accounts.dav.dto;

import java.net.URI;

public class CardDAVResponseItem {
  private Contact contact;
  private URI href;

  private boolean flagIsCollection = false;
  private boolean flagIsAddressBook = false;
  private boolean flagIsContact = false;

  public CardDAVResponseItem() {}

  public CardDAVResponseItem(boolean isCollection, boolean isAddressBook, boolean isContact, URI href) {
    this.flagIsCollection = isCollection;
    this.flagIsAddressBook = isAddressBook;
    this.flagIsContact = isContact;
    this.href = href;
  }

  public boolean isCollection() { return this.flagIsCollection; }
  public boolean isAddressBook() { return this.flagIsAddressBook; }
  public boolean isContact() { return this.flagIsContact; }

  public Contact getContact() { return this.contact; }
  public URI getHref() { return this.href; }

  public void setIsCollection(boolean isCollection) { this.flagIsCollection = isCollection; }
  public void setIsAddressBook(boolean isAddressBook) { this.flagIsAddressBook = isAddressBook; }
  public void setIsContact(boolean isContact) { this.flagIsContact = isContact; }
  public void setContact(Contact contact) { this.contact = contact; }
  public void setHref(URI href) { this.href = href; }
}
