import QtQuick 2.12
//import QtQuick.Window 2.12
import QtQuick.Controls 2.5
//import QtQuick.Layouts 1.3

//import maui_accounts 1.0 as Accounts

Rectangle {
    color: "#2196f3"
    anchors.fill: parent
}

//ApplicationWindow {
//    id: home
//    width: 1280
//    height: 720
//    visible: true
//    header: ToolBar {
//        id: menuBar
//        width: parent.width
//        height: Qt.platform.os == "Android" ? 64 : 48

//        Label {
//            id: labelAppName
//            text: "Accounts"
//            font.pointSize: Qt.platform.os == "Android" ? 24 : 12
//            verticalAlignment: Text.AlignVCenter
//            anchors.left: parent.left
//            anchors.leftMargin: 16
//            anchors.verticalCenter: parent.verticalCenter
//        }

//        RoundButton {
//            padding: 0
//            flat: true
//            icon.source: "icons/refresh.png"
//            display: AbstractButton.IconOnly
//            anchors.right: parent.right
//            anchors.rightMargin: 16
//            anchors.verticalCenter: parent.verticalCenter
//            visible: stackPages.currentIndex == 0

//            onClicked: {
//                Accounts.MainViewController.getAccountList()
//            }
//        }
//    }
//    onClosing: {
//        if (Qt.platform.os == "Android") {
//            backPressed()
//        } else {
//            close.accepted = true
//        }
//    }

//    Popup {
//        id: progressDialog
//        anchors.centerIn: parent
//        width: 600
//        height: 100
//        modal: true
//        focus: true
//        closePolicy: Popup.NoAutoClose

//        Rectangle {
//            anchors.right: parent.right
//            anchors.rightMargin: 20
//            anchors.verticalCenter: parent.verticalCenter
//            anchors.leftMargin: 20
//            anchors.left: parent.left

//            AnimatedImage {
//                id: spinner
//                height: 50
//                anchors.verticalCenter: parent.verticalCenter
//                fillMode: Image.PreserveAspectFit
//                source: "icons/spinner.gif"
//            }

//            Label {
//                id: progressDialogText
//                anchors.left: spinner.right
//                anchors.leftMargin: 20
//                anchors.verticalCenter: parent.verticalCenter
//            }
//        }
//    }

//    ListModel {
//        id: listmodelAccounts

//        ListElement {
//            accountName: "anupam - Opendesktop"
//            name: "Opendesktop"
//            username: "ab0027"
//        }
//    }

//    RoundButton {
//        width: Qt.platform.os == "Android" ? 64 : 48
//        height: Qt.platform.os == "Android" ? 64 : 48
//        icon.source: "icons/back.png"
//        z: 100
//        anchors.bottom: parent.bottom
//        anchors.bottomMargin: 16
//        anchors.right: parent.right
//        anchors.rightMargin: 16
//        visible: stackPages.currentIndex > 0
//        onClicked: {
//            backPressed()
//        }
//    }

//    StackLayout {
//        id: stackPages
//        anchors.right: parent.right
//        anchors.rightMargin: 0
//        anchors.left: parent.left
//        anchors.leftMargin: 0
//        anchors.bottom: parent.bottom
//        anchors.bottomMargin: 0
//        anchors.top: menuBar.bottom
//        anchors.topMargin: 0

//        Rectangle {
//            Layout.fillHeight: true
//            Layout.fillWidth: true

//            Label {
//                id: labelNoAccounts
//                color: "#999999"
//                text: qsTr("No Accounts Found. Add Account to get started.")
//                anchors.horizontalCenter: parent.horizontalCenter
//                anchors.top: parent.top
//                anchors.topMargin: 24
//                font.italic: true
//                font.weight: Font.Light
//                visible: listmodelAccounts.count <= 0
//            }

//            ScrollView {
//                anchors.fill: parent

//                ColumnLayout {
//                    id: accountsList
//                    width:parent.width

