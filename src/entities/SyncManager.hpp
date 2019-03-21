#ifndef ENTITIES_SYNCMANAGER_HPP
#define ENTITIES_SYNCMANAGER_HPP

#include "../libccdav/lib/CardDAV.hpp"

#include <QCoreApplication>
#include <QNetworkReply>
#include <QObject>
#include <QString>

class SyncManager : public QObject {
 public:
  SyncManager(QString username, QString password, QString url);
  void doSync();

 private:
  CardDAV *m_CardDAV;
  QCoreApplication *app;

  const QString uuidNs = "eeebe4e7-2900-483c-aabf-a1b6e0b278fe";
  const QString SYNC_OPERATION_INSERT = "sync_op_insert";
  const QString SYNC_OPERATION_UPDATE = "sync_op_update";
  const QString SYNC_OPERATION_DELETE = "sync_op_delete";

  void handleNetworkError(QNetworkReply::NetworkError err);
  QList<QString> createContact(QString rawContactId, QString vCard);
  QList<QString> updateContact(QString rawContactId, QString cTag,
                               QString vCard);
  QList<QString> deleteContact(QString rawContactId);

  void parseAndSendOps(QList<QList<QString>> ops);
};

#endif
