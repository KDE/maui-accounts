import QtQuick 2.12
import QtQuick.Window 2.12
import QtQuick.Controls 2.5
import QtQuick.Layouts 1.3

import org.mauikit.accounts 1.0 as Accounts

ApplicationWindow {
    id: home
    visible: true
    header: ToolBar {
        id: menuBar
        width: parent.width
        height: 64

        Label {
            id: labelAppName
            text: stackPages.currentIndex == 0 ? "Accounts" : "Add Account"
            font.pointSize: 24
            verticalAlignment: Text.AlignVCenter
            anchors.left: parent.left
            anchors.leftMargin: 16
            anchors.verticalCenter: parent.verticalCenter
        }

        RoundButton {
            padding: 0
            flat: true
            icon.source: "icons/refresh.png"
            display: AbstractButton.IconOnly
            anchors.right: parent.right
            anchors.rightMargin: 16
            anchors.verticalCenter: parent.verticalCenter
            visible: stackPages.currentIndex == 0

            onClicked: {
                Accounts.MainViewController.getAccountList()
            }
        }
    }
    onClosing: {
        if (swipeView.currentIndex > 0) {
            close.accepted = false
            swipeView.setCurrentIndex(swipeView.currentIndex-1)
        } else if (stackPages.currentIndex > 0) {
            close.accepted = false
            stackPages.currentIndex--;
        }
    }

    ToastManager {
        id: toastManager
    }

    ListModel {
        id: listmodelAccounts
    }

    StackLayout {
        id: stackPages
        anchors.right: parent.right
        anchors.rightMargin: 0
        anchors.left: parent.left
        anchors.leftMargin: 0
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 0
        anchors.top: menuBar.bottom
        anchors.topMargin: 0
        currentIndex: 0

        Rectangle {
            Layout.fillHeight: true
            Layout.fillWidth: true

            Label {
                id: labelNoAccounts
                color: "#999999"
                text: qsTr("No Accounts Found. Add Account to get started.")
                anchors.horizontalCenter: parent.horizontalCenter
                anchors.top: parent.top
                anchors.topMargin: 24
                font.italic: true
                font.weight: Font.Light
                visible: listmodelAccounts.count <= 0
            }

            ScrollView {
                anchors.fill: parent

                ColumnLayout {
                    id: accountsList
                    width:parent.width

                    anchors.left: parent.left
                    anchors.leftMargin: 24
                    anchors.top: parent.top
                    anchors.topMargin: 24
                    anchors.right: parent.right
                    anchors.rightMargin: 24

                    spacing: 16

                    Item {
                        Layout.fillWidth: true
                        Layout.bottomMargin: 24
                        visible: listmodelAccounts.count > 0

                        Label {
                            id: labelCarddav
                            color: "#888888"
                            text: "CardDAV"

                            anchors.top: parent.top
                        }

                        Rectangle {
                            height: 2
                            color: "#cccccc"
                            radius:4
                            anchors.top: labelCarddav.bottom
                            anchors.left: parent.left
                            anchors.right: parent.right
                        }
                    }

                    Repeater {
                        model: listmodelAccounts

                        Item {
                            id: listmodelAccountsDelegate
                            Layout.fillWidth: true
                            height: 80

                            MouseArea {
                                anchors.fill: parent
                                onClicked: {
                                    Accounts.MainViewController.showUrl(listmodelAccounts.get(index).accountName)
                                }
                            }

                            Menu {
                                id: accountsMenu

                                MenuItem {
                                    text: "Sync Now"
                                    onClicked: {
                                        Accounts.MainViewController.syncAccount(listmodelAccounts.get(index).accountName)
                                    }
                                }

                                MenuItem {
                                    text: "Remove"
                                    onClicked: {
                                        Accounts.MainViewController.removeAccount(listmodelAccounts.get(index).accountName)
                                    }
                                }
                            }

                            Rectangle {
                                color: "#f8f8f8"
                                radius:4
                                anchors.fill: parent

                                ColumnLayout {
                                    anchors.right: parent.right
                                    anchors.rightMargin: 16
                                    anchors.left: parent.left
                                    anchors.leftMargin: 16
                                    anchors.verticalCenter: parent.verticalCenter

                                    Text {
                                        text: name
                                        font.pointSize: 14
                                        font.bold: true
                                    }
                                    Text {
                                        text: username
                                    }
                                }

                                RoundButton {
                                    flat: true
                                    icon.source: "icons/menu.png"
                                    display: AbstractButton.IconOnly
                                    anchors.right: parent.right
                                    anchors.rightMargin: 16
                                    anchors.verticalCenter: parent.verticalCenter

                                    onClicked: {
                                        accountsMenu.popup(parent)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            RoundButton {
                width: 64
                height: 64
                text: "+"
                anchors.bottom: parent.bottom
                anchors.bottomMargin: 16
                anchors.right: parent.right
                anchors.rightMargin: 16
                font.pointSize: 24
                font.bold: true
                onClicked: {
                    stackPages.currentIndex = 1
                }
            }
        }

        SwipeView {
            id: swipeView
            interactive: false
            Layout.fillWidth: true
            Layout.fillHeight: true
            currentIndex: 0

            Rectangle {
                Layout.fillWidth: true
                Layout.fillHeight: true

                ColumnLayout {
                    anchors.centerIn: parent
                    Button {
                        text: "OpenDesktop"
                        Layout.minimumWidth: 250
                        onClicked: {
                            inputServer.text = ""
                            inputServer.visible = false
                            buttonAddAccountOpendesktop.visible = true
                            buttonAddAccountCustom.visible = false

                            swipeView.setCurrentIndex(1)
                        }
                    }
                    Button {
                        text: "Custom Server"
                        Layout.minimumWidth: 250
                        onClicked: {
                            inputServer.visible = true
                            buttonAddAccountOpendesktop.visible = false
                            buttonAddAccountCustom.visible = true

                            swipeView.setCurrentIndex(1)
                        }
                    }
                }
            }

            Rectangle {
                id: rectangle
                Layout.fillWidth: true
                Layout.fillHeight: true

                ColumnLayout {
                    anchors.top: parent.top
                    anchors.topMargin: 16
                    anchors.left: parent.left
                    anchors.leftMargin: 16
                    anchors.right: parent.right
                    anchors.rightMargin: 16

                    ComboBox {
                        id: inputProtocol
                        Layout.fillWidth: true
                        textRole: "value"
                        visible: false

                        model: ListModel {
                            id: inputProtocolModel
                            ListElement {
                                key: "carddav"
                                value: "CardDAV"
                            }
                            ListElement {
                                key: "caldav"
                                value: "CalDAV"
                            }
                        }
                    }

                    TextField {
                        id: inputServer
                        Layout.fillWidth: true
                        placeholderText: "Server Address"
                    }

                    TextField {
                        id: inputUsername
                        Layout.fillWidth: true
                        placeholderText: "Username"
                    }

                    TextField {
                        id: inputPassword
                        Layout.fillWidth: true
                        placeholderText: "Password"
                        echoMode: TextInput.PasswordEchoOnEdit
                    }

                    Button {
                        id: buttonAddAccountOpendesktop
                        text: qsTr("Add Account")
                        Layout.minimumWidth: 250
                        Layout.alignment: Qt.AlignHCenter
                        onClicked: {
                            Accounts.MainViewController.addOpendesktopAccount(inputProtocolModel.get(inputProtocol.currentIndex).key, inputUsername.text, inputPassword.text)
                        }
                    }

                    Button {
                        id: buttonAddAccountCustom
                        text: qsTr("Add Account")
                        Layout.minimumWidth: 250
                        Layout.alignment: Qt.AlignHCenter
                        onClicked: {
                            Accounts.MainViewController.addCustomAccount(inputProtocolModel.get(inputProtocol.currentIndex).key, inputServer.text, inputUsername.text, inputPassword.text)
                        }
                    }
                }
            }
        }
    }

    Connections {
        target: Accounts.MainViewController
        onShowToast: {
            toastManager.show(message);
        }
        onAccountAdded: {
            stackPages.currentIndex = 0;
            swipeView.setCurrentIndex(0)

            inputServer.text = ""
            inputUsername.text = ""
            inputPassword.text = ""
        }

        onAccountList: {
            listmodelAccounts.clear()

            for (var i=0; i<accounts.length; i++) {
                var account = accounts[i].split(" - ")

                listmodelAccounts.append({
                    accountName: accounts[i],
                    name: account[1],
                    username: account[0]
                })
            }
        }
    }

    Component.onCompleted: {
        Accounts.MainViewController.getAccountList()
    }
}










































































































































































/*##^## Designer {
    D{i:0;autoSize:true;height:1280;width:720}
}
 ##^##*/
