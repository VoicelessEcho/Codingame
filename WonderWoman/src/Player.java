import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public enum Occupied {player, enemy, empty}
    public static GridCell[][] grid;
    public static List<LegalAction> availActions;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int size = in.nextInt();
        int unitsPerPlayer = in.nextInt();

        debugMsg("Size: " + String.valueOf(size));
        debugMsg("Player units: " + String.valueOf(unitsPerPlayer));

        grid = new GridCell[size][size];
        availActions = new ArrayList<>();

        // game loop
        while (true) {
            String row_ = "";
            for (int i = 0; i < size; i++) {
                String row = in.next();
                parseCells(row, i);
                row_ += row + "\n";
            }
            debugMsg(row_);



            for (int i = 0; i < unitsPerPlayer; i++) {
                int unitX = in.nextInt();
                int unitY = in.nextInt();

                grid[unitY][unitX].occupied = Occupied.player;
                grid[unitY][unitX].charId = i;
            }
            for (int i = 0; i < unitsPerPlayer; i++) {
                int otherX = in.nextInt();
                int otherY = in.nextInt();

                if (otherX > -1 && otherY > -1) {
                    grid[otherY][otherX].occupied = Occupied.enemy;
                    grid[otherY][otherX].charId = i;
                }
            }

            availActions.clear();
            int legalActions = in.nextInt();
            for (int i = 0; i < legalActions; i++) {
                String atype = in.next();
                int index = in.nextInt();
                String dir1 = in.next();
                String dir2 = in.next();
            }

            debugMsg(cellsView(grid, size));
            debugMsg(actionsView(availActions));
            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            System.out.println("MOVE&BUILD 0 N S");
        }
    }

    public static void debugMsg(Object msg){
        System.err.println(msg);
    }
    public static String cellsView(GridCell[][] cells, int size){
        String msg = "";
        String sep = "+";
        for (int i = 0; i < size; i++) {
            sep += "---+";
        }
        msg += sep + "\n";
        for (int i = 0; i < size; i++) {
            String line = "|";
            for (int j = 0; j < size; j++) {
                GridCell cell = grid[i][j];
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
                line += tok;
            }
            line += "\n";
            msg += line;
            msg += sep + "\n";
        }
        return msg;
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
            grid[rowNumber][i] = new GridCell(rowNumber, i, h, Occupied.empty, null);
        }
    }



    public static class GridCell{
        public Occupied occupied;
        public Integer x;
        public Integer y;
        public Integer h; //Height
        public Integer charId;

        public GridCell(Integer x, Integer y, Integer h, Occupied occupied, Integer charId) {
            this.occupied = occupied;
            this.x = x;
            this.y = y;
            this.h = h;
            this.charId = charId;
        }
    }

    public static class LegalAction{
        public String atype;
        public Integer unitId;
        public String dir1;
        public String dir2;

        public LegalAction(String atype, Integer unitId, String dir1, String dir2) {
            this.atype = atype;
            this.unitId = unitId;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }
    }

}