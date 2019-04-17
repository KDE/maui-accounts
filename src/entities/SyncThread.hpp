#include "./SyncManager.hpp"

#include <QObject>
#include <QThread>

#ifndef ENTITIES_SYNCTHREAD_HPP
#define ENTITIES_SYNCTHREAD_HPP

class SyncThread : public QThread {
  Q_OBJECT
  void run() override {
    //    SyncManager* syncManager = new SyncManager(_username, _password,
    //    _url); syncManager->doSyncAndroid(); syncManager->deleteLater();
  }

 public:
  SyncThread(QString username, QString password, QString url) {
    _username = username;
    _password = password;
    _url = url;
  }

 private:
  QString _username;
  QString _password;
  QString _url;
};

#endif
