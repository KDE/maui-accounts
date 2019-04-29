#ifndef ENTITIES_SYNCMANAGER_HPP
#define ENTITIES_SYNCMANAGER_HPP

#include "../libccdav/lib/CardDAV.hpp"

#include <QCoreApplication>
#include <QNetworkReply>
#include <QObject>
#include <QString>

class SyncManager : public QObject {
  Q_OBJECT
 public:
  SyncManager(QString username, QString password, QString url);

#ifdef ANDROID
 public slots:
  void doSyncAndroid();
#endif

 signals:
  void syncComplete();

 private:
  const QString uuidNs = "eeebe4e7-2900-483c-aabf-a1b6e0b278fe";
  const QString SYNC_OPERATION_INSERT = "sync_op_insert";
  const QString SYNC_OPERATION_UPDATE = "sync_op_update";
  const QString SYNC_OPERATION_INSERT_URL_CTAG = "sync_op_insert_url_ctag";
  const QString SYNC_OPERATION_UPDATE_CTAG = "sync_op_update_ctag";
  const QString SYNC_OPERATION_DELETE = "sync_op_delete";

  CardDAV *m_CardDAV;
  QCoreApplication *app = nullptr;
  QString url;

  QList<QString> createContact(QString rawContactId, QString vCard);
  QList<QString> updateContact(QString rawContactId, QString cTag,
                               QString vCard, QString url);
  QList<QString> deleteContact(QString rawContactId, QString url);
  void handleNetworkError(QNetworkReply::NetworkError err);
  void parseAndSendOps(QList<QList<QString>> ops);
  QString getFilenameFromUrl(QString url);
  QString getLastIdFromUrl(QString url);
  QString generateContactUuid(QString vCard);
  QString generateFullUrl(QString baseUrl, QString filename);
  QList<QString> buildOperation(QString operation, QString vCard, QString cTag,
                                QString url, QString rawContactId);
};

#endif
