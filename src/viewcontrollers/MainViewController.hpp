#ifndef VIEWCONTROLLERS_MAINVIEWCONTROLLER_HPP
#define VIEWCONTROLLERS_MAINVIEWCONTROLLER_HPP

#include <KWallet>
#include <QJsonObject>
#include <QObject>

class MainViewController : public QObject {
  Q_OBJECT

#ifndef ANDROID
 public:
  MainViewController();

 signals:
  void showIndefiniteProgress(QString message);
  void hideIndefiniteProgress();

 private:
  KWallet::Wallet *wallet;
  QJsonObject accountsJsonObject;

  const QString WALLET_FOLDER_NAME = "org.mauikit.accounts";
  const QString WALLET_ENTRY_ACCOUNTS = "accounts";
  const QString JSON_FIELD_ACCOUNTS = "accounts";
  const QString JSON_ACCOUNT_ARRAY_FIELD_PROTOCOL = "protocol";
  const QString JSON_ACCOUNT_ARRAY_FIELD_URL = "url";
  const QString JSON_ACCOUNT_ARRAY_FIELD_USERNAME = "username";
  const QString JSON_ACCOUNT_ARRAY_FIELD_ACCOUNTNAME = "account_name";

  QString accountsJsonFilePath;

  void writeAccountsJsonObjectToFile();

  class AccountData {
   public:
    AccountData(QString accountName, QString protocol, QString url,
                QString username, QString password);
    QString accountName;
    QString protocol;
    QString url;
    QString username;
    QString password;
  };

#endif

 public slots:
  void addOpendesktopAccount(QString protocol, QString username,
                             QString password);
  void addCustomAccount(QString protocol, QString url, QString username,
                        QString password);
  void getAccountList();
  void removeAccount(QString accountName);
  void syncAccount(QString accountName);
  void showUrl(QString accountName);

 signals:
  void accountList(QList<QString> accounts);
  void accountAdded();

 private:
  void addAccount(QString protocol, QString url, QString username,
                  QString password, QString accountName);
  void showToast(QString text);
  void showIndefiniteProgressDialog(QString message, bool isCancelable);
  void hideIndefiniteProgressDialog();
};

#endif
