QT += quick concurrent svg
CONFIG += c++11

linux:unix:!android {
    LIBS += -lKF5Wallet
}

android: {
    QT += androidextras

    ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android-sources

    include($$PWD/openssl/openssl.pri)

    DEFINES += ANDROID
}

include($$PWD/libccdav/ccdav.pri)

TEMPLATE = app

# The following define makes your compiler emit warnings if you use
# any Qt feature that has been marked deprecated (the exact warnings
# depend on your compiler). Refer to the documentation for the
# deprecated API to know how to port your code away from it.
DEFINES += QT_DEPRECATED_WARNINGS

# You can also make your code fail to compile if it uses deprecated APIs.
# In order to do so, uncomment the following line.
# You can also select to disable deprecated APIs only up to a certain version of Qt.
#DEFINES += QT_DISABLE_DEPRECATED_BEFORE=0x060000    # disables all the APIs deprecated before Qt 6.0.0

SOURCES += \
  src/main.cpp \
  src/entities/SyncManager.cpp \
  src/entities/LocalContacts.cpp \
  src/viewcontrollers/MainViewController.cpp

HEADERS += \
  src/entities/SyncManager.hpp \
  src/entities/LocalContacts.hpp \
  src/entities/Constants.hpp \
  src/viewcontrollers/MainViewController.hpp

RESOURCES += src/qml/qml.qrc

OTHER_FILES += .gitignore

android : {
    OTHER_FILES += \
      android-sources/AndroidManifest.xml \
      android-sources/src/org/mauikit/accounts/* \
      android-sources/src/org/mauikit/accounts/syncadapter/* \
      android-sources/src/org/mauikit/accounts/utils/* \
      android-sources/src/org/mauikit/accounts/dav/* \
      android-sources/src/org/mauikit/accounts/dav/dto/* \
      android-sources/src/org/mauikit/accounts/dav/utils/* \
      android-sources/res/drawable/* \
      android-sources/res/drawable-v24/* \
      android-sources/res/mipmap-anydpi-v26/* \
      android-sources/res/mipmap-hdpi/* \
      android-sources/res/mipmap-mdpi/* \
      android-sources/res/mipmap-xhdpi/* \
      android-sources/res/mipmap-xxhdpi/* \
      android-sources/res/mipmap-xxxhdpi/* \
      android-sources/res/values/* \
      android-sources/res/xml/* \
      android-sources/libs/*
}

# Additional import path used to resolve QML modules in Qt Creator's code model
QML_IMPORT_PATH =

# Additional import path used to resolve QML modules just for Qt Quick Designer
QML_DESIGNER_IMPORT_PATH =

# Default rules for deployment.
qnx: target.path = /tmp/$${TARGET}/bin
else: unix:!android: target.path = /opt/$${TARGET}/bin
!isEmpty(target.path): INSTALLS += target
