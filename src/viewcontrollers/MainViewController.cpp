#include "MainViewController.hpp"
#include "../libccdav/lib/CardDAV.hpp"
#include "../libccdav/lib/utils/CardDAVReply.hpp"

#ifdef ANDROID
#include <jni.h>
#include <QAndroidJniObject>
#include <QtAndroidExtras/QAndroidJniEnvironment>
#include <QtAndroidExtras/QAndroidJniObject>
#endif

#include <QDebug>
#include <QNetworkReply>
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
          "()[Ljava/lang/String;");

  jobjectArray accountsJniArray = accountsArrayJniObject.object<jobjectArray>();
  int len = env->GetArrayLength(accountsJniArray);

  for (int i = 0; i < len; i++) {
    jstring string =
        static_cast<jstring>(env->GetObjectArrayElement(accountsJniArray, i));
    const char *accountName = env->GetStringUTFChars(string, 0);

    qDebug() << accountName;
    accounts.append(accountName);

    env->ReleaseStringUTFChars(string, accountName);
  }

  emit accountList(accounts);
}

void MainViewController::removeAccount(QString accountName) {
  emit showToast("Deleting Account");

  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "removeAccount",
      "(Ljava/lang/String;)V",
      QAndroidJniObject::fromString(accountName).object<jstring>());

  QTimer::singleShot(1000, this, [=]() { this->getAccountList(); });
}

void MainViewController::syncAccount(QString accountName) {
  emit showToast("Syncing Account");

  QAndroidJniObject::callStaticMethod<void>(
      "org/mauikit/accounts/MainActivity", "syncAccount",
      "(Ljava/lang/String;)V",
      QAndroidJniObject::fromString(accountName).object<jstring>());
}

void MainViewController::addAccount(QString protocol, QString url,
                                    QString username, QString password,
                                    QString accountName) {
  CardDAV *m_CardDAV = new CardDAV(url, username, password);

  emit showToast("Checking Server Credentials");

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

          emit showToast("Account Added to System");
        } else {
          qDebug() << "Invalid Username or Password";
          emit showToast("Invalid Username or Password");
        }
      });
  this->connect(reply, &CardDAVReply::error,
                [=](QNetworkReply::NetworkError err) {
                  qDebug() << "Unknown Error Occured." << err;
                  //                  emit showToast("Unknown Error Occured");
                });
}
#else
void MainViewController::addOpendesktopAccount(QString protocol,
                                               QString username,
                                               QString password) {}

void MainViewController::addCustomAccount(QString protocol, QString server,
                                          QString username, QString password) {}

void MainViewController::getAccountList() {}

void MainViewController::removeAccount(QString accountName) {}

void MainViewController::syncAccount(QString accountName) {}
#endif
