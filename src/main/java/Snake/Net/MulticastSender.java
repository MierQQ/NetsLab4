package Snake.Net;

import Protobuf.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class MulticastSender implements Runnable {

    private final MulticastSocket out;
    private final SocketAddress address;
    private final NetModel netModel;

    public MulticastSender(MulticastSocket out, SocketAddress address, NetModel netModel) throws UnknownHostException {
        this.out = out;
        this.address = address;
        this.netModel = netModel;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                var msg = netModel.getAnnouncement();
                var toSend = SnakesProto.GameMessage.newBuilder().setAnnouncement(msg).setMsgSeq(netModel.getSeq()).build();
                var buf = toSend.toByteArray();
                out.send(new DatagramPacket(buf, buf.length, address));
                netModel.sendPing();
                Thread.sleep(1000);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }
}