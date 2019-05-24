#include "MainViewController.hpp"
#include "../libccdav/lib/CardDAV.hpp"
#include "../libccdav/lib/utils/CardDAVReply.hpp"

#ifdef ANDROID
#include <jni.h>
#include <QAndroidJniObject>
#include <QtAndroidExtras/QAndroidJniEnvironment>
#include <QtAndroidExtras/QAndroidJniObject>
#else
#include <QtConcurrent>
#include "../entities/SyncManager.hpp"
#endif

#include <QDebug>
#include <QJsonArray>
#include <QJsonDocument>
#include <QNetworkReply>
#include <QStandardPaths>
#include <QTimer>

#ifdef ANDROID
void MainViewController::addOpendesktopAccount(QString protocol,
                                               QString username,
                                               QString password) {
  QString accountName = username + " - OpenDesktop";
  QString url =
      "https://cloud.opendesktop.cc/remote.php/dav/addressbooks/users/" +
      username + "/contacts";

  addAccount(protocol, url, username, password, accountName);
}

void MainViewController::addCustomAccount(QString protocol, QString url,
                                          QString username, QString password) {
  QString accountName = username + " - " + QUrl(url).host();

  addAccount(protocol, url, username, password, accountName);
}

void MainViewController::getAccountList() {
  QAndroidJniEnvironment env;
  QList<QString> accounts;

  QAndroidJniObject accountsArrayJniObject =
      QAndroidJniObject::callStaticObjectMethod(
          "org/mauikit/accounts/MainActivity", "getAccounts",
          "()[[Ljava/lang/String;");

  jobjectArray accountsJniArray = accountsArrayJniObject.object<jobjectArray>();
  int len = env->GetArrayLength(accountsJniArray);

  for (int i = 0; i < len; i++) {
    jobjectArray array = static_cast<jobjectArray>(
        env->GetObjectArrayElement(accountsJniArray, i));
    jstring jAccountName =
        static_cast<jstring>(env->GetObjectArrayElement(array, 0));
    jstring jCount = static_cast<jstring>(env->GetObjectArrayElement(array, 1));
    const char *accountName = env->GetStringUTFChars(jAccountName, 0);
    const char *count = env->GetStringUTFChars(jCount, 0);

    qDebug() << accountName << count;
    accounts.append(QString(accountName).append(" (" + QString(count) + ")"));

    env->ReleaseStringUTFChars(jAccountName, accountName);
    env->ReleaseStringUTFChars(jCount, count);
  }

  emit accountList(accounts);
}

void MainViewController::removeAccount(QString accountName) {
  showToast("Deleting Account");

  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "removeAccount",
      "(Ljava/lang/String;)V",
      QAndroidJniObject::fromString(accountName).object<jstring>());

  QTimer::singleShot(1000, this, [=]() { this->getAccountList(); });
}

void MainViewController::syncAccount(QString accountName) {
  showToast("Syncing Account");

  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "syncAccount",
      "(Ljava/lang/String;)V",
      QAndroidJniObject::fromString(accountName).object<jstring>());
}

void MainViewController::showUrl(QString accountName) {
  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "showUrl", "(Ljava/lang/String;)V",
      QAndroidJniObject::fromString(accountName).object<jstring>());
}

void MainViewController::addAccount(QString protocol, QString url,
                                    QString username, QString password,
                                    QString accountName) {
  CardDAV *m_CardDAV = new CardDAV(url, username, password);

  //  showToast("Checking Server Credentials");
  showIndefiniteProgressDialog("Checking Server Credentials", false);

  CardDAVReply *reply = m_CardDAV->testConnection();
  this->connect(
      reply, &CardDAVReply::testConnectionResponse, [=](bool isSuccess) {
        if (isSuccess) {
          QAndroidJniObject::callStaticMethod<void>(
              "org/mauikit/accounts/MainActivity", "createSyncAccount",
              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
              "Ljava/lang/String;)V",
              QAndroidJniObject::fromString(accountName).object<jstring>(),
              QAndroidJniObject::fromString(username).object<jstring>(),
              QAndroidJniObject::fromString(password).object<jstring>(),
              QAndroidJniObject::fromString(url).object<jstring>());

          qDebug() << "Account Added to System";
          emit accountAdded();

          this->getAccountList();

          hideIndefiniteProgressDialog();
          showToast("Account Added to System");
        } else {
          qDebug() << "Invalid Username or Password";
          showToast("Invalid Username or Password");
        }
      });
  this->connect(reply, &CardDAVReply::error,
                [=](QNetworkReply::NetworkError err) {
                  qDebug() << "Unknown Error Occured." << err;
                  //                  showToast("Unknown Error Occured");
                });
}

