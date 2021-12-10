package Snake.Logic;

import java.util.HashMap;
import java.util.LinkedList;

public class GameState {
    private Field field;

    private LinkedList<Player> players = new LinkedList<Player>();

    public LinkedList<Player> getPlayers() {
        return players;
    }

    public GameState(int fieldXSize, int fieldYSize) {
        this.field = new Field(fieldXSize, fieldYSize);
    }

    public Field getField() {
        return field;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public Player getPlayer(int id) {
        for (var it: players) {
            if (it.id == id) {
                return it;
            }
        }
        return null;
    }


}
