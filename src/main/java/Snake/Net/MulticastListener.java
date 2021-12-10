package Snake.Net;

import com.google.protobuf.InvalidProtocolBufferException;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.LinkedList;


public class MulticastListener implements Runnable{
    LinkedList<NetModel.AnnouncementWithTime> announcementWithTimeLinkedList;
    JList<NetModel.AnnouncementWithTime> list;
    NetModel netModel;
    public MulticastListener(LinkedList<NetModel.AnnouncementWithTime> announcementWithTimeLinkedList, JList<NetModel.AnnouncementWithTime> list, NetModel netModel) {
        this.announcementWithTimeLinkedList = announcementWithTimeLinkedList;
        this.list = list;
        this.netModel = netModel;
    }
    @Override
    public void run() {
        while (true) {
            MulticastSocket socket = null;
            try {
                socket = new MulticastSocket(NetModel.PORT);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            byte[] buf = new byte[64 * 1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            try {
                netModel.processMsg(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
