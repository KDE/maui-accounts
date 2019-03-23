#include "SyncManager.hpp"
#include "../libccdav/lib/CardDAV.hpp"
#include "../libccdav/lib/dto/Contact.hpp"
#include "../libccdav/lib/utils/CardDAVReply.hpp"
#include "../libccdav/lib/utils/NetworkHelper.hpp"

#include <jni.h>
#include <QDebug>
#include <QList>
#include <QUuid>
#include <QtAndroidExtras/QAndroidJniEnvironment>
#include <QtAndroidExtras/QAndroidJniObject>

SyncManager::SyncManager(QString username, QString password, QString url) {
  int argc = 1;
  char *argv[] = {"sync"};

  this->m_CardDAV = new CardDAV(url, username, password);
  this->app = new QCoreApplication(argc, argv);

  this->url = url;
}

void SyncManager::doSync() {
  qDebug() << "doSync(): ";

  bool isError = false;
  QList<QList<QString>> ops;
  QList<Contact *> remoteContacts;

  CardDAVReply *reply = m_CardDAV->listAllContacts();
  this->connect(reply, &CardDAVReply::listAllContactsResponse,
                [=, &remoteContacts](QList<Contact *> contacts) {
                  remoteContacts = contacts;
                  this->app->exit(0);
                });
  this->connect(reply, &CardDAVReply::error,
                [=, &isError](QNetworkReply::NetworkError err) {
                  qDebug() << err;
                  isError = true;
                  this->app->exit(0);
                });
  this->app->exec();

  if (isError) {
    qDebug() << "Unknown Error. Stopping Sync";
    return;
  }

  qDebug() << "doSync: remoteContacts Length :" << remoteContacts.length();

  QAndroidJniEnvironment env;
  QAndroidJniObject contactsArrayJniObject =
      QAndroidJniObject::callStaticObjectMethod(
          "org/mauikit/accounts/MainActivity", "getContacts",
          "()[[Ljava/lang/String;");
  jobjectArray contactsJniArray = contactsArrayJniObject.object<jobjectArray>();
  int len = env->GetArrayLength(contactsJniArray);

  qDebug() << "doSync: getContacts Length :" << len;

  for (int i = 0; i < len; i++) {
    jobjectArray stringArr = static_cast<jobjectArray>(
        env->GetObjectArrayElement(contactsJniArray, i));
    jstring e_vCard =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 0));
    jstring e_cTag =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 1));
    jstring e_url =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 2));
    jstring e_rawContactId =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 3));

    const char *vCard = env->GetStringUTFChars(e_vCard, 0);
    const char *cTag = env->GetStringUTFChars(e_cTag, 0);
    const char *url = env->GetStringUTFChars(e_url, 0);
    const char *rawContactId = env->GetStringUTFChars(e_rawContactId, 0);

    qDebug() << "doSync: Syncing Contact :" << cTag << url << rawContactId
             << vCard;

    // TODO [X] : Upload New Local Contacts
    // TODO [X] : Upload Updated Local Contacts
    // TODO [X] : Download New Remote Contacts
    // TODO [X] : Download Updated Remote Contacts
    // TODO [ ] : Delete Local Contacts if Remote Deleted
    // TODO [X] : Delete Remote Contacts if Local Deleted

    if (strcmp(url, "") == 0) {
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

    env->ReleaseStringUTFChars(e_vCard, vCard);
    env->ReleaseStringUTFChars(e_cTag, cTag);
    env->ReleaseStringUTFChars(e_url, url);
    env->ReleaseStringUTFChars(e_rawContactId, rawContactId);
  }

  contactsArrayJniObject = QAndroidJniObject::callStaticObjectMethod(
      "org/mauikit/accounts/MainActivity", "getDeletedContacts",
      "()[[Ljava/lang/String;");

  contactsJniArray = contactsArrayJniObject.object<jobjectArray>();
  len = env->GetArrayLength(contactsJniArray);

  qDebug() << "doSync: getDeletedContacts Length :" << len;

  for (int i = 0; i < len; i++) {
    jobjectArray stringArr = static_cast<jobjectArray>(
        env->GetObjectArrayElement(contactsJniArray, i));
    jstring e_url =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 0));
    jstring e_rawContactId =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 1));

    const char *url = env->GetStringUTFChars(e_url, 0);
    const char *rawContactId = env->GetStringUTFChars(e_rawContactId, 0);

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

    env->ReleaseStringUTFChars(e_url, url);
    env->ReleaseStringUTFChars(e_rawContactId, rawContactId);
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

  this->parseAndSendOps(ops);
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

void SyncManager::parseAndSendOps(QList<QList<QString>> ops) {
  QAndroidJniEnvironment env;
  jobjectArray jops = env->NewObjectArray(
      ops.length(), env->FindClass("[Ljava/lang/String;"), 0);
  int opCount = 0;

  for (QList<QString> op : ops) {
    int opElementCount = 0;
    jobjectArray jop =
        env->NewObjectArray(op.length(), env->FindClass("java/lang/String"), 0);

    for (QString opStr : op) {
      const char *str = opStr.toUtf8().toBase64().constData();
      jstring jstr = env->NewStringUTF(str);
      env->SetObjectArrayElement(jop, opElementCount, jstr);

      opElementCount++;
    }

    env->SetObjectArrayElement(jops, opCount, jop);

    opCount++;
  }

  QAndroidJniObject::callStaticMethod<void>("org/mauikit/accounts/MainActivity",
                                            "syncContacts",
                                            "([[Ljava/lang/String;)V", jops);
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