void MainViewController::showToast(QString text) {
  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "showToast", "(Ljava/lang/String;)V",
      QAndroidJniObject::fromString(text).object<jstring>());
}

void MainViewController::showIndefiniteProgressDialog(QString message,
                                                      bool isCancelable) {
  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "showIndefiniteProgressDialog",
      "(Ljava/lang/String;Z)V",
      QAndroidJniObject::fromString(message).object<jstring>(),
      isCancelable ? JNI_TRUE : JNI_FALSE);
}

void MainViewController::hideIndefiniteProgressDialog() {
  QAndroidJniObject::callStaticMethod<void>("org/mauikit/accounts/MainActivity",
                                            "hideIndefiniteProgressDialog");
}
#else
MainViewController::MainViewController() {
  QDir appDataFolder(QStandardPaths::writableLocation(
      QStandardPaths::StandardLocation::AppDataLocation));
  if (!appDataFolder.exists()) {
    appDataFolder.mkpath(".");
  }

  accountsJsonFilePath =
      QStandardPaths::writableLocation(
          QStandardPaths::StandardLocation::AppDataLocation) +
      "/accounts.json";
  wallet = KWallet::Wallet::openWallet(KWallet::Wallet::NetworkWallet(), 0,
                                       KWallet::Wallet::OpenType::Synchronous);

  if (!wallet->hasFolder(WALLET_FOLDER_NAME)) {
    wallet->createFolder(WALLET_FOLDER_NAME);
  }
  wallet->setFolder(WALLET_FOLDER_NAME);

  QFile accountJsonFile(accountsJsonFilePath);

  if (!accountJsonFile.open(QIODevice::ReadOnly)) {
    qWarning("Couldn't open config file.");
  }

  accountsJsonObject =
      QJsonDocument::fromJson(accountJsonFile.readAll()).object();

  accountJsonFile.close();
}

void MainViewController::writeAccountsJsonObjectToFile() {
  QFile accountJsonFile(accountsJsonFilePath);

  if (!accountJsonFile.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
    qWarning("Couldn't open config file.");
  }

  accountJsonFile.write(QJsonDocument(accountsJsonObject).toJson());
  accountJsonFile.close();
}

void MainViewController::addOpendesktopAccount(QString protocol,
                                               QString username,
                                               QString password) {
  QString accountName = username + " - OpenDesktop";
  QString url =
      "https://cloud.opendesktop.cc/remote.php/dav/addressbooks/users/" +
      username + "/contacts";

  addAccount(protocol, url, username, password, accountName);
}

void MainViewController::addCustomAccount(QString protocol, QString server,
                                          QString username, QString password) {
  addAccount(protocol, server, username, password, username);
}

void MainViewController::getAccountList() {
  QJsonArray accounts = accountsJsonObject[JSON_FIELD_ACCOUNTS].toArray();
  QList<QString> accountsStringArray;

  for (int i = 0; i < accounts.size(); i++) {
    accountsStringArray.append(
        accounts[i]
            .toObject()[JSON_ACCOUNT_ARRAY_FIELD_ACCOUNTNAME]
            .toString());
  }

  emit accountList(accountsStringArray);
}

