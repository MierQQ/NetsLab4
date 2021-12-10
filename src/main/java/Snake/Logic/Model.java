package Snake.Logic;


import Snake.Net.NetModel;
import Protobuf.SnakesProto;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;

public class Model implements Runnable, ActionListener, KeyListener {
    private Random rng = new Random();
    private GameState state;
    private Thread thread;
    private float foodSpawnChanceAfterDeath;
    private int food = 0;
    private int foodCap;
    private int startFood;
    private int fieldXSize;
    private int fieldYSize;
    private float foodPerPlayer;
    private int sleepTime;

    public String getName() {
        return name;
    }

    private String name;
    private boolean isOwner = true;
    private NetModel netModel;

    private JFrame frame;
    private JPanel canvas;
    private JTextArea rating;
    private JTextArea gameInfo;
    private JList<NetModel.AnnouncementWithTime> gamesList;
    private JButton exitButton;
    private JButton newGameButton;
    private JButton connectToGame;
    private Player currentPlayer;

    public long seq;


    public void foodEaten() {
        --food;
    }

    public SnakesProto.GameConfig getConfig() {
        return SnakesProto.GameConfig.newBuilder()
                .setWidth(fieldXSize)
                .setHeight(fieldYSize)
                .setFoodStatic(startFood)
                .setFoodPerPlayer(foodPerPlayer)
                .setStateDelayMs(sleepTime)
                .setDeadFoodProb(foodSpawnChanceAfterDeath)
                .build();
    }

    public Model(JFrame frame, JPanel canvas, JTextArea rating, JTextArea gameInfo, JList<NetModel.AnnouncementWithTime> gamesList, JButton exitButton, JButton newGameButton, JButton connectToGame) throws IOException {

        Properties prop = new Properties();
        prop.load(ClassLoader.getSystemResourceAsStream("property.config"));
        startFood = Integer.parseInt(prop.getProperty("startFood"));
        fieldXSize = Integer.parseInt(prop.getProperty("fieldXSize"));
        fieldYSize = Integer.parseInt(prop.getProperty("fieldYSize"));
        foodSpawnChanceAfterDeath = Float.parseFloat(prop.getProperty("foodSpawnChanceAfterDeath"));
        foodPerPlayer = Float.parseFloat(prop.getProperty("foodPerPlayer"));
        sleepTime = Integer.parseInt(prop.getProperty("sleepTime"));
        name = prop.getProperty("name");
        this.state = new GameState(fieldXSize, fieldYSize);
        this.connectToGame = connectToGame;
        this.frame = frame;
        this.canvas = canvas;
        this.rating = rating;
        this.gameInfo = gameInfo;
        this.gamesList = gamesList;
        this.exitButton = exitButton;
        this.newGameButton = newGameButton;
        frame.addKeyListener(this);
        exitButton.addActionListener(this);
        newGameButton.addActionListener(this);
        connectToGame.addActionListener(this);
        netModel = new NetModel(this, isOwner);
    }

    public GameState getState() {
        return state;
    }

    public Player spawnPlayer(int id, String name) {
        var spawnPoint = state.getField().getEmptyBox();
        if (spawnPoint.x == -1 && spawnPoint.y == -1) {
            return null;
        }
        var newPlayer = new Player(state.getField(), spawnPoint.x,spawnPoint.y, id, name, this);
        state.addPlayer(newPlayer);
        return newPlayer;
    }

    public void spawnFood() {
        LinkedList<Point> emptyList = new LinkedList<>();
        for (int i = 0; i < state.getField().getNumberOfColumns(); ++i) {
            for (int j = 0; j < state.getField().getNumberOfLines(); ++j) {
                if (state.getField().getCell(i, j) == Cell.CellType.Empty) {
                    emptyList.add(new Point(i, j));
                }
            }
        }
        if (emptyList.size() != 0 && food < foodCap) {
            state.getField().setCell(emptyList.get(rng.nextInt(emptyList.size())), Cell.CellType.Food);
            food++;
        }
    }

