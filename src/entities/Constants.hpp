#ifndef ENTITIES_CONSTANTS_HPP
#define ENTITIES_CONSTANTS_HPP

#include <QString>

class Constants {
 public:
  static const QString SYNC_OPERATION_INSERT;
  static const QString SYNC_OPERATION_UPDATE;
  static const QString SYNC_OPERATION_INSERT_URL_CTAG;
  static const QString SYNC_OPERATION_UPDATE_CTAG;
  static const QString SYNC_OPERATION_DELETE;
};

const QString Constants::SYNC_OPERATION_INSERT = "sync_op_insert";
const QString Constants::SYNC_OPERATION_UPDATE = "sync_op_update";
const QString Constants::SYNC_OPERATION_INSERT_URL_CTAG =
    "sync_op_insert_url_ctag";
const QString Constants::SYNC_OPERATION_UPDATE_CTAG = "sync_op_update_ctag";
const QString Constants::SYNC_OPERATION_DELETE = "sync_op_delete";

#endif
