package Snake.Logic;

import java.awt.*;

public class Field {
    private final Cell[][] field;
    private final int numberOfColumns;
    private final int numberOfLines;

    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public Field(int numberOfColumns, int numberOfLines) {
        this.numberOfColumns = numberOfColumns;
        this.numberOfLines = numberOfLines;
        field = new Cell[numberOfColumns][numberOfLines];
        for (int x = 0; x < numberOfColumns; ++x) {
            for (int y = 0; y < numberOfLines; ++y) {
                field[x][y] = new Cell(Cell.CellType.Empty);
            }
        }
    }

    public Cell.CellType getCell(int x, int y) {
        return field[x][y].getTypeOfCell();
    }

    public void setCell(int x, int y, Cell.CellType type) {
        field[x][y].setTypeOfCell(type);
    }

    public void setCell(Point point, Cell.CellType type) {
        field[point.x][point.y].setTypeOfCell(type);
    }

    public Color getCellColor(int x, int y) {
        return field[x][y].getColor();
    }

    public Color getCellColor(Point point) {
        return field[point.x][point.y].getColor();
    }

    public boolean isGoodForPlacement(int x, int y) {
        for (int i = x; i < x + 5; ++i) {
            for (int j = y; j < y + 5; ++j) {
                if (field[i % numberOfColumns][j % numberOfLines].getTypeOfCell() != Cell.CellType.Empty) {
                    return false;
                }
            }
        }
        return true;
    }

    public Point getEmptyBox() {
        for (int x = 0; x < numberOfColumns; ++x) {
            for (int y = 0; y < numberOfLines; ++y) {
                if (isGoodForPlacement(x, y)) {
                    return new Point((x + 2) % numberOfColumns, (y + 2) % numberOfLines);
                }
            }
        }
        return new Point(-1, -1);
    }
}