void MainViewController::removeAccount(QString accountName) {
  QJsonArray accountsArray = accountsJsonObject[JSON_FIELD_ACCOUNTS].toArray();

  for (int i = 0; i < accountsArray.size(); i++) {
    QJsonObject accountObject = accountsArray[i].toObject();

    if (accountObject[JSON_ACCOUNT_ARRAY_FIELD_ACCOUNTNAME].toString() ==
        accountName) {
      qDebug() << "Removing account" << accountName;

      accountsArray.removeAt(i);
      accountsJsonObject[JSON_FIELD_ACCOUNTS] = accountsArray;
      wallet->removeEntry(
          accountObject[JSON_ACCOUNT_ARRAY_FIELD_USERNAME].toString());

      break;
    }
  }

  writeAccountsJsonObjectToFile();
  getAccountList();
}

void MainViewController::syncAccount(QString accountName) {
  QJsonArray accountsArray = accountsJsonObject[JSON_FIELD_ACCOUNTS].toArray();

  for (int i = 0; i < accountsArray.size(); i++) {
    QJsonObject accountObject = accountsArray[i].toObject();

    if (accountObject[JSON_ACCOUNT_ARRAY_FIELD_ACCOUNTNAME].toString() ==
        accountName) {
      qDebug() << "Syncing account" << accountName;

      QByteArray password;
      wallet->readEntry(
          accountObject[JSON_ACCOUNT_ARRAY_FIELD_USERNAME].toString(),
          password);

      QtConcurrent::run([=]() {
        SyncManager *manager = new SyncManager(
            accountName,
            accountObject[JSON_ACCOUNT_ARRAY_FIELD_USERNAME].toString(),
            QString::fromStdString(password.toStdString()),
            accountObject[JSON_ACCOUNT_ARRAY_FIELD_URL].toString());
        manager->doSync();

        qDebug() << "Sync Complete";
      });

      break;
    }
  }
}

void MainViewController::showUrl(QString accountName) {}

void MainViewController::addAccount(QString protocol, QString url,
                                    QString username, QString password,
                                    QString accountName) {
  CardDAV *m_CardDAV = new CardDAV(url, username, password);

  emit showIndefiniteProgress("Checking Server Credentials");

  CardDAVReply *reply = m_CardDAV->testConnection();
  this->connect(
      reply, &CardDAVReply::testConnectionResponse, [=](bool isSuccess) {
        if (isSuccess) {
          if (!accountsJsonObject.contains(JSON_FIELD_ACCOUNTS)) {
            accountsJsonObject[JSON_FIELD_ACCOUNTS] = QJsonArray();
          }

          QJsonArray accountsArray =
              accountsJsonObject[JSON_FIELD_ACCOUNTS].toArray();

          QJsonObject accountObject;
          accountObject[JSON_ACCOUNT_ARRAY_FIELD_URL] = url;
          accountObject[JSON_ACCOUNT_ARRAY_FIELD_PROTOCOL] = protocol;
          accountObject[JSON_ACCOUNT_ARRAY_FIELD_USERNAME] = username;
          accountObject[JSON_ACCOUNT_ARRAY_FIELD_ACCOUNTNAME] = accountName;

          wallet->writeEntry(username,
                             QByteArray::fromStdString(password.toStdString()));

          accountsArray.append(accountObject);
          accountsJsonObject[JSON_FIELD_ACCOUNTS] = accountsArray;

          writeAccountsJsonObjectToFile();

          qDebug() << "Account Added to System";
          emit accountAdded();

          this->getAccountList();

          emit hideIndefiniteProgress();
          showToast("Account Added to System");
        } else {
          qDebug() << "Invalid Username or Password";
          showToast("Invalid Username or Password");
        }
      });
  this->connect(reply, &CardDAVReply::error,
                [=](QNetworkReply::NetworkError err) {
                  qDebug() << "Unknown Error Occured." << err;
                  //                  showToast("Unknown Error Occured");
                });
}

void MainViewController::showToast(QString text) {}

void MainViewController::showIndefiniteProgressDialog(QString message,
                                                      bool isCancelable) {}

void MainViewController::hideIndefiniteProgressDialog() {}

MainViewController::AccountData::AccountData(QString accountName,
                                             QString protocol, QString url,
                                             QString username,
                                             QString password) {
  this->accountName = accountName;
  this->protocol = protocol;
  this->url = url;
  this->username = username;
  this->password = password;
}
#endif
