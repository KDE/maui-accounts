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
}

void SyncManager::doSync() {
  qDebug() << "doSync(): ";

  QList<QList<QString>> ops;

  QAndroidJniEnvironment env;
  QAndroidJniObject contactsArrayJniObject =
      QAndroidJniObject::callStaticObjectMethod(
          "org/mauikit/accounts/MainActivity", "getContacts",
          "()[[Ljava/lang/String;");

  jobjectArray contactsJniArray = contactsArrayJniObject.object<jobjectArray>();
  int len = env->GetArrayLength(contactsJniArray);

  qDebug() << "Length :" << len;

  for (int i = 0; i < len; i++) {
    jobjectArray stringArr = static_cast<jobjectArray>(
        env->GetObjectArrayElement(contactsJniArray, i));
    jstring e_vCard =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 0));
    jstring e_cTag =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 1));
    jstring e_rawContactId =
        static_cast<jstring>(env->GetObjectArrayElement(stringArr, 2));

    const char *vCard = env->GetStringUTFChars(e_vCard, 0);
    const char *cTag = env->GetStringUTFChars(e_cTag, 0);
    const char *rawContactId = env->GetStringUTFChars(e_rawContactId, 0);

    qDebug() << "Syncing Contact :" << vCard << cTag << rawContactId;

    // JOB 1 [ ] : Upload New Contacts
    // JOB 2 [ ] : Download New Remote Contacts
    // JOB 3 [ ] : Delete Local Contacts
    // JOB 4 [ ] : Delete Remote Contacts

    if (strcmp(cTag, "") == 0) {
      // Upload New Contact

      QList<QString> op = createContact(rawContactId, vCard);
      if (op.size() == 3) {
        ops.append(op);
      }
    }

    env->ReleaseStringUTFChars(e_vCard, vCard);
    env->ReleaseStringUTFChars(e_cTag, cTag);
  }

  this->parseAndSendOps(ops);
}

void SyncManager::handleNetworkError(QNetworkReply::NetworkError err) {
  qDebug() << err;
}

QList<QString> SyncManager::createContact(QString rawContactId, QString vCard) {
  QString cTag;
  QList<QString> returnVal;
  bool isError = false;

  CardDAVReply *reply = m_CardDAV->createContact(
      QUuid::createUuidV5(this->uuidNs, QString(vCard))
          .toString(QUuid::StringFormat::WithoutBraces),
      vCard, true);
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
    qDebug() << "OP cTag :" << cTag;

    returnVal.insert(0, this->SYNC_OPERATION_UPDATE);
    returnVal.insert(1, vCard);
    returnVal.insert(2, cTag);
  }

  return returnVal;
}

QList<QString> SyncManager::updateContact(QString rawContactId, QString cTag,
                                          QString vCard) {}

QList<QString> SyncManager::deleteContact(QString rawContactId) {}

void SyncManager::parseAndSendOps(QList<QList<QString>> ops) {
  qDebug() << ops;
}
