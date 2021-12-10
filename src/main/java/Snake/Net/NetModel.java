package Snake.Net;

import Snake.Logic.Cell;
import Snake.Logic.GameState;
import Snake.Logic.Model;
import Snake.Logic.Player;

import Protobuf.SnakesProto;
import com.google.protobuf.InvalidProtocolBufferException;

import javax.swing.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.Enumeration;
import java.util.LinkedList;

public class NetModel{
    static final int PORT = 9291;
    static final String ADDRESS = "239.192.0.4";
    private int nodeTimeOut;
    private int portOfOwnerGame;
    private InetAddress addressOfOwnerGame;
    private long lastMessageFromHost;
    private long lastMessageFromMe;


    public boolean isMaster() {
        for (var it: playerInfoLinkedList) {
            if (it.id == getId() && it.playerProtobuf.getRole() == SnakesProto.NodeRole.DEPUTY) {
                if (nodeTimeOut > System.currentTimeMillis() - lastMessageFromHost) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getSeq() {
        return model.getSeq();
    }

    public void setNodeTimeOut(int nodeTimeOut) {
        this.nodeTimeOut = nodeTimeOut;
    }

    public void sendPing() {
        if (!isOwner && System.currentTimeMillis() - lastMessageFromMe > 1000) {
            var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
            var buf = toSend.toByteArray();
            var pack = new DatagramPacket(buf, buf.length, addressOfOwnerGame, portOfOwnerGame);
            try {
                datagramSocket.send(pack);
                lastMessageFromMe = System.currentTimeMillis();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void sendChangeRole() throws IOException {
        PlayerInfo sendTo = null;
        for(var it : playerInfoLinkedList) {
            if (it.playerProtobuf.getRole() != SnakesProto.NodeRole.VIEWER) {
                sendTo = it;
                it.playerProtobuf = SnakesProto.GamePlayer.newBuilder(it.playerProtobuf).setRole(SnakesProto.NodeRole.DEPUTY).build();
                break;
            }
        }
        if (sendTo == null) {
            return;
        }
        var msg = SnakesProto.GameMessage.newBuilder()
                .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setReceiverRole(SnakesProto.NodeRole.DEPUTY).setSenderRole(SnakesProto.NodeRole.MASTER).build())
                .setMsgSeq(model.getSeq())
                .setSenderId(getId())
                .build();
        var buf = msg.toByteArray();
        var pack = new DatagramPacket(buf, buf.length, InetAddress.getByName(sendTo.playerProtobuf.getIpAddress()), sendTo.playerProtobuf.getPort());
        datagramSocket.send(pack);
    }

    public static class AnnouncementWithTime {
        public SnakesProto.GameMessage.AnnouncementMsg announcementMsg;
        public long time;
        public DatagramPacket received;
        public SnakesProto.GamePlayer master;

        public AnnouncementWithTime(SnakesProto.GameMessage.AnnouncementMsg announcementMsg, long time, DatagramPacket packet, SnakesProto.GamePlayer master) {
            this.announcementMsg = announcementMsg;
            this.time = time;
            this.received = packet;
            this.master = master;
        }


        @Override
        public String toString() {
            return received.getAddress().toString() + " " + announcementMsg.getPlayers().getPlayersList().size();//announcementMsg.getPlayers().toString();
        }
    }
    private boolean isOwner;
    private MulticastSocket multicastSocket;
    private DatagramSocket datagramSocket;
    private JList<AnnouncementWithTime> list;
    private LinkedList<AnnouncementWithTime> announcementWithTimeLinkedList = new LinkedList<AnnouncementWithTime>();
    private InetAddress group;

    public LinkedList<PlayerInfo> getPlayerInfoLinkedList() {
        return playerInfoLinkedList;
    }

    private LinkedList<PlayerInfo> playerInfoLinkedList = new LinkedList<>();
    private Model model;


    private PlayerInfo deputy = null;

    public static class PlayerInfo {
        int id;
        Player player;
        SnakesProto.GamePlayer playerProtobuf;
        long lastTime;

        public PlayerInfo(Player player) {
            this.player = player;
            id = player.id;
            playerProtobuf = SnakesProto.GamePlayer.newBuilder()
                    .setScore(player.score)
                    .setId(id)
                    .setRole(SnakesProto.NodeRole.MASTER)
                    .setName(player.name)
                    .setPort(PORT)
                    .setType(SnakesProto.PlayerType.HUMAN)
                    .setIpAddress("")
                    .build();
        }
        public PlayerInfo(Player player, String ip, SnakesProto.NodeRole role) {
            this.player = player;
            id = player.id;
            lastTime = System.currentTimeMillis();
            playerProtobuf = SnakesProto.GamePlayer.newBuilder()
                    .setScore(player.score)
                    .setId(id)
                    .setRole(role)
                    .setName(player.name)
                    .setPort(PORT)
                    .setType(SnakesProto.PlayerType.HUMAN)
                    .setIpAddress(ip)
                    .build();
        }
    }

    private static int id = -1;

    public static int getId() {
        if (id == -1) {
            try {
                return (Inet4Address.getLocalHost().getHostAddress() + ManagementFactory.getRuntimeMXBean().getName()).hashCode();
            } catch (UnknownHostException e) {
                return -1;
            }
        }
        return id;
    }

    public NetModel(Model model, boolean isOwner) {
        try {
            this.isOwner = isOwner;
            this.model = model;
            this.list = model.getGamesList();
            datagramSocket = new DatagramSocket();
            multicastSocket = new MulticastSocket(PORT);
            group = InetAddress.getByName(ADDRESS);
            multicastSocket.joinGroup(group);

            new Thread(new MulticastListener(announcementWithTimeLinkedList, list, this)).start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void startSender() {
        try {
            announceThread = new Thread(new MulticastSender(multicastSocket, new InetSocketAddress(group, PORT), this));
            announceThread.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private SnakesProto.GameState.Coord coord(int x, int y) {
        return SnakesProto.GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    public SnakesProto.Direction getDir(Player.Direction dir) {
        switch (dir) {
            case UP: return SnakesProto.Direction.UP;
            case DOWN: return SnakesProto.Direction.DOWN;
            case LEFT: return SnakesProto.Direction.LEFT;
            case RIGHT: return SnakesProto.Direction.RIGHT;
        }
        return null;
    }

    public Player.Direction getDirModel(SnakesProto.Direction dir) {
        switch (dir) {
            case UP: return Player.Direction.UP;
            case DOWN: return Player.Direction.DOWN;
            case LEFT: return Player.Direction.LEFT;
            case RIGHT: return Player.Direction.RIGHT;
        }
        return null;
    }

    public void processMsg(DatagramPacket packet) throws IOException {
        var msg = SnakesProto.GameMessage.parseFrom(packet.getData());
        switch (msg.getTypeCase()) {
            case ANNOUNCEMENT: {
                var announce = msg.getAnnouncement();
                SnakesProto.GamePlayer master = null;
                for (var it: announce.getPlayers().getPlayersList()) {
                    if (it.getRole() == SnakesProto.NodeRole.MASTER) {
                        master = it;
                    }
                }
                if (master == null) {
                    return;
                }
                SnakesProto.GamePlayer finalMaster = master;
                announcementWithTimeLinkedList.removeIf(it -> it.master == finalMaster);
                announcementWithTimeLinkedList.add(new NetModel.AnnouncementWithTime(announce, System.currentTimeMillis(), packet, master));
                long currentTime = System.currentTimeMillis();
                announcementWithTimeLinkedList.removeIf(it -> currentTime - it.time > 500);
                list.setListData((NetModel.AnnouncementWithTime[]) announcementWithTimeLinkedList.toArray());
                return;
            }
            case STATE: {
                var state = msg.getState();
                model.setSeq(msg.getMsgSeq());
                LinkedList<PlayerInfo> newPlayers = new LinkedList<>();
                synchronized (this) {
                    this.state = new GameState(state.getState().getConfig().getWidth(), state.getState().getConfig().getHeight());
                    for (var snake: state.getState().getPlayers().getPlayersList()) {
                        SnakesProto.GameState.Snake sn = null;
                        for (var sna: state.getState().getSnakesList()) {
                            if (sna.getPlayerId() == snake.getId()) {
                                sn = sna;
                            }
                        }
                        if (sn == null) {
                            continue;
                        }
                        var s = new Player(sn, model, snake.getName());
                        this.state.getPlayers().add(s);
                        newPlayers.add(new PlayerInfo(s, snake.getIpAddress(), snake.getRole()));
                        playerInfoLinkedList = newPlayers;
                    }
                    for (var food: state.getState().getFoodsList()) {
                        this.state.getField().setCell(food.getX(), food.getY(), Cell.CellType.Food);
                    }
                }
                var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
                var buf = toSend.toByteArray();
                var pack = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                try {
                    datagramSocket.send(pack);
                    lastMessageFromMe = System.currentTimeMillis();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                return;
            }
            case JOIN: {
                if (isOwner) {
                    var join = msg.getJoin();
                    int id = msg.hasSenderId() ? msg.getSenderId() : Integer.hashCode(packet.getPort() + packet.getAddress().hashCode());
                    var player = model.spawnPlayer(id , join.getName());
                    if (player == null) {
                        return;
                    }
                    if (deputy != null) {
                        playerInfoLinkedList.add(new PlayerInfo(player, packet.getAddress().toString(), join.getOnlyView()? SnakesProto.NodeRole.VIEWER : SnakesProto.NodeRole.NORMAL));
                    } else {
                        playerInfoLinkedList.add(new PlayerInfo(player, packet.getAddress().toString(), SnakesProto.NodeRole.DEPUTY));
                    }
                    if (!join.getOnlyView()) {
                        this.state.getPlayers().add(player);
                    }
                    var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
                    var buf = toSend.toByteArray();
                    var pack = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                    datagramSocket.send(pack);
                }
                return;
            }
            case ACK: {
                for (var it: playerInfoLinkedList) {
                    if (it.id == msg.getSenderId()) {
                        it.lastTime = System.currentTimeMillis();
                    }
                }
                if (isJoin) {
                    id = msg.hasReceiverId() ? msg.getReceiverId() : id;
                    isJoin = false;
                }
                return;
            }
            case PING: {
                if (isOwner) {
                    for (var it : playerInfoLinkedList) {
                        if (it.id == msg.getSenderId()) {
                            it.lastTime = System.currentTimeMillis();
                        }
                    }
                } else {
                    lastMessageFromHost = System.currentTimeMillis();
                }
                var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
                var buf = toSend.toByteArray();
                var pack = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());try {
                    datagramSocket.send(pack);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                return;
            }
            case ERROR: {
                var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
                var buf = toSend.toByteArray();
                var pack = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                try {
                    datagramSocket.send(pack);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                System.exit(-3);
                return;
            }
            case STEER: {
                int id = msg.getSenderId();
                Player.Direction dir = getDirModel( msg.getSteer().getDirection());
                model.SetPlayerInput(id, dir);
                var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
                var buf = toSend.toByteArray();
                var pack = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                try {
                    datagramSocket.send(pack);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                return;
            }
            case ROLE_CHANGE: {
                var myRole = msg.getRoleChange().getReceiverRole();
                var senderId = msg.getSenderId();
                var senderRole = msg.getRoleChange().getSenderRole();
                if (senderRole == SnakesProto.NodeRole.MASTER) {
                    addressOfOwnerGame = packet.getAddress();
                    portOfOwnerGame = packet.getPort();
                }
                for (var it: playerInfoLinkedList) {
                    if (it.id == getId()) {
                        it.playerProtobuf = SnakesProto.GamePlayer.newBuilder(it.playerProtobuf).setRole(myRole).build();
                    }
                    if (it.id == senderId) {
                        it.playerProtobuf = SnakesProto.GamePlayer.newBuilder(it.playerProtobuf).setRole(senderRole).build();
                    }
                }
                var toSend= SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setAck(SnakesProto.GameMessage.AckMsg.newBuilder().getDefaultInstanceForType()).setMsgSeq(model.getSeq()).build();
                var buf = toSend.toByteArray();
                var pack = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                try {
                    datagramSocket.send(pack);
                    lastMessageFromMe = System.currentTimeMillis();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                return;
            }
            case TYPE_NOT_SET: {
                return;
            }
        }
    }

    public void sendState() {
        var state = model.getState();
        SnakesProto.GameConfig config = SnakesProto.GameConfig.newBuilder()
                .setWidth(state.getField().getNumberOfColumns())
                .setHeight(state.getField().getNumberOfLines()).build();
        var players = SnakesProto.GamePlayers.newBuilder();
        for (var it : playerInfoLinkedList) {
            players = players.addPlayers(it.playerProtobuf);
        }

        var stateToSend = SnakesProto.GameState.newBuilder()
                .setStateOrder((int)model.getSeq())
                .setPlayers(players)
                .setConfig(config);

        for (var it : state.getPlayers()) {
            SnakesProto.GameState.Snake.SnakeState snakeState = SnakesProto.GameState.Snake.SnakeState.ALIVE;
            long currentTime = System.currentTimeMillis();
            for (var playerInfo: playerInfoLinkedList) {
                if (it == playerInfo.player && currentTime - playerInfo.lastTime > nodeTimeOut) {
                    snakeState = SnakesProto.GameState.Snake.SnakeState.ZOMBIE;
                }
            }

            var snake = SnakesProto.GameState.Snake.newBuilder()
                    .setPlayerId(it.id)
                    .setHeadDirection(getDir(it.lastDir))
                    .setState(snakeState);
            for (var body : it.getBody()) {
                snake = snake.addPoints(coord(body.x, body.y));
            }
            stateToSend = stateToSend.addSnakes(snake.build());
        }

        for (int x = 0; x < state.getField().getNumberOfColumns(); ++x) {
            for (int y = 0; y < state.getField().getNumberOfLines(); ++y) {
                if (state.getField().getCell(x, y) == Cell.CellType.Food) {
                    stateToSend = stateToSend.addFoods(coord(x, y));
                }
            }
        }

        SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder()
                .setState(stateToSend.build())
                .build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(model.getSeq())
                .setState(stateMsg)
                .build();
        var buf = gameMessage.toByteArray();
        for (var player: playerInfoLinkedList) {
            try {
                if (player.id != getId()) {
                    var packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(player.playerProtobuf.getIpAddress()), PORT);
                    datagramSocket.send(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public void sendInput(Player.Direction direction) {
        var steer = SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(getDir(direction)).build();
        var msg = SnakesProto.GameMessage.newBuilder().setSenderId(getId()).setMsgSeq(model.getSeq()).setSteer(steer).build();
        var buff = msg.toByteArray();
        var packet = new DatagramPacket(buff, buff.length, addressOfOwnerGame, portOfOwnerGame);
        try {
            datagramSocket.send(packet);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private GameState state = new GameState(10, 10);

    public GameState getState() {
        synchronized (this) {
            return state;
        }
    }

    public SnakesProto.GamePlayers getGamePlayers() {
        var gamePlayers = SnakesProto.GamePlayers.newBuilder();
        for (var it : playerInfoLinkedList) {
            gamePlayers = gamePlayers.addPlayers(SnakesProto.GamePlayer.newBuilder(it.playerProtobuf).setScore(it.player.score).build());
        }
        return gamePlayers.build();
    }

    public SnakesProto.GameMessage.AnnouncementMsg getAnnouncement() {
        var point = model.getState().getField().getEmptyBox();
        var config = model.getConfig();
        var gamePlayers = getGamePlayers();
        boolean canJoin = true;
        if (point.x == -1 && point.y == -1) {
            canJoin = false;
        }
        var gmBuilder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                .setCanJoin(canJoin)
                .setConfig(config)
                .setPlayers(getGamePlayers()).build();
        return gmBuilder;
    }

    public Thread announceThread;
    private boolean isJoin;

    public void connect(DatagramPacket packet) throws IOException {
        isOwner = false;
        isJoin = true;
        addressOfOwnerGame = packet.getAddress();
        portOfOwnerGame = packet.getPort();
        announceThread.interrupt();
        var msg = SnakesProto.GameMessage.newBuilder()
                .setJoin(SnakesProto.GameMessage.JoinMsg.newBuilder().setName(model.getName()).build())
                .setSenderId(getId())
                .setMsgSeq(0)
                .build();
        var buf = msg.toByteArray();
        var pack = new DatagramPacket(buf, buf.length, addressOfOwnerGame, portOfOwnerGame);
        datagramSocket.send(pack);
    }
}
