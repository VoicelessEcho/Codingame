import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public enum Occupied {player, enemy, empty}
    public enum Dir{N, S, E, W, NE, NW, SE, SW}
    public static GridCell[][] grid;
    public static List<LegalAction> availActions;
    public static Map<Integer, Unit> playerUnits;
    public static Map<Integer, Unit> enemyUnits;

    public static void main(String args[]) {
        try {
            Scanner in = new Scanner(System.in);
            int size = in.nextInt();
            int unitsPerPlayer = in.nextInt();


            debugMsg(String.valueOf(size));
            debugMsg(String.valueOf(unitsPerPlayer));

            grid = new GridCell[size][size];
            availActions = new ArrayList<>();
            playerUnits = new HashMap<>();
            enemyUnits = new HashMap<>();

            // game loop
            while (true) {
                String row_ = "";
                for (int i = 0; i < size; i++) {
                    String row = in.next();
                    parseCells(row, i);
                    row_ += row + "\n";
                }
                debugMsg(row_);
                setNextCells(grid, size);


                for (int i = 0; i < unitsPerPlayer; i++) {
                    int unitX = in.nextInt();
                    int unitY = in.nextInt();
                    debugMsg(String.valueOf(unitX) + " " + String.valueOf(unitY));

                    grid[unitX][unitY].occupied = Occupied.player;
                    grid[unitX][unitY].charId = i;
                    Unit unit = playerUnits.get(i);
                    if (unit == null) {
                        unit = new Unit(Occupied.player, unitX, unitY, i);
                        playerUnits.put(i, unit);
                    }
                    else {
                        unit.x = unitX;
                        unit.y = unitY;
                    }
                    unit.actions.clear();
                }
                for (int i = 0; i < unitsPerPlayer; i++) {
                    int otherX = in.nextInt();
                    int otherY = in.nextInt();
                    debugMsg(String.valueOf(otherX) + " " + String.valueOf(otherY));

                    if (otherX > -1 && otherY > -1) {
                        grid[otherX][otherY].occupied = Occupied.enemy;
                        grid[otherX][otherY].charId = i;
                    }
                    Unit unit = enemyUnits.get(i);
                    if (unit == null) {
                        unit = new Unit(Occupied.enemy, otherX, otherY, i);
                        enemyUnits.put(i, unit);
                    }
                    else {
                        unit.x = otherX;
                        unit.y = otherY;
                    }
                }

                availActions.clear();
                int legalActions = in.nextInt();
                debugMsg(legalActions);
                for (int i = 0; i < legalActions; i++) {
                    String atype = in.next();
                    int index = in.nextInt();
                    String dir1 = in.next();
                    String dir2 = in.next();

                    debugMsg(atype + " " + String.valueOf(index) + " " + dir1 + " " + dir2);

                    LegalAction action = new LegalAction(atype,
                            index,
                            dir1,
                            dir2,
                            atype + " " + String.valueOf(index) + " " + dir1 + " " + dir2);
                    availActions.add(action);
                    playerUnits.get(index).actions.add(action);
                }

                debugMsg(cellsView(grid, size));
                //debugMsg(actionsView(availActions));
                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

                System.out.println(executeLogic(grid, availActions, playerUnits, enemyUnits));
                //System.out.println("MOVE&BUILD 0 N S");
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String executeLogic(GridCell[][] grid, List<LegalAction> availActions, Map<Integer, Unit> playerUnits, Map<Integer, Unit> enemyUnits) {
        Unit player = playerUnits.get(0);
        GridCell playerCell = grid[player.x][player.y];
        List<GridCell> thirdLevelCells = getThirdLevelCells(playerCell);
        if (thirdLevelCells.size() != 0 && playerCell.h == 2){
            return getLegalActionMoveToCell(playerCell, thirdLevelCells, availActions);
        }

        Random random = new Random();
        LegalAction action = availActions.get(random.nextInt(availActions.size() - 1));
        String s = action.whole + " Going random :D";
        return s;
    }

    private static String getLegalActionMoveToCell(GridCell playerCell, List<GridCell> thirdLevelCells, List<LegalAction> availActions) {
        GridCell nextCell = thirdLevelCells.get(0);
        for (LegalAction action : availActions ) {
            Dir dir = playerCell.getNextCellDirection(nextCell);
            if (Dir.valueOf(action.dir1) == dir){
                return action.whole;
            }
        }
        return null;
    }

    private static List<GridCell> getThirdLevelCells(GridCell cell) {
        List<GridCell> cellList = new ArrayList<>();
        Map<Dir, GridCell> cellMap = cell.cellsNext;
        for (Dir d : cellMap.keySet()) {
            GridCell cellNext = cellMap.get(d);
            if (cellNext.h == 3){
                cellList.add(cellNext);
            }
        }
        return cellList;
    }

    public static void debugMsg(Object msg){
        System.err.println(msg);
    }
    public static String cellsView(GridCell[][] cells, int size){
        String sep = "+";
        for (int i = 0; i < size; i++) {
            sep += "---+";
        }
        sep += "\n";

        String[] sarr = new String[size];
        Arrays.fill(sarr, "|");
        ArrayList<String> strList = new ArrayList<>();
        strList.addAll(Arrays.asList(sarr));

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                String s = strList.get(y);
                GridCell cell = cells[x][y];
                String tok = "";
                if (cell.h > -1){
                    switch (cell.occupied){
                        case player:
                            tok += String.valueOf(cell.charId);
                            tok += "P" + String.valueOf(cell.h);
                            tok += "|";
                            break;
                        case enemy:
                            tok += String.valueOf(cell.charId);
                            tok += "E" + String.valueOf(cell.h);
                            tok += "|";
                            break;
                        case empty:
                            tok += "   |";
                            break;
                    }
                }
                else {
                    tok += " -1|";
                }
                s += tok;
                strList.remove(y);
                strList.add(y, s);
            }
        }
        String lines = sep;
        for (String line : strList) {
            lines += line + "\n" + sep;
        }

        return lines;
    }
    public static String actionsView(List<LegalAction> actions){
        String actionsStr = "";
        for (LegalAction action : actions) {
            actionsStr += String.valueOf(action.unitId) +
                    " " + action.atype;
            if (action.dir1 != null){
                actionsStr += " Move: " + action.dir1;
            }

            if (action.dir2 != null){
               actionsStr += " Build: " + action.dir2;
            }
            actionsStr += "\n";
        }
        return actionsStr;
    }

    public static void parseCells(String row, int rowNumber){
        for (int i = 0; i < row.length() ; i++) {
            Character c = row.charAt(i);
            int h = -1;
            switch (c){
                case '.':
                    break;
                default:
                    h = Integer.parseInt("" + c);
            }
            grid[i][rowNumber] = new GridCell(i, rowNumber, h, Occupied.empty, null);
        }
    }
    public static void setNextCells(GridCell[][] grid, int size){
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                GridCell cell = grid[x][y];
                if (cell.h == -1){
                    continue;
                }
                if (x > 0){
                    GridCell cw = grid[x - 1][y];
                    if (cw.h != -1) {
                        cell.cellsNext.put(Dir.W, cw);
                    }
                    if (y > 0){
                        GridCell cnw = grid[x - 1][y - 1];
                        if (cnw.h != -1) {
                            cell.cellsNext.put(Dir.NW, cnw);
                        }
                    }
                    if (y < size - 1){
                        GridCell csw = grid[x - 1][y + 1];
                        if (csw.h != -1) {
                            cell.cellsNext.put(Dir.SW, csw);
                        }
                    }
                }
                if (x < size - 1){
                    GridCell ce = grid[x + 1][y];
                    if (ce.h != -1) {
                        cell.cellsNext.put(Dir.E, ce);
                    }
                    if (y > 0){
                        GridCell cne = grid[x + 1][y - 1];
                        if (cne.h != -1) {
                            cell.cellsNext.put(Dir.NE, cne);
                        }
                    }
                    if (y < size - 1){
                        GridCell cse = grid[x + 1][y + 1];
                        if (cse.h != -1) {
                            cell.cellsNext.put(Dir.SE, cse);
                        }
                    }
                }
                if (y > 0){
                    GridCell cn = grid[x][y - 1];
                    if (cn.h != -1) {
                        cell.cellsNext.put(Dir.N, cn);
                    }
                }
                if (y < size - 1){
                    GridCell cs = grid[x][y + 1];
                    if (cs.h != -1) {
                        cell.cellsNext.put(Dir.S, cs);
                    }
                }

            }
        }
    }


    public static class GridCell{
        public Occupied occupied;
        public Integer x;
        public Integer y;
        public Integer h; //Height
        public Integer charId;
        public Unit unit;
        public Map<Dir, GridCell> cellsNext;


        public GridCell(Integer x, Integer y, Integer h, Occupied occupied, Integer charId) {
            this.occupied = occupied;
            this.x = x;
            this.y = y;
            this.h = h;
            this.charId = charId;
            this.unit = null;
            this.cellsNext = new HashMap<>();
        }

        public Dir getNextCellDirection(GridCell cell){
            for (Dir dir : cellsNext.keySet() ) {
                GridCell c = cellsNext.get(dir);
                if (c.x == cell.x && c.y == cell.y){
                    return dir;
                }
            }
            return  null;
        }
    }

    public static class LegalAction{
        public String atype;
        public Integer unitId;
        public String dir1;
        public String dir2;
        public String whole;

        public LegalAction(String atype, Integer unitId, String dir1, String dir2, String whole) {
            this.atype = atype;
            this.unitId = unitId;
            this.dir1 = dir1;
            this.dir2 = dir2;
            this.whole = whole;
        }
    }

    public static class Unit{
        public Occupied type;
        public Integer x;
        public Integer y;
        public Integer id;
        public List<LegalAction> actions;

        public Unit(Occupied type, Integer x, Integer y, Integer id) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.id = id;
            this.actions = new ArrayList<>();
        }
    }
}