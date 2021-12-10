package Snake.Logic;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Cell {
    private static Map<CellType ,Color> colorMap = null;
    private CellType typeOfCell;

    public void setTypeOfCell(CellType typeOfCell) {
        this.typeOfCell = typeOfCell;
    }

    public CellType getTypeOfCell() {
        return typeOfCell;
    }

    public void initMap() {
        if (colorMap == null) {
            colorMap = new HashMap<>();
            colorMap.put(CellType.Empty, Color.gray);
            colorMap.put(CellType.Head, Color.MAGENTA);
            colorMap.put(CellType.Body, Color.RED);
            colorMap.put(CellType.Food, Color.ORANGE);
        }
    }

    public enum CellType {
        Empty, Head, Body, Food
    }

    Cell(CellType typeOfCell) {
        initMap();
        this.typeOfCell = typeOfCell;
    }

    public Color getColor() {
        return colorMap.get(typeOfCell);
    };
}
