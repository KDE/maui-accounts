#include "LocalContacts.hpp"
#include "Constants.hpp"
#include "SyncManager.hpp"

#include <QDebug>
#include <QDirIterator>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QStandardPaths>

LocalContacts::LocalContacts(QString accountName) {
  this->accountName = accountName;
  contactsFolderPath =
      QStandardPaths::writableLocation(QStandardPaths::GenericDataLocation) +
      "/kpeoplevcard/";
  contacsMetadataJsonPath =
      QStandardPaths::writableLocation(
          QStandardPaths::StandardLocation::AppDataLocation) +
      "/contacts-metadata.json";

  QDir appDataFolder(QStandardPaths::writableLocation(
      QStandardPaths::StandardLocation::AppDataLocation));
  if (!appDataFolder.exists()) {
    appDataFolder.mkpath(".");
  }

  if (!QDir(contactsFolderPath).exists()) {
    QDir(contactsFolderPath).mkpath(".");
  }

  QFile contactsMetadataJson(contacsMetadataJsonPath);

  if (!contactsMetadataJson.exists()) {
    contactsMetadataJson.open(QIODevice::ReadWrite);

    QJsonObject root;
    root[JSON_FIELD_LAST_CONTACT_ID] = 0;
    root[JSON_FIELD_CONTACTS] = QJsonObject();

    contactsMetadataJson.write(QJsonDocument(root).toJson());
    contactsMetadataJson.close();
  }

  qDebug() << "KContacts Path :" << contactsFolderPath;
}

QList<LocalContacts::Contact> LocalContacts::getContacts() {
  QList<LocalContacts::Contact> contacts;

  QJsonObject root = QJsonDocument::fromJson(readMetadataJsonFile()).object();

  QDirIterator iterator(contactsFolderPath);
  while (iterator.hasNext()) {
    QFile file(iterator.next());

    if (file.open(QIODevice::ReadOnly)) {
      QString rawContactId =
          getRawContactIdFromContactFilename(QFileInfo(file).fileName());
      QJsonObject contactObj =
          root[JSON_FIELD_CONTACTS].toObject()[rawContactId].toObject();
      QString vCard = file.readAll();
      QString cTag = contactObj[JSON_CONTACT_FIELD_CTAG].toString();
      QString url = contactObj[JSON_CONTACT_FIELD_URL].toString();

      contacts.append(Contact(vCard, cTag, url, rawContactId.toInt()));
    } else {
      qDebug() << "Can't open " << QFileInfo(file).fileName();
    }

    file.close();
  }

  return contacts;
}

QList<LocalContacts::Contact> LocalContacts::getDeletedContacts() {
  QList<LocalContacts::Contact> contacts;
  return contacts;
}

void LocalContacts::syncContacts(QList<QList<QString> > ops) {
  for (QList<QString> op : ops) {
    QString operation = op.at(0);
    QString vCard = op.at(1);
    QString cTag = op.at(2);
    QString url = op.at(3);
    int rawContactId = op.at(4).toInt();

    if (operation == Constants::SYNC_OPERATION_INSERT) {
      insertContact(accountName, vCard, cTag, url);
    } else if (operation == Constants::SYNC_OPERATION_UPDATE) {
      deleteContact(rawContactId);
      insertContact(accountName, vCard, cTag, url);
    } else if (operation == Constants::SYNC_OPERATION_INSERT_URL_CTAG) {
      updateContact(rawContactId, url, cTag);
    } else if (operation == Constants::SYNC_OPERATION_UPDATE_CTAG) {
      updateContact(rawContactId, cTag);
    } else if (operation == Constants::SYNC_OPERATION_DELETE) {
      deleteContact(rawContactId);
    }
  }
}

void LocalContacts::insertContact(QString accountName, QString vCard,
                                  QString cTag, QString url) {
  qDebug() << "Inserting Contact";

  QJsonObject root = QJsonDocument::fromJson(readMetadataJsonFile()).object();

  int newRawContactId = root[JSON_FIELD_LAST_CONTACT_ID].toInt() + 1;
  root[JSON_FIELD_LAST_CONTACT_ID] = newRawContactId;

  QJsonObject contactJsonObj;
  contactJsonObj[JSON_CONTACT_FIELD_ACCOUNTNAME] = accountName;
  contactJsonObj[JSON_CONTACT_FIELD_CTAG] = cTag;
  contactJsonObj[JSON_CONTACT_FIELD_URL] = url;

  QJsonObject contacts = root[JSON_FIELD_CONTACTS].toObject();
  contacts.insert(QString::number(newRawContactId), contactJsonObj);

  root[JSON_FIELD_CONTACTS] = contacts;

  writeMetadataJsonFile(QJsonDocument(root).toJson());

  QFile contactVcfFile(
      contactsFolderPath +
      generateContactFilename(QString::number(newRawContactId)));
  contactVcfFile.open(QIODevice::WriteOnly | QIODevice::Truncate);
  contactVcfFile.write(vCard.toUtf8());

  contactVcfFile.close();
}

void LocalContacts::updateContact(int rawContactId, QString url, QString cTag) {
  qDebug() << "Updating Contact";

  QJsonObject root = QJsonDocument::fromJson(readMetadataJsonFile()).object();

  QJsonObject contact = root[JSON_FIELD_CONTACTS]
                            .toObject()[QString::number(rawContactId)]
                            .toObject();

  if (url != nullptr) {
    contact[JSON_CONTACT_FIELD_URL] = url;
  }
  contact[JSON_CONTACT_FIELD_CTAG] = cTag;

  root[JSON_FIELD_CONTACTS].toObject()[QString::number(rawContactId)] = contact;

  writeMetadataJsonFile(QJsonDocument(root).toJson());
}

void LocalContacts::updateContact(int rawContactId, QString cTag) {
  updateContact(rawContactId, nullptr, cTag);
}

void LocalContacts::deleteContact(int rawContactId) {
  qDebug() << "Deleting Contact";

  QJsonObject root = QJsonDocument::fromJson(readMetadataJsonFile()).object();

  root[JSON_FIELD_CONTACTS].toObject().remove(QString::number(rawContactId));

  writeMetadataJsonFile(QJsonDocument(root).toJson());
}

QString LocalContacts::generateContactFilename(QString rawContactId) {
  return QString("org.mauikit.accounts-") + rawContactId + ".vcf";
}

QString LocalContacts::getRawContactIdFromContactFilename(QString filename) {
  QString tmp = filename;
  tmp.replace("org.mauikit.accounts-", "");
  tmp.replace(".vcf", "");

  return tmp;
}

QByteArray LocalContacts::readMetadataJsonFile() {
  QFile metadata(contacsMetadataJsonPath);
  metadata.open(QIODevice::ReadOnly);
  QByteArray data = metadata.readAll();
  metadata.close();

  return data;
}

void LocalContacts::writeMetadataJsonFile(QByteArray json) {
  QFile metadata(contacsMetadataJsonPath);
  metadata.open(QIODevice::WriteOnly | QIODevice::Truncate);
  metadata.write(json);
  metadata.close();
}

LocalContacts::Contact::Contact(QString vCard, QString cTag, QString url,
                                int rawContactId) {
  this->vCard = vCard;
  this->cTag = cTag;
  this->url = url;
  this->rawContactId = rawContactId;
}
