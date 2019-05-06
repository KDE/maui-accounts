#ifndef ENTITIES_LOCALCONTACTS_HPP
#define ENTITIES_LOCALCONTACTS_HPP

#include <QList>
#include <QString>

class LocalContacts {
 public:
  LocalContacts(QString accountName);

  class Contact {
   public:
    QString vCard;
    QString cTag;
    QString url;
    int rawContactId;

    Contact(QString vCard, QString cTag, QString url, int rawContactId);
  };

  QList<Contact> getContacts();
  QList<Contact> getDeletedContacts();
  void syncContacts(QList<QList<QString>> ops);

 private:
  QString contactsFolderPath;
  QString contacsMetadataJsonPath;
  QString accountName;

  const QString JSON_FIELD_LAST_CONTACT_ID = "lastContactId";
  const QString JSON_FIELD_CONTACTS = "contacts";
  const QString JSON_CONTACT_FIELD_ACCOUNTNAME = "accountName";
  const QString JSON_CONTACT_FIELD_URL = "url";
  const QString JSON_CONTACT_FIELD_CTAG = "cTag";

  void insertContact(QString accountName, QString vCard, QString cTag,
                     QString url);
  void updateContact(int rawContactId, QString url, QString cTag);
  void updateContact(int rawContactId, QString cTag);
  void deleteContact(int rawContactId);
  QString generateContactFilename(QString rawContactId);
  QString getRawContactIdFromContactFilename(QString filename);

  QByteArray readMetadataJsonFile();
  void writeMetadataJsonFile(QByteArray json);
};

#endif
