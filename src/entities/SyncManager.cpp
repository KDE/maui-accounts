#include "SyncManager.hpp"
#include "../libccdav/lib/CardDAV.hpp"
#include "../libccdav/lib/dto/Contact.hpp"
#include "../libccdav/lib/utils/CardDAVReply.hpp"
#include "../libccdav/lib/utils/NetworkHelper.hpp"

#include <QDebug>
#include <QList>
#include <QUuid>
#include <QtConcurrent>

SyncManager::SyncManager(QString accountName, QString username,
                         QString password, QString url) {
  this->m_CardDAV = new CardDAV(url, username, password);

  this->url = url;
  this->accountName = accountName;
  this->m_localContacts = new LocalContacts(accountName);
}

void SyncManager::doSync() {
  qDebug() << "doSync(): ";

  if (app == nullptr) {
    //    int argc = 1;
    //    char *argv[] = {"sync"};

    //    this->app = new QCoreApplication(argc, argv);
    this->app = new QEventLoop();
  }

  bool isError = false;
  QList<QList<QString>> ops;
  QList<Contact *> remoteContacts;

  CardDAVReply *reply = m_CardDAV->listAllContacts();
  this->connect(reply, &CardDAVReply::listAllContactsResponse,
                [=, &remoteContacts](QList<Contact *> contacts) {
                  remoteContacts = contacts;
                  app->exit(0);
                });
  this->connect(reply, &CardDAVReply::error,
                [=, &isError](QNetworkReply::NetworkError err) {
                  qDebug() << err;
                  isError = true;
                  app->exit(0);
                });
  app->exec();

  if (isError) {
    qDebug() << "Unknown Error. Stopping Sync";
    return;
  }

  qDebug() << "doSync: remoteContacts Length :" << remoteContacts.length();

  QList<LocalContacts::Contact> localContacts = m_localContacts->getContacts();

  qDebug() << "doSync: getContacts Length :" << localContacts.size();

  for (LocalContacts::Contact c : localContacts) {
    QString vCard = c.vCard;
    QString cTag = c.cTag;
    QString url = c.url;
    QString rawContactId = QString(c.rawContactId);

    qDebug() << "doSync: Syncing Contact :" << cTag << url << rawContactId
             << vCard;

    // TODO [X] : Upload New Local Contacts
    // TODO [X] : Upload Updated Local Contacts
    // TODO [X] : Download New Remote Contacts
    // TODO [X] : Download Updated Remote Contacts
    // TODO [ ] : Delete Local Contacts if Remote Deleted
    // TODO [X] : Delete Remote Contacts if Local Deleted

    if (url == "") {
      // Upload New Contact

      qDebug() << "doSync: New Contact";
      QList<QString> op = createContact(rawContactId, vCard);

      // op should contain 5 elements : operationType, vCard, cTag, url,
      // rawContactId
      if (op.size() == 5) {
        ops.append(op);
      }
    } else {
      Contact *remoteContact = nullptr;

      for (Contact *c : remoteContacts) {
        if (getFilenameFromUrl(c->getHref().toEncoded()) ==
            getFilenameFromUrl(url)) {
          qDebug() << "Found Remote contact :" << getFilenameFromUrl(url);
          remoteContact = c;

          remoteContacts.removeOne(c);

          break;
        }
      }

      // KNOWN_BUG
      // FIXME :
      if (remoteContact != nullptr) {
        QString localContactGeneratedFilename =
            generateContactUuid(vCard) + ".vcf";
        QString localContactSavedFilename = getFilenameFromUrl(url);

        qDebug() << "Filenames :" << localContactGeneratedFilename
                 << localContactSavedFilename;

        if (localContactSavedFilename == localContactGeneratedFilename) {
          if (remoteContact->getEtag() != cTag) {
            qDebug() << "Remote Contact Updated";
            QList<QString> op = buildOperation(
                this->SYNC_OPERATION_UPDATE, remoteContact->getVcard(),
                remoteContact->getEtag(), url, rawContactId);

            if (op.size() == 5) {
              ops.append(op);
            }
          } else {
            qDebug() << "Contact not Updated. Ignoring";
          }
        } else {
          qDebug() << "Local Contact Updated";

          QList<QString> op = updateContact(rawContactId, cTag, vCard, url);

          if (op.size() == 5) {
            ops.append(op);
          }
        }
      } else {
        qDebug()
            << "doSync: Control should never come here. Something is wrong";
      }
    }
  }

  QList<LocalContacts::Contact> localDeletedContacts =
      m_localContacts->getDeletedContacts();

  qDebug() << "doSync: getDeletedContacts Length :"
           << localDeletedContacts.size();

  for (LocalContacts::Contact c : localDeletedContacts) {
    QString url = c.url;
    QString rawContactId = QString(c.rawContactId);

    qDebug() << "doSync: Deleting Contact :" << url << rawContactId;

    QList<QString> op = deleteContact(rawContactId, url);
    // op should contain 5 elements : operationType, vCard, cTag, url,
    // rawContactId
    if (op.size() == 5) {
      ops.append(op);
    }

    for (Contact *c : remoteContacts) {
      if (getFilenameFromUrl(c->getHref().toEncoded()) ==
          getFilenameFromUrl(url)) {
        // Contact Removed from Remote Server
        remoteContacts.removeOne(c);
      }
    }
  }

  qDebug() << "doSync: Remaining Remote Contacts Length :"
           << remoteContacts.length();

  for (Contact *c : remoteContacts) {
    QList<QString> op =
        buildOperation(this->SYNC_OPERATION_INSERT, c->getVcard(), c->getEtag(),
                       c->getHref().toEncoded(), "");

    // op should contain 5 elements : operationType, vCard, cTag, url,
    // rawContactId
    if (op.size() == 5) {
      ops.append(op);
    }
  }

  m_localContacts->syncContacts(ops);
}

