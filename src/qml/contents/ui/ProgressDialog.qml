import QtQuick 2.12
import QtQuick.Controls 2.5

Rectangle {
    id: rectangle

    Label {
        id: title
        text: "Title"
        font.pointSize: 14
        anchors.top: parent.top
        anchors.topMargin: 16
        anchors.left: parent.left
        anchors.leftMargin: 16
    }
    Rectangle {
        id: rectangle1
        anchors.right: parent.right
        anchors.rightMargin: 16
        anchors.left: parent.left
        anchors.leftMargin: 16
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 16
        anchors.top: title.bottom
        anchors.topMargin: 16

        BusyIndicator {
            id: busyIndicator
            anchors.verticalCenterOffset: 0
            anchors.verticalCenter: parent.verticalCenter
        }

        Label {
            id: label
            text: qsTr("Some text here")
            anchors.verticalCenter: parent.verticalCenter
            anchors.left: busyIndicator.right
            anchors.leftMargin: 16
        }

    }
}



/*##^## Designer {
    D{i:0;autoSize:true;height:300;width:600}
}
 ##^##*/
