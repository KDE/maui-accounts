#include "entities/SyncManager.hpp"
#include "viewcontrollers/MainViewController.hpp"

#include <jni.h>
#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QtAndroidExtras/QAndroidJniObject>
#include <QtConcurrent>

const char* uri = "org.mauikit.accounts";
MainViewController* mainviewcontroller = nullptr;

extern "C" {
JNIEXPORT void JNICALL
Java_org_mauikit_accounts_syncadapter_ContactsSyncAdapter_performSync(
    JNIEnv* env, jobject obj, jstring username, jstring password, jstring url) {
  Q_UNUSED(env)
  Q_UNUSED(obj)

  qDebug() << "c++ performSync(): ";

  const char* username_str = env->GetStringUTFChars(username, 0);
  const char* password_str = env->GetStringUTFChars(password, 0);
  const char* url_str = env->GetStringUTFChars(url, 0);

  SyncManager* syncManager =
      new SyncManager(username_str, password_str, url_str);
  syncManager->doSync();
  syncManager->deleteLater();

  env->ReleaseStringUTFChars(username, username_str);
  env->ReleaseStringUTFChars(password, password_str);
  env->ReleaseStringUTFChars(url, url_str);
}
}

static QObject* mainviewcontroller_singleton_provider(QQmlEngine* engine,
                                                      QJSEngine* scriptEngine) {
  Q_UNUSED(engine)
  Q_UNUSED(scriptEngine)

  if (mainviewcontroller == nullptr) {
    mainviewcontroller = new MainViewController();
  }

  return mainviewcontroller;
}

int main(int argc, char* argv[]) {
  QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
  QLoggingCategory::setFilterRules("default.debug=true");

  QGuiApplication app(argc, argv);
  QQmlApplicationEngine engine;

  app.setApplicationName("Accounts");
  app.setApplicationVersion("0.1.3");
  app.setApplicationDisplayName("Accounts");

  qmlRegisterSingletonType<MainViewController>(
      uri, 1, 0, "MainViewController", mainviewcontroller_singleton_provider);

  engine.load(QUrl(QStringLiteral("qrc:/main.qml")));
  if (engine.rootObjects().isEmpty()) return -1;

  QAndroidJniObject::callStaticMethod<void>("org/mauikit/accounts/MainActivity",
                                            "init");

  return app.exec();
}