//                    anchors.left: parent.left
//                    anchors.leftMargin: 24
//                    anchors.top: parent.top
//                    anchors.topMargin: 24
//                    anchors.right: parent.right
//                    anchors.rightMargin: 24

//                    spacing: 16

//                    Item {
//                        Layout.fillWidth: true
//                        Layout.bottomMargin: 24
//                        visible: listmodelAccounts.count > 0

//                        Label {
//                            id: labelCarddav
//                            color: "#888888"
//                            text: "CardDAV"

//                            anchors.top: parent.top
//                        }

//                        Rectangle {
//                            height: 2
//                            color: "#cccccc"
//                            radius:4
//                            anchors.top: labelCarddav.bottom
//                            anchors.left: parent.left
//                            anchors.right: parent.right
//                        }
//                    }

//                    Repeater {
//                        model: listmodelAccounts

//                        Item {
//                            id: listmodelAccountsDelegate
//                            Layout.fillWidth: true
//                            height: 80

//                            MouseArea {
//                                anchors.fill: parent
//                                onClicked: {
//                                    Accounts.MainViewController.showUrl(listmodelAccounts.get(index).accountName)
//                                }
//                            }

//                            Menu {
//                                id: accountsMenu

//                                MenuItem {
//                                    text: "Sync Now"
//                                    onClicked: {
//                                        Accounts.MainViewController.syncAccount(listmodelAccounts.get(index).accountName)
//                                    }
//                                }

//                                MenuItem {
//                                    text: "Remove"
//                                    onClicked: {
//                                        Accounts.MainViewController.removeAccount(listmodelAccounts.get(index).accountName)
//                                    }
//                                }
//                            }

//                            Rectangle {
//                                color: "#f8f8f8"
//                                radius:4
//                                anchors.fill: parent

//                                ColumnLayout {
//                                    anchors.right: parent.right
//                                    anchors.rightMargin: 16
//                                    anchors.left: parent.left
//                                    anchors.leftMargin: 16
//                                    anchors.verticalCenter: parent.verticalCenter

//                                    Text {
//                                        text: name
//                                        font.pointSize: Qt.platform.os == "Android" ? 14 : 10
//                                        font.bold: true
//                                    }
//                                    Text {
//                                        text: username
//                                    }
//                                }

//                                RoundButton {
//                                    flat: true
//                                    icon.source: "icons/menu.png"
//                                    display: AbstractButton.IconOnly
//                                    anchors.right: parent.right
//                                    anchors.rightMargin: 16
//                                    anchors.verticalCenter: parent.verticalCenter

//                                    onClicked: {
//                                        accountsMenu.popup(parent)
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }

//            RoundButton {
//                width: Qt.platform.os == "Android" ? 64 : 48
//                height: Qt.platform.os == "Android" ? 64 : 48
//                text: "+"
//                anchors.bottom: parent.bottom
//                anchors.bottomMargin: 16
//                anchors.right: parent.right
//                anchors.rightMargin: 16
//                font.pointSize: Qt.platform.os == "Android" ? 24 : 14
//                font.bold: true
//                onClicked: {
//                    stackPages.currentIndex = 1
//                }
//            }
//        }

//        SwipeView {
//            id: swipeView
//            interactive: false
//            Layout.fillWidth: true
//            Layout.fillHeight: true
//            currentIndex: 0

//            Rectangle {
//                Layout.fillWidth: true
//                Layout.fillHeight: true

//                ColumnLayout {
//                    anchors.centerIn: parent
//                    Button {
//                        text: "OpenDesktop"
//                        Layout.minimumWidth: 250
//                        onClicked: {
//                            inputServer.text = ""
//                            inputServer.visible = false
//                            buttonAddAccountOpendesktop.visible = true
//                            buttonAddAccountCustom.visible = false