void SyncManager::handleNetworkError(QNetworkReply::NetworkError err) {
  qDebug() << err;
}

QList<QString> SyncManager::createContact(QString rawContactId, QString vCard) {
  QString cTag;
  QString uuid = generateContactUuid(vCard);
  bool isError = false;

  CardDAVReply *reply = m_CardDAV->createContact(uuid, vCard, true);
  this->connect(reply, &CardDAVReply::createContactResponse,
                [=, &cTag](Contact *contact) {
                  qDebug() << "\n\n    Contact Created."
                           << "\n    ETAG :" << contact->getEtag()
                           << "\n    Href:" << contact->getHref()
                           << "\n    vCard :" << contact->getVcard() << "\n";

                  cTag = contact->getEtag();

                  app->exit(0);
                });
  this->connect(reply, &CardDAVReply::error,
                [=, &isError](QNetworkReply::NetworkError err) {
                  qDebug() << err;
                  isError = true;
                  app->exit(0);
                });

  app->exec();

  if (!isError) {
    return buildOperation(this->SYNC_OPERATION_INSERT_URL_CTAG, vCard, cTag,
                          this->url + "/" + uuid + ".vcf", rawContactId);
  } else {
    qDebug() << "ERROR : Remote Contact Not Created";

    return QList<QString>();
  }
}

QList<QString> SyncManager::updateContact(QString rawContactId, QString cTag,
                                          QString vCard, QString url) {
  bool isError = false;
  Contact *remoteContact;

  CardDAVReply *reply = m_CardDAV->updateContact(QUrl(url), vCard, cTag);
  this->connect(reply, &CardDAVReply::updateContactResponse,
                [=, &remoteContact](Contact *contact) {
                  qDebug() << "\n\n    Contact Updated."
                           << "\n    ETAG :" << contact->getEtag()
                           << "\n    Href:" << contact->getHref()
                           << "\n    vCard :" << contact->getVcard() << "\n";

                  remoteContact = contact;

                  app->exit(0);
                });
  this->connect(reply, &CardDAVReply::error,
                [=, &isError](QNetworkReply::NetworkError err) {
                  qDebug() << err;
                  isError = true;
                  app->exit(0);
                });
  app->exec();

  if (!isError) {
    return buildOperation(this->SYNC_OPERATION_UPDATE_CTAG,
                          remoteContact->getVcard(), remoteContact->getEtag(),
                          url, rawContactId);
  } else {
    qDebug() << "ERROR : Remote Contact Not Updated";

    return QList<QString>();
  }
}

QList<QString> SyncManager::deleteContact(QString rawContactId, QString url) {
  bool isError = false;

  CardDAVReply *reply = m_CardDAV->deleteContact(QUrl(url));
  this->connect(reply, &CardDAVReply::deleteContactResponse, [=]() {
    qDebug() << "\n\n    Contact Deleted.";
    app->exit(0);
  });
  this->connect(reply, &CardDAVReply::error,
                [=, &isError](QNetworkReply::NetworkError err) {
                  qDebug() << err;
                  isError = true;
                  app->exit(0);
                });

  app->exec();

  if (isError) {
    qDebug() << "ERROR : Remote Contact Not Deleted";
  }

  return buildOperation(this->SYNC_OPERATION_DELETE, "", "", url, rawContactId);
}

QString SyncManager::getFilenameFromUrl(QString url) {
  return url.mid(url.lastIndexOf("/") + 1);
}

QString SyncManager::generateContactUuid(QString vCard) {
  return QUuid::createUuidV5(this->uuidNs, vCard)
      .toString(QUuid::StringFormat::WithoutBraces);
}

QList<QString> SyncManager::buildOperation(QString operation, QString vCard,
                                           QString cTag, QString url,
                                           QString rawContactId) {
  QList<QString> returnVal;
  returnVal.insert(0, operation);
  returnVal.insert(1, vCard);
  returnVal.insert(2, cTag);
  returnVal.insert(3, url);
  returnVal.insert(4, rawContactId);

  return returnVal;
}