    public JList<NetModel.AnnouncementWithTime> getGamesList() {
        return gamesList;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    class KillerAndKilled {
        public Player killer;
        public Player killed;
    }

    private boolean isKill(KillerAndKilled kk) {
        Player killer = kk.killer;
        Player killed = kk.killed;
        for (var body: killer.getBody()) {
            if (body != killed.getBody().getFirst() && body.x == killed.getBody().getFirst().x && body.y == killed.getBody().getFirst().y) {
                return true;
            }
        }
        return false;
    }

    private LinkedList<KillerAndKilled> getKillersAndKilledList() {
        LinkedList<KillerAndKilled> result = new LinkedList<>();
        for (var killer: state.getPlayers()) {
            for (var killed: state.getPlayers()) {
                var kk = new KillerAndKilled();
                kk.killed = killed;
                kk.killer = killer;
                if (isKill(kk)) {
                    result.add(kk);
                }
            }
        }
        return result;
    }

    private void processKills(LinkedList<KillerAndKilled> killList) {
        for (var it: killList) {
            if (it.killed != it.killer) {
                it.killer.score++;
            }
            for (var body: it.killed.getBody()) {
                if (rng.nextDouble() < foodSpawnChanceAfterDeath) {
                    state.getField().setCell(body, Cell.CellType.Food);
                    food++;
                } else {
                    state.getField().setCell(body, Cell.CellType.Empty);
                }
            }
            state.getPlayers().remove(it.killed);
        }
    }

    private void gameOwner() {
        seq = 0;
        netModel.startSender();
        while (!thread.isInterrupted()) {
            synchronized (this) {
                for (var it : state.getPlayers()) {
                    it.doTurn();
                    for (var body : it.getBody()) {
                        state.getField().setCell(body, Cell.CellType.Body);
                    }
                }
                for (int i = food; food < foodCap; ++i) {
                    spawnFood();
                }
                if (currentPlayer != null) {
                    state.getField().setCell(currentPlayer.getBody().getFirst(), Cell.CellType.Head);
                }
                processKills(getKillersAndKilledList());
                if (!state.getPlayers().contains(currentPlayer)) {
                    currentPlayer = null;
                }
                StringBuilder ratingStr = new StringBuilder();
                for (var it : state.getPlayers()) {
                    ratingStr.append(it.name).append(": ").append(it.score).append("\n");
                }
                rating.setText(ratingStr.toString());
                StringBuilder gameInfoString = new StringBuilder();
                gameInfoString.append("Owner: ").append(name).append("\nsize: ").append(fieldXSize).append("x").append(fieldYSize).append("\nfood: ").append(startFood).append("+").append(foodPerPlayer).append("x");
                gameInfo.setText(gameInfoString.toString());
                netModel.sendState();
                seq++;
            }
            canvas.repaint();
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        netModel.announceThread.interrupt();
    }

    private void connectedGame() throws IOException {
        netModel.startSender();
        currentPlayer = new Player(state.getField(), fieldXSize, fieldYSize, NetModel.getId(), name, this);
        while (!thread.isInterrupted()) {
            synchronized (this) {
                state = netModel.getState();
                netModel.sendInput(currentPlayer.newDir);
            }
            canvas.repaint();
            if (netModel.isMaster()) {
                BecameMaster();
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void run() {
        if (isOwner) {
            gameOwner();
        }
        try {
            connectedGame();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void newGame() {
        synchronized (this) {
            this.state = new GameState(fieldXSize, fieldYSize);
            currentPlayer = spawnPlayer(NetModel.getId(), name);
            food = 0;
            foodCap = startFood + (int) (foodPerPlayer * state.getPlayers().size());
            isOwner = true;
            netModel.getPlayerInfoLinkedList().add(new NetModel.PlayerInfo(currentPlayer));
            if (currentPlayer == null) {
                System.exit(-1);
            }
        }
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void BecameMaster() throws IOException {
        netModel.sendChangeRole();
        gameOwner();
    }

    public void connect() {
        synchronized (this) {
            var selected = gamesList.getSelectedValue();
            if (selected == null) {
                return;
            }
            try {
                netModel.connect(selected.received);
            } catch (IOException e) {
                e.printStackTrace();
            }
            setConfig(selected.announcementMsg);
            isOwner = false;
        }
        thread.interrupt();
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "Новая игра": {
                newGame();
                return;
            }
            case "Выход": {
                System.exit(0);
                return;
            }
            case "Подключиться": {
                connect();
                return;
            }
        }
    }

    private void setConfig(SnakesProto.GameMessage.AnnouncementMsg announcementMsg) {
        startFood = announcementMsg.getConfig().getFoodStatic();
        fieldXSize = announcementMsg.getConfig().getWidth();
        fieldYSize = announcementMsg.getConfig().getHeight();
        foodSpawnChanceAfterDeath = announcementMsg.getConfig().getDeadFoodProb();
        foodPerPlayer = announcementMsg.getConfig().getFoodPerPlayer();
        sleepTime = announcementMsg.getConfig().getStateDelayMs();
        netModel.setNodeTimeOut(announcementMsg.getConfig().getNodeTimeoutMs());
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: {
                currentPlayer.setNewDir(Player.Direction.UP);
                return;
            }
            case KeyEvent.VK_S: {
                currentPlayer.setNewDir(Player.Direction.DOWN);
                return;
            }
            case KeyEvent.VK_A: {
                currentPlayer.setNewDir(Player.Direction.LEFT);
                return;
            }
            case KeyEvent.VK_D: {
                currentPlayer.setNewDir(Player.Direction.RIGHT);
                return;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public void SetPlayerInput(int id, Player.Direction direction) {
        var player = state.getPlayer(id);
        if (player != null) player.setNewDir(direction);
    }

}