//                            swipeView.setCurrentIndex(1)
//                        }
//                    }
//                    Button {
//                        text: "Custom Server"
//                        Layout.minimumWidth: 250
//                        onClicked: {
//                            inputServer.visible = true
//                            buttonAddAccountOpendesktop.visible = false
//                            buttonAddAccountCustom.visible = true

//                            swipeView.setCurrentIndex(1)
//                        }
//                    }
//                }
//            }

//            Rectangle {
//                id: rectangle
//                Layout.fillWidth: true
//                Layout.fillHeight: true

//                ColumnLayout {
//                    anchors.top: parent.top
//                    anchors.topMargin: 16
//                    anchors.left: parent.left
//                    anchors.leftMargin: 16
//                    anchors.right: parent.right
//                    anchors.rightMargin: 16

//                    ComboBox {
//                        id: inputProtocol
//                        Layout.fillWidth: true
//                        textRole: "value"
//                        visible: false

//                        model: ListModel {
//                            id: inputProtocolModel
//                            ListElement {
//                                key: "carddav"
//                                value: "CardDAV"
//                            }
//                            ListElement {
//                                key: "caldav"
//                                value: "CalDAV"
//                            }
//                        }
//                    }

//                    TextField {
//                        id: inputServer
//                        Layout.fillWidth: true
//                        placeholderText: "Server Address"
//                    }

//                    TextField {
//                        id: inputUsername
//                        Layout.fillWidth: true
//                        placeholderText: "Username"
//                    }

//                    TextField {
//                        id: inputPassword
//                        Layout.fillWidth: true
//                        placeholderText: "Password"
//                        echoMode: TextInput.PasswordEchoOnEdit
//                    }

//                    Button {
//                        id: buttonAddAccountOpendesktop
//                        text: qsTr("Add Account")
//                        Layout.minimumWidth: 250
//                        Layout.alignment: Qt.AlignHCenter
//                        onClicked: {
//                            Accounts.MainViewController.addOpendesktopAccount(inputProtocolModel.get(inputProtocol.currentIndex).key, inputUsername.text, inputPassword.text)
//                        }
//                    }

//                    Button {
//                        id: buttonAddAccountCustom
//                        text: qsTr("Add Account")
//                        Layout.minimumWidth: 250
//                        Layout.alignment: Qt.AlignHCenter
//                        onClicked: {
//                            Accounts.MainViewController.addCustomAccount(inputProtocolModel.get(inputProtocol.currentIndex).key, inputServer.text, inputUsername.text, inputPassword.text)
//                        }
//                    }
//                }
//            }
//        }
//    }

//    Connections {
//        target: Accounts.MainViewController

//        onAccountAdded: {
//            stackPages.currentIndex = 0;
//            swipeView.setCurrentIndex(0)

//            inputServer.text = ""
//            inputUsername.text = ""
//            inputPassword.text = ""
//        }

//        onAccountList: {
//            listmodelAccounts.clear()

//            for (var i=0; i<accounts.length; i++) {
//                var account = accounts[i].split(" - ")

//                listmodelAccounts.append({
//                    accountName: accounts[i],
//                    name: account[1],
//                    username: account[0]
//                })
//            }
//        }

//        onShowIndefiniteProgress: {
//            progressDialogText.text = message
//            progressDialog.open()
//        }

//        onHideIndefiniteProgress: {
//            progressDialogText.text = ""
//            progressDialog.close()
//        }
//    }

//    Component.onCompleted: {
//        Accounts.MainViewController.getAccountList()
//    }

//    function backPressed() {
//        if (swipeView.currentIndex > 0) {
//            close.accepted = false
//            swipeView.setCurrentIndex(swipeView.currentIndex-1)
//        } else if (stackPages.currentIndex > 0) {
//            close.accepted = false
//            stackPages.currentIndex--;
//        }
//    }
//}












































































































































































/*##^## Designer {
    D{i:0;autoSize:true;height:480;width:640}
}
 ##^##*/
