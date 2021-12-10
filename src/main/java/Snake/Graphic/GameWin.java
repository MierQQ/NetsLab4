package Snake.Graphic;

import Snake.Logic.Field;
import Snake.Logic.Model;
import Snake.Net.NetModel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GameWin extends JFrame {
    private final int width;
    private final int height;
    private final int borderSize;
    private Model gameModel;

    private final Canvas gameField = new Canvas();
    private final JTextArea rating = new JTextArea();
    private final JTextArea gameInfo = new JTextArea();
    private final JList<NetModel.AnnouncementWithTime> gamesList = new JList<>();
    private final JButton exitButton = new JButton("Выход");
    private final JButton newGameButton = new JButton("Новая игра");
    private final JButton connectToGame = new JButton("Подключиться");

    class Canvas extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Field field = gameModel.getState().getField();
            gameField.drawField(field, g);
        }

        private void drawField(Field field, Graphics g) {
            int columns = field.getNumberOfColumns();
            int lines = field.getNumberOfLines();
            int squareSize = Math.min((width / 2 - 2 * borderSize) / field.getNumberOfColumns(), (height - 2 * borderSize) / field.getNumberOfLines());
            for (int x = 0; x < columns; ++x) {
                for (int y = 0; y < lines; ++y) {
                    g.setColor(field.getCellColor(x, y));
                    g.fillRect(x * squareSize, y * squareSize, squareSize, squareSize);
                }
            }
            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i <= lines; ++i) {
                g.drawLine(i * squareSize - 1, 0, i * squareSize - 1, lines * squareSize - 1);
                if (lines != i) {
                    g.drawLine(i * squareSize, 0, i * squareSize, lines * squareSize - 1);
                }
            }
            for (int i = 0; i <= columns; ++i) {
                g.drawLine(0, i * squareSize - 1, columns * squareSize - 1, i * squareSize - 1);
                if (columns != i) {
                    g.drawLine(0, i * squareSize, columns * squareSize - 1, i * squareSize);
                }
            }
        }
    }

    public GameWin(int width, int height, int borderSize) {
        super("Super turbo street megadrive racing snakes tournament");
        this.width = width;
        this.height = height;
        this.borderSize = borderSize;
        this.setBounds(100,100, width, height);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container container = this.getContentPane();
        container.setLayout(null);
        gameField.setBounds(borderSize, borderSize,width / 2 - 2 * borderSize, height - 2 * borderSize);
        rating.setBounds(width / 2 + borderSize, borderSize, width / 4 - 2 * borderSize, height / 4 - 2 * borderSize);
        //rating.setText("rating");
        gameInfo.setBounds(width * 3 / 4 + borderSize, borderSize, width / 4 - 2 * borderSize, height / 4 - 2 * borderSize);
        //gameInfo.setText("gameInfo");
        //gamesList.setListData(new String[]{"eere", "sdsd", "dsfsdf"});
        gamesList.setBounds(width / 2 + borderSize, height / 2 + borderSize, width / 2 - 2 * borderSize, height * 3 / 8 - 2 * borderSize);
        exitButton.setBounds(width / 2 + borderSize, height / 4 + borderSize , width / 4 - 2 * borderSize, height / 4 - 2 * borderSize);
        newGameButton.setBounds(width * 3 / 4 + borderSize, height / 4 + borderSize,width / 4 - 2 * borderSize, height / 4 - 2 * borderSize);
        connectToGame.setBounds(width / 2 + borderSize, height * 7 / 8 + borderSize, width / 2 - 2 * borderSize, height / 16 - 2 * borderSize);
        container.add(connectToGame);
        connectToGame.setFocusable(false);
        container.add(gameField);
        gameField.setFocusable(false);
        container.add(rating);
        rating.setFocusable(false);
        container.add(gameInfo);
        gameInfo.setFocusable(false);
        container.add(gamesList);
        gamesList.setFocusable(false);
        container.add(exitButton);
        exitButton.setFocusable(false);
        container.add(newGameButton);
        newGameButton.setFocusable(false);
        try {
            gameModel = new Model(this, gameField, rating, gameInfo, gamesList, exitButton, newGameButton, connectToGame);
        } catch (IOException exception) {
            System.exit(-2);
        }
        setFocusable(true);
        setVisible(true);
    }
}
