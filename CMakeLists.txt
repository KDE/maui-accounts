project(maui-accounts LANGUAGES CXX)

cmake_minimum_required(VERSION 3.10)

set(CMAKE_INCLUDE_CURRENT_DIR ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(ECM 1.7.0 REQUIRED NO_MODULE)
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake" ${ECM_MODULE_PATH})

include(KDEInstallDirs)
include(KDECompilerSettings)
include(KDECMakeSettings)
include(FeatureSummary)

find_package(Qt5 REQUIRED NO_MODULE COMPONENTS Qml Concurrent Svg Quick Widgets)
include(FeatureSummary)

find_package(
    KF5 REQUIRED

    COMPONENTS
        Wallet
        Declarative
        ConfigWidgets
        CoreAddons
)

find_package(libdavclient REQUIRED)

set(CMAKE_INCLUDE_CURRENT_DIR ON)
set(CMAKE_AUTORCC ON)

if (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
    set(CMAKE_INSTALL_PREFIX /usr CACHE PATH "" FORCE)
endif()

set(
    ACCOUNTS_SRCS
    
    src/main.cpp
    src/entities/SyncManager.cpp
    src/entities/LocalContacts.cpp
    src/viewcontrollers/MainViewController.cpp
)

set(
    ACCOUNTS_ASSETS
    src/qml/contents/ui/qml.qrc
)

# add_executable(
#     ${PROJECT_NAME}
#     
#     ${ACCOUNTS_SRCS}
#     ${ACCOUNTS_ASSETS}
# )

add_library(
    maui_accounts MODULE

    ${ACCOUNTS_SRCS}
    ${ACCOUNTS_ASSETS}
)

target_link_libraries(
    maui_accounts
    
    Qt5::Qml
    Qt5::Quick
    Qt5::Widgets
    Qt5::Concurrent
    Qt5::Svg
    
    KF5::ConfigWidgets
    KF5::Declarative
    KF5::Wallet
    KF5::CoreAddons

    libdavclient.so
)

kcoreaddons_desktop_to_json(maui_accounts src/maui_accounts.desktop SERVICE_TYPES kcmodule.desktop)

install(FILES src/maui_accounts.desktop DESTINATION ${SERVICES_INSTALL_DIR})
install(TARGETS maui_accounts DESTINATION ${PLUGIN_INSTALL_DIR}/kcms)
kpackage_install_package(src/qml maui_accounts kcms)



# CPack Configuration
include(InstallRequiredSystemLibraries)

set(CPACK_GENERATOR "DEB")
set(CPACK_DEBIAN_PACKAGE_SHLIBDEPS ON)
set(CPACK_DEBIAN_PACKAGE_MAINTAINER "Anupam Basak")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "MAUI Accounts")
set(CPACK_PACKAGE_VENDOR "Anupam Basak")
set(CPACK_PACKAGE_CONTACT "Anupam Basak <anupam.basak27@gmail.com>")
#set(CPACK_PACKAGE_DESCRIPTION_FILE "${CMAKE_CURRENT_SOURCE_DIR}/ReadMe.txt")
#set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/Copyright.txt")
set(CPACK_PACKAGE_VERSION_MAJOR ${PROJECT_VERSION_MAJOR})
set(CPACK_PACKAGE_VERSION_MINOR ${PROJECT_VERSION_MINOR})
set(CPACK_PACKAGE_VERSION_PATCH ${PROJECT_VERSION_PATCH})
set(CPACK_PACKAGE_INSTALL_DIRECTORY "CMake ${CMake_VERSION_MAJOR}.${CMake_VERSION_MINOR}")
set(CPACK_SOURCE_STRIP_FILES "")

include(CPack)
###

feature_summary(WHAT ALL FATAL_ON_MISSING_REQUIRED_PACKAGES)
