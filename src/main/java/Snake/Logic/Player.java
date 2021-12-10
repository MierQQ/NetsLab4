package Snake.Logic;

import Protobuf.SnakesProto;

import java.awt.*;
import java.util.LinkedList;

public class Player {
    private LinkedList<Point> body = new LinkedList<>();
    private Model model;
    Field field;
    public int score = 0;
    public int id;
    public String name;

    public Player(SnakesProto.GameState.Snake snake, Model model, String name) {
        for (var it :snake.getPointsList()) {
            body.add(new Point(it.getX(), it.getY()));
        }
        field = model.getState().getField();
        this.model = model;
        id = snake.getPlayerId();
        name = name;
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;
        public int getDY() {
            switch (this) {
                case UP: {
                    return -1;
                }
                case DOWN: {
                    return 1;
                }
                default: {
                    return 0;
                }
            }
        }

        public int getDX() {
            switch (this) {
                case RIGHT: {
                    return 1;
                }
                case LEFT: {
                    return -1;
                }
                default: {
                    return 0;
                }
            }
        }
    }

    public Direction lastDir;
    public Direction newDir;

    public void setNewDir(Direction newDir) {
        this.newDir = newDir;
    }

    public Player(Field field, int x, int y, int id, String name, Model model) {
        this.name = name;
        this.field = field;
        this.model = model;
        body.addFirst(new Point(x,y));
        lastDir = Direction.UP;
        newDir = lastDir;
    }

    public void doTurn() {
        if (newDir.getDX() + lastDir.getDX() == 0 && newDir.getDY() + lastDir.getDY() == 0) {
            newDir = lastDir;
        }
        int xHead = (body.peekFirst().x + newDir.getDX() + field.getNumberOfColumns()) % field.getNumberOfColumns();
        int yHead = (body.peekFirst().y + newDir.getDY() + field.getNumberOfLines()) % field.getNumberOfLines();
        var nextCell = field.getCell(xHead, yHead);
        var head = new Point(xHead, yHead);
        body.addFirst(head);
        if (nextCell != Cell.CellType.Food) {
            field.setCell(body.getLast(), Cell.CellType.Empty);
            body.removeLast();
        }
        if (nextCell == Cell.CellType.Food) {
            ++score;
            model.foodEaten();
        }
        lastDir = newDir;
    }

    public LinkedList<Point> getBody() {
        return body;
    }

}
