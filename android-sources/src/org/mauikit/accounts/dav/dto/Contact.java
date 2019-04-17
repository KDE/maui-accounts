package org.mauikit.accounts.dav.dto;

import java.net.URI;

public class Contact {
  private String vCard;
  private String eTag;
  private URI href;

  public Contact(String vCard, String eTag, URI href) {
    this.vCard = vCard;
    this.eTag = eTag;
    this.href = href;
  }

  public String getVcard() { return vCard; }
  public String getEtag() { return eTag; }
  public URI getHref() { return href; }
}
