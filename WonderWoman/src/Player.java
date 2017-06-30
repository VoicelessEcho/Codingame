import java.util.*;
import java.io.*;
import java.math.*;
import java.util.concurrent.ThreadLocalRandom;

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

    public static Map<Integer, Unit> prev_playerUnits;
    public static Map<Integer, Unit> prev_enemyUnits;

    public static int p1Score = 0;
    public static int p2Score = 0;

    public static NeuralNet net;

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

            prev_playerUnits = new HashMap<>();
            prev_enemyUnits = new HashMap<>();

            net = createNet();

            // game loop
            while (true) {
                String row_ = "";
                for (int i = 0; i < size; i++) {
                    String row = in.next();
                    parseCells(row, i);
                    row_ += row + "\n";
                }
                debugMsg(row_);
                //setNextCells(grid, size);


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

                p1Score = updatePlayerScore(p1Score, playerUnits, prev_playerUnits);
                p2Score = updateEnemyScore(p2Score, enemyUnits, prev_enemyUnits);

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

                System.out.println(executeLogic(getP1Inputs(), availActions, grid));
                //System.out.println(executeLogic(grid, availActions, playerUnits, enemyUnits));
                //System.out.println("MOVE&BUILD 0 N S");
                prev_playerUnits.putAll(playerUnits);
                prev_enemyUnits.putAll(enemyUnits);
                playerUnits.clear();
                enemyUnits.clear();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static int updatePlayerScore(int currentScore,  Map<Integer, Unit> playerUnits, Map<Integer, Unit> prev_playerUnits) {
        if (prev_playerUnits.size() == 0){
            return currentScore;
        }
        for (int index : playerUnits.keySet() ) {
            Unit unit = playerUnits.get(index);
            Unit prev_unit = prev_playerUnits.get(index);

            if (unit.x != prev_unit.x || unit.y != prev_unit.y){
                if (grid[unit.x][unit.y].h == 3){
                    currentScore = currentScore + 1;
                }
            }
        }
        return currentScore;
    }

    private static int updateEnemyScore(int currentScore, Map<Integer, Unit> enemyUnits, Map<Integer, Unit> prev_enemyUnits) {
        if (prev_enemyUnits.size() == 0){
            return currentScore;
        }
        for (int index : enemyUnits.keySet() ) {
            Unit unit = enemyUnits.get(index);
            Unit prev_unit = prev_enemyUnits.get(index);

            if (unit.x != prev_unit.x || unit.y != prev_unit.y){
                if (grid[unit.x][unit.y].h == 3){
                    currentScore = currentScore + 1;
                }
            }
        }
        return currentScore;
    }

    private static String executeLogic(int[] grid, List<LegalAction> availActions, GridCell[][] grid_){
        NeuralOutput[] outputs = net.execute(grid);
        LegalAction action = null;

        String msg = " ";
        NeuralOutput moveOut = null;
        NeuralOutput buildOut = null;
        for (int i = 0; i < outputs.length / 2; i++) {
            for (int j = outputs.length/2; j < outputs.length; j++) {
                moveOut = outputs[i];
                buildOut = outputs[j];

                action = getLegaAction(moveOut.dir1, buildOut.dir2, availActions);
                if (action != null){
                    break;
                }
            }
            if (action != null){
                msg += moveOut.dir1.name() + "&" + buildOut.dir2.name();
                break;
            }
        }



        if (action == null){
            Unit player = playerUnits.get(0);
            GridCell playerCell = grid_[player.x][player.y];
            List<GridCell> thirdLevelCells = getThirdLevelCells(playerCell);
            if (thirdLevelCells.size() != 0 && playerCell.h >= 2){
                return getLegalActionMoveToCell(playerCell, thirdLevelCells, availActions);
            }

            int index = 0;
            if (availActions.size() > 1){
                ThreadLocalRandom random = ThreadLocalRandom.current();
                index = random.nextInt(availActions.size());
            }
            action = availActions.get(index);    
            
            
            msg = " Random move :D";
        }

        String s = action.whole + msg;
        return s;
    }

    private static LegalAction getLegaAction(NeuralNet.Dir dir1, NeuralNet.Dir dir2, List<LegalAction> availActions) {
        for (LegalAction action : availActions) {
            if (action.dir1.equals(dir1.name()) && action.dir2.equals(dir2.name())){
                return action;
            }
        }

        return null;
    }

    private static String executeLogic(GridCell[][] grid, List<LegalAction> availActions, Map<Integer, Unit> playerUnits, Map<Integer, Unit> enemyUnits) {
        Unit player = playerUnits.get(0);
        GridCell playerCell = grid[player.x][player.y];
        List<GridCell> thirdLevelCells = getThirdLevelCells(playerCell);
        if (thirdLevelCells.size() != 0 && playerCell.h >= 2){
            return getLegalActionMoveToCell(playerCell, thirdLevelCells, availActions);
        }

        Random random = new Random();
        LegalAction action = null;
        while (action == null){
            if (availActions.size() <= 1){
                action = availActions.get(0);
            }
            else {
                action = availActions.remove(random.nextInt(availActions.size()));
            }

            GridCell actionMoveCell = playerCell.cellsNext.get(Dir.valueOf(action.dir1));
            GridCell actionBuildCell = actionMoveCell.cellsNext.get(Dir.valueOf(action.dir2));

            if (actionBuildCell.h == 3 && actionBuildCell.getNexCellsWithEnemy().size() == 0){
                action = null;
            }
        }

        String s = action.whole + " Going random :D";
        return s;
    }

    private static String getLegalActionMoveToCell(GridCell playerCell, List<GridCell> thirdLevelCells, List<LegalAction> availActions) {
        GridCell tmpCell = thirdLevelCells.get(0);
        GridCell nextCell = null;
        while (nextCell == null && thirdLevelCells.size() != 0){
            nextCell = thirdLevelCells.remove(0);
            for (LegalAction action : availActions ) {
                GridCell actionBuildCell = nextCell.cellsNext.get(Dir.valueOf(action.dir2));
                if (actionBuildCell.h == 3 && actionBuildCell.getNexCellsWithEnemy().size() == 0){
                    nextCell = null;
                }
                if (nextCell != null){
                    return action.whole;
                }
            }

        }
        if (nextCell == null){
            nextCell = tmpCell;
            for (LegalAction action : availActions ) {
                Dir dir = playerCell.getNextCellDirection(nextCell);
                if (Dir.valueOf(action.dir1) == dir){
                    return action.whole;
                }
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

    public static int[] getP1Inputs(){
        int[][] currentGrid = copyGrid(grid);
        for (int i = 0; i < playerUnits.size(); i++) {
            Unit p1Unit = playerUnits.get(i);
            Unit p2Unit = enemyUnits.get(i);

            currentGrid[p1Unit.x][p1Unit.y] = currentGrid[p1Unit.x][p1Unit.y] + 10;
            currentGrid[p2Unit.x][p2Unit.y] = currentGrid[p2Unit.x][p2Unit.y] - 15;
        }
        int[] arrGrid = getGridAsArray(currentGrid);
        int[] arrP1 = new int[arrGrid.length + 1];
        arrP1[0] = p1Score - p2Score;
        for (int i = 0; i < arrGrid.length - 1; i++) {
            arrP1[i + 1] = arrGrid[i];
        }
        return arrP1;
    }

    private static int[][] copyGrid(GridCell[][] grid) {
        int[][] copy = new int[7][7];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid.length; y++) {
                copy[x][y] = grid[x][y].h;
            }
        }
        for (int x = grid.length; x < 7; x++) {
            for (int y = grid.length; y < 7; y++) {
                copy[x][y] = -1;
            }
        }
        return copy;
    }

    public static int[] getGridAsArray(int[][] grid){
        int[] arr = new int[49];
        int i = 0;
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                arr[i] = grid[x][y];
                i++;
            }
        }
        return arr;
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

        public List<GridCell> getNexCellsWithEnemy() {
            List<GridCell> enemyCells = new ArrayList<>();
            for (Dir dir : cellsNext.keySet()) {
                GridCell cell = cellsNext.get(dir);
                if (cell.occupied == Occupied.enemy){
                    enemyCells.add(cell);
                }
            }

            return enemyCells;
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


    public static class NeuralNet{
        public int generationNumber;
        public int parent1;
        public int parent2;
        public int score;
        public GenType type = GenType.None;

        public enum GenType {Generated, Mutated, Combined, None}
        public enum Dir {N, W, E, S, NW, NE, SW, SE, None}

        public List<NeuralLayer> layers;
        public NeuralOutput[] outputs;

        public NeuralNet(List<NeuralLayer> layers, NeuralOutput[] outputs) {
            this.layers = layers;
            this.outputs = outputs;
        }

        public NeuralNet() {
            layers = new ArrayList<>();
            outputs = new NeuralOutput[]{
                    new NeuralOutput(Dir.N, Dir.None),
                    new NeuralOutput(Dir.E, Dir.None),
                    new NeuralOutput(Dir.S, Dir.None),
                    new NeuralOutput(Dir.W, Dir.None),
                    new NeuralOutput(Dir.NE, Dir.None),
                    new NeuralOutput(Dir.NW, Dir.None),
                    new NeuralOutput(Dir.SE, Dir.None),
                    new NeuralOutput(Dir.SW, Dir.None),

                    new NeuralOutput(Dir.None, Dir.N),
                    new NeuralOutput(Dir.None, Dir.E),
                    new NeuralOutput(Dir.None, Dir.S),
                    new NeuralOutput(Dir.None, Dir.W),
                    new NeuralOutput(Dir.None, Dir.NE),
                    new NeuralOutput(Dir.None, Dir.NW),
                    new NeuralOutput(Dir.None, Dir.SE),
                    new NeuralOutput(Dir.None, Dir.SW)
            };
        }

        public NeuralOutput[] execute(int[] inputs){

            double[] prevOutput = new double[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                prevOutput[i] = inputs[i];
            }

            //calc hidden
            for (int i = 0; i < layers.size(); i++) {
                NeuralLayer layer = layers.get(i);
                double [] outputs = layer.computeOutputs(this, prevOutput);
                prevOutput = outputs;
            }

            //calc outputs
            double[] outValues = computeOutputs(prevOutput);

            List<Double> moveValList = new ArrayList<>();
            List<Double> buildValList = new ArrayList<>();

            for (int i = 0; i < outValues.length/2; i++) {
                moveValList.add(outValues[i]);
            }

            for (int i = outValues.length/2; i < outValues.length; i++) {
                buildValList.add(outValues[i]);
            }

            Collections.sort(moveValList);
            Collections.sort(buildValList);

            List<NeuralOutput> sortedOutputs = new ArrayList<>();
            for (int i = 1; i <= moveValList.size(); i++) {
                double val = moveValList.get(moveValList.size() - i);
                for (int j = 0; j < outValues.length / 2; j++) {
                    double valTest = outValues[j];
                    if (val == valTest){
                        sortedOutputs.add(outputs[j]);
                        break;
                    }
                }
            }

            for (int i = 1; i <= buildValList.size(); i++) {
                double val = buildValList.get(buildValList.size() - i);
                for (int j = outValues.length / 2; j < outValues.length; j++) {
                    double valTest = outValues[j];
                    if (val == valTest){
                        sortedOutputs.add(outputs[j]);
                        break;
                    }
                }
            }

            NeuralOutput[] outArr = new NeuralOutput[sortedOutputs.size()];
            outArr = sortedOutputs.toArray(outArr);
            return outArr;
        }

        public double[] computeOutputs(double[] inputs) {
            double[] outputs = new double[this.outputs.length];
            for (int i = 0; i < this.outputs.length; i++) {
                NeuralNode node = this.outputs[i];
                outputs[i] = node.computeOutputs(this, inputs);
            }

            return outputs;
        }
    }

    public static class NeuralLayer{
        public List<NeuralNode> layer = new ArrayList<NeuralNode>();
        public boolean lastLayer;

        public NeuralLayer() {
        }


        public double[] computeOutputs(NeuralNet net, double[] inputs) {
            double[] outputs = new double[layer.size()];
            for (int i = 0; i < layer.size(); i++) {
                NeuralNode node = layer.get(i);
                outputs[i] = node.computeOutputs(net, inputs);
            }

            return outputs;
        }


    }

    public static class NeuralNode{
        public Set<Integer> inputs;
        public Map<Integer, Double> inWeights;

        public NeuralNode(Set<Integer> inputs, Map<Integer, Double> inWeights) {
            this.inputs = inputs;
            this.inWeights = inWeights;
        }

        public NeuralNode() {
            inputs = new HashSet<>();
            inWeights = new HashMap<>();
        }

        public double computeOutputs(NeuralNet net, double[] inputs) {
            try {
                double out = 0;
                for (Integer index : this.inputs) {
                    double in = inputs[index];
                    double inW = inWeights.get(index);
                    out += in * inW;
                }

                out = Math.tanh(out);

                return out;
            }
            catch (Exception e){
                NeuralNode n = this;
                e.printStackTrace();

            }
            return 0;
        }

    }

    public static class NeuralOutput extends NeuralNode{
        public NeuralNet.Dir dir1;
        public NeuralNet.Dir dir2;

        public NeuralOutput(NeuralNet.Dir dir1, NeuralNet.Dir dir2) {
            super();
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        public NeuralOutput() {
            super();
            this.dir1 = null;
            this.dir2 = null;
        }
    }


    public static NeuralNet createNet(){
        List<NeuralLayer> layers = new ArrayList<>();
        //----layer #0----------------
        NeuralLayer l_0 = new NeuralLayer();
        layers.add(l_0);
        //----node #0----------------
        NeuralNode node_l0_0 = new NeuralNode();
        node_l0_0.inputs.addAll(Arrays.asList(0, 1, 2, 3, 5, 8, 10, 11, 12, 15, 17, 19, 21, 22, 23, 25, 26, 27, 28, 32, 36, 37, 38, 40, 42, 44, 45, 46));
        double[] weights_i0_0 = new double[]{0.44904863834381104f, 0.8154012560844421f, 0.2106388360261917f, 0.4149154722690582f, 0.5434101819992065f, 0.2142682522535324f, 0.8500089645385742f, 0.13652397692203522f, 0.03868737816810608f, 0.3694920241832733f, 0.13574707508087158f, 0.2037791758775711f, 0.033211734145879745f, 0.10274738818407059f, 0.21100260317325592f, 0.5734748244285583f, 0.053373582661151886f, 0.16014735400676727f, 0.1611151397228241f, 0.07650334388017654f, 0.040086496621370316f, 0.02724362164735794f, 0.45445945858955383f, 0.6849851012229919f, 0.7560881972312927f, 0.8581806421279907f, 0.44826868176460266f, 0.02920912764966488f};
        setInWeights(node_l0_0, weights_i0_0);
        l_0.layer.add(node_l0_0);
        //----node #1----------------
        NeuralNode node_l0_1 = new NeuralNode();
        node_l0_1.inputs.addAll(Arrays.asList(32, 33, 35, 4, 5, 40, 41, 43, 12, 44, 46, 47, 16, 18, 20, 25, 26, 28, 29, 30));
        double[] weights_i0_1 = new double[]{0.37817904353141785f, 0.955390989780426f, 0.8761708736419678f, 0.04534643888473511f, 0.3695818781852722f, 0.11509853601455688f, 0.14101940393447876f, 0.03544359654188156f, 0.2804485957396713f, 0.11567386239767075f, 0.25560855865478516f, 0.08640450239181519f, 0.027395764365792274f, 0.2295161932706833f, 0.7392041683197021f, 0.7588684558868408f, 0.034564025700092316f, 0.3850066363811493f, 0.18531912565231323f, 0.3184167146682739f};
        setInWeights(node_l0_1, weights_i0_1);
        l_0.layer.add(node_l0_1);
        //----node #2----------------
        NeuralNode node_l0_2 = new NeuralNode();
        node_l0_2.inputs.addAll(Arrays.asList(0, 2, 4, 37, 9, 11, 14, 17, 18, 19, 22, 23, 24, 27, 29, 31));
        double[] weights_i0_2 = new double[]{0.9500888586044312f, 0.4114430546760559f, 0.2455417364835739f, 0.6095142364501953f, 0.9979553818702698f, 0.9614913463592529f, 0.47375985980033875f, 0.4940468668937683f, 0.6628301739692688f, 0.705379843711853f, 0.8643555641174316f, 0.6292794942855835f, 0.5512185299433707f, 0.5689727663993835f, 0.6611193418502808f, 0.1093502938747406f};
        setInWeights(node_l0_2, weights_i0_2);
        l_0.layer.add(node_l0_2);
        //----node #3----------------
        NeuralNode node_l0_3 = new NeuralNode();
        node_l0_3.inputs.addAll(Arrays.asList(1, 2, 5, 6, 7, 41, 42, 43, 11, 45, 13, 14, 46, 17, 20, 22, 23, 24, 28, 29));
        double[] weights_i0_3 = new double[]{0.48951369524002075f, 0.33645790815353394f, 0.20376017689704895f, 0.8402922749519348f, 0.2540936768054962f, 0.6469778418540955f, 0.6213034391403198f, 0.690802276134491f, 0.8232928514480591f, 0.8418956995010376f, 0.3876183032989502f, 0.341083288192749f, 0.002722544362768531f, 0.8966972231864929f, 0.294321745634079f, 0.991377592086792f, 0.9943650364875793f, 0.7588725686073303f, 0.30222946405410767f, 0.4266684055328369f};
        setInWeights(node_l0_3, weights_i0_3);
        l_0.layer.add(node_l0_3);
        //----node #4----------------
        NeuralNode node_l0_4 = new NeuralNode();
        node_l0_4.inputs.addAll(Arrays.asList(2, 35, 3, 5, 6, 39, 40, 24, 10, 43, 29));
        double[] weights_i0_4 = new double[]{0.45630574226379395f, 0.19912390410900116f, 0.16250060498714447f, 0.5344908833503723f, 0.1800544410943985f, 0.0f, 0.040113434195518494f, 0.9346821904182434f, 0.268396258354187f, 0.35177719593048096f, 0.7876257300376892f};
        setInWeights(node_l0_4, weights_i0_4);
        l_0.layer.add(node_l0_4);
        //----node #5----------------
        NeuralNode node_l0_5 = new NeuralNode();
        node_l0_5.inputs.addAll(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 10, 11, 13, 15, 16, 17, 18, 19, 20, 21, 28, 29, 31, 35, 36, 38, 43, 44, 45));
        double[] weights_i0_5 = new double[]{0.402024507522583f, 0.6922608017921448f, 0.17818421125411987f, 0.7958041429519653f, 0.008891873992979527f, 0.13998565077781677f, 0.6914161443710327f, 0.8559950929395668f, 0.12842796742916107f, 0.29830554127693176f, 0.8434110283851624f, 0.9792779088020325f, 0.17897282540798187f, 0.6311670541763306f, 0.9013285505432551f, 0.6706284284591675f, 0.782965898513794f, 0.24634338915348053f, 0.7041075229644775f, 0.2022169828414917f, 0.813182687241822f, 0.46663424372673035f, 0.15355145931243896f, 0.36309507489204407f, 0.6654825806617737f, 0.05397870019078255f, 0.5647884011268616f};
        setInWeights(node_l0_5, weights_i0_5);
        l_0.layer.add(node_l0_5);
        //----node #6----------------
        NeuralNode node_l0_6 = new NeuralNode();
        node_l0_6.inputs.addAll(Arrays.asList(32, 1, 34, 36, 40, 41, 9, 42, 43, 44, 46, 22, 23, 26, 30));
        double[] weights_i0_6 = new double[]{0.7756791114807129f, 0.2902924716472626f, 0.4126213490962982f, 0.7339922189712524f, 0.5794047117233276f, 0.4209118187427521f, 0.4066944718360901f, 0.3581071197986603f, 0.38886067271232605f, 0.3470667004585266f, 0.04373704269528389f, 0.1308008458133758f, 0.4947419762611389f, 0.5624307990074158f, 0.5089544653892517f};
        setInWeights(node_l0_6, weights_i0_6);
        l_0.layer.add(node_l0_6);
        //----node #7----------------
        NeuralNode node_l0_7 = new NeuralNode();
        node_l0_7.inputs.addAll(Arrays.asList(34, 2, 3, 36, 37, 5, 38, 39, 7, 41, 43, 14, 19, 24, 26, 29));
        double[] weights_i0_7 = new double[]{0.14534717798233032f, 0.8373047709465027f, 0.9328005313873291f, 0.23109033703804016f, 0.8273319602012634f, 0.19582320749759674f, 0.9761716723442078f, 0.24086444079875946f, 0.49879440665245056f, 0.3253772556781769f, 0.40168339014053345f, 0.9916143417358398f, 0.3710375130176544f, 0.5453360676765442f, 0.9517301917076111f, 0.927767276763916f};
        setInWeights(node_l0_7, weights_i0_7);
        l_0.layer.add(node_l0_7);
        //----node #8----------------
        NeuralNode node_l0_8 = new NeuralNode();
        node_l0_8.inputs.addAll(Arrays.asList(0, 32, 1, 34, 2, 4, 38, 40, 9, 10, 42, 11, 44, 12, 45, 15, 16, 18, 21, 22, 24, 25, 27, 28));
        double[] weights_i0_8 = new double[]{0.06788395345211029f, 0.6482880711555481f, 0.5448631048202515f, 0.009452275931835175f, 0.2142285853624344f, 0.8000776767730713f, 0.125240758061409f, 0.0827389508485794f, 0.3629891872406006f, 0.02166368067264557f, 0.023841632530093193f, 0.1381540596485138f, 0.3654392957687378f, 0.6755297780036926f, 0.18392424285411835f, 0.2078579217195511f, 0.4780805706977844f, 0.42672327160835266f, 0.437759667634964f, 0.2949143052101135f, 0.35433095693588257f, 0.02138601616024971f, 0.37190520763397217f, 0.2920071482658386f};
        setInWeights(node_l0_8, weights_i0_8);
        l_0.layer.add(node_l0_8);
        //----node #9----------------
        NeuralNode node_l0_9 = new NeuralNode();
        node_l0_9.inputs.addAll(Arrays.asList(16, 37, 22, 39, 42, 26, 46));
        double[] weights_i0_9 = new double[]{0.10911613702774048f, 0.2796440124511719f, 0.5208615064620972f, 0.5925120115280151f, 0.40305033326148987f, 0.8322664499282837f, 0.06133696064352989f};
        setInWeights(node_l0_9, weights_i0_9);
        l_0.layer.add(node_l0_9);
        //----node #10----------------
        NeuralNode node_l0_10 = new NeuralNode();
        node_l0_10.inputs.addAll(Arrays.asList(0, 34, 3, 36, 39, 8, 9, 13, 14, 46, 47, 18, 23, 26, 30));
        double[] weights_i0_10 = new double[]{0.3944954574108124f, 0.583673357963562f, 0.43694573640823364f, 0.2105790227651596f, 0.233907088637352f, 0.4650888442993164f, 0.005405454896390438f, 0.1449701488018036f, 0.01003133412450552f, 0.3168049454689026f, 0.07381827384233475f, 0.2092447131872177f, 0.6146509051322937f, 0.5989264249801636f, 0.9107770919799805f};
        setInWeights(node_l0_10, weights_i0_10);
        l_0.layer.add(node_l0_10);
        //----node #11----------------
        NeuralNode node_l0_11 = new NeuralNode();
        node_l0_11.inputs.addAll(Arrays.asList(18, 2, 35, 22, 26, 11, 27, 29, 13, 30));
        double[] weights_i0_11 = new double[]{0.34025201201438904f, 0.6593665480613708f, 0.15266837179660797f, 0.48042529821395874f, 0.36574703454971313f, 0.9628610610961914f, 0.08962337672710419f, 0.9925202131271362f, 0.20132233202457428f, 0.12448729574680328f};
        setInWeights(node_l0_11, weights_i0_11);
        l_0.layer.add(node_l0_11);
        //----node #12----------------
        NeuralNode node_l0_12 = new NeuralNode();
        node_l0_12.inputs.addAll(Arrays.asList(1, 33, 36, 37, 39, 40, 41, 12, 44, 45, 46, 16, 23, 29, 31));
        double[] weights_i0_12 = new double[]{0.6522121429443359f, 0.9385188221931458f, 0.9976882338523865f, 0.8345653414726257f, 0.30695487689158774f, 0.9666628837585449f, 0.1910768449306488f, 0.833871066570282f, 0.8550965785980225f, 0.41005247831344604f, 0.7812842130661011f, 0.573143482208252f, 0.17895814776420593f, 0.03101278655230999f, 0.2830204963684082f};
        setInWeights(node_l0_12, weights_i0_12);
        l_0.layer.add(node_l0_12);
        //----node #13----------------
        NeuralNode node_l0_13 = new NeuralNode();
        node_l0_13.inputs.addAll(Arrays.asList(18, 22, 31));
        double[] weights_i0_13 = new double[]{0.37111878395080566f, 0.9948122501373291f, 0.0f};
        setInWeights(node_l0_13, weights_i0_13);
        l_0.layer.add(node_l0_13);
        //----node #14----------------
        NeuralNode node_l0_14 = new NeuralNode();
        node_l0_14.inputs.addAll(Arrays.asList(3, 47));
        double[] weights_i0_14 = new double[]{0.08225694298744202f, 0.21977299451828003f};
        setInWeights(node_l0_14, weights_i0_14);
        l_0.layer.add(node_l0_14);

        NeuralOutput outputs[] = new NeuralOutput[16];
        //----output #0----------------
        NeuralOutput out_0 = new NeuralOutput(NeuralNet.Dir.N, NeuralNet.Dir.None);
        out_0.inputs.addAll(Arrays.asList(0, 1, 2, 7, 10, 11));
        double[] weights_o0 = new double[]{0.9788110202778858f, 0.7930197102909391f, 0.22018613305168178f, 0.22457449112208427f, 0.4510691174950765f, 0.23637705147371613f};
        setInWeights(out_0, weights_o0);
        outputs[0] = out_0;

        //----output #1----------------
        NeuralOutput out_1 = new NeuralOutput(NeuralNet.Dir.E, NeuralNet.Dir.None);
        out_1.inputs.addAll(Arrays.asList(1, 2, 3, 7, 10, 11, 13, 14));
        double[] weights_o1 = new double[]{0.033758455196124415f, 0.10230045254933129f, 0.10155734528325433f, 0.2944398722034368f, 0.2369628757745621f, 0.6550558317569443f, 0.9182115316943716f, 0.02990423735845582f};
        setInWeights(out_1, weights_o1);
        outputs[1] = out_1;

        //----output #2----------------
        NeuralOutput out_2 = new NeuralOutput(NeuralNet.Dir.S, NeuralNet.Dir.None);
        out_2.inputs.addAll(Arrays.asList(4, 5, 8));
        double[] weights_o2 = new double[]{0.19470159845605728f, 0.043224616365738266f, 0.6413349707929675f};
        setInWeights(out_2, weights_o2);
        outputs[2] = out_2;

        //----output #3----------------
        NeuralOutput out_3 = new NeuralOutput(NeuralNet.Dir.W, NeuralNet.Dir.None);
        out_3.inputs.addAll(Arrays.asList(0, 3, 5, 6, 9, 12, 13));
        double[] weights_o3 = new double[]{0.04171761729691015f, 0.0f, 0.13259363404843938f, 0.04150324787410109f, 0.3256984737391654f, 0.9293613920164703f, 0.6031886490921611f};
        setInWeights(out_3, weights_o3);
        outputs[3] = out_3;

        //----output #4----------------
        NeuralOutput out_4 = new NeuralOutput(NeuralNet.Dir.NE, NeuralNet.Dir.None);
        out_4.inputs.addAll(Arrays.asList(0, 2, 3, 4, 5, 6, 8, 12, 13));
        double[] weights_o4 = new double[]{0.14801860752227602f, 0.3809129052686112f, 0.13919082504537106f, 0.15309329852796416f, 0.31907046326043653f, 0.46525202666100984f, 0.47354674377730244f, 0.65778185752881f, 0.8421787162598267f};
        setInWeights(out_4, weights_o4);
        outputs[4] = out_4;

        //----output #5----------------
        NeuralOutput out_5 = new NeuralOutput(NeuralNet.Dir.NW, NeuralNet.Dir.None);
        out_5.inputs.addAll(Arrays.asList(0, 5));
        double[] weights_o5 = new double[]{0.006374541891430274f, 0.34391503622257724f};
        setInWeights(out_5, weights_o5);
        outputs[5] = out_5;

        //----output #6----------------
        NeuralOutput out_6 = new NeuralOutput(NeuralNet.Dir.SE, NeuralNet.Dir.None);
        out_6.inputs.addAll(Arrays.asList(1, 5, 7, 8, 9, 13, 14));
        double[] weights_o6 = new double[]{0.02501238713017362f, 0.7551819418928416f, 0.09682613760193792f, 0.06729042632268623f, 0.32661133087273553f, 0.08011077219768759f, 0.8320138975866589f};
        setInWeights(out_6, weights_o6);
        outputs[6] = out_6;

        //----output #7----------------
        NeuralOutput out_7 = new NeuralOutput(NeuralNet.Dir.SW, NeuralNet.Dir.None);
        out_7.inputs.addAll(Arrays.asList(3, 7, 10));
        double[] weights_o7 = new double[]{0.9640739349926928f, 0.9162661937270487f, 0.04550015828348686f};
        setInWeights(out_7, weights_o7);
        outputs[7] = out_7;

        //----output #8----------------
        NeuralOutput out_8 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.N);
        out_8.inputs.addAll(Arrays.asList(2, 8, 13));
        double[] weights_o8 = new double[]{0.14498397556286935f, 0.7964933258562786f, 0.1397239151939237f};
        setInWeights(out_8, weights_o8);
        outputs[8] = out_8;

        //----output #9----------------
        NeuralOutput out_9 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.E);
        out_9.inputs.addAll(Arrays.asList(1, 4, 6, 8, 9, 10, 11, 13, 14));
        double[] weights_o9 = new double[]{0.19186775126626432f, 0.32184839216546124f, 0.016110757974791667f, 0.029302656504605284f, 0.8443443031461398f, 0.20142223754776245f, 0.03338957356987016f, 0.6554702165656571f, 0.2556366068108621f};
        setInWeights(out_9, weights_o9);
        outputs[9] = out_9;

        //----output #10----------------
        NeuralOutput out_10 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.S);
        out_10.inputs.addAll(Arrays.asList(0, 1, 2, 3, 4, 8, 11, 12, 13));
        double[] weights_o10 = new double[]{0.5959382157507654f, 0.6522681487228936f, 0.0651045166519606f, 0.01821680564253436f, 0.4512313014150078f, 0.01612731972971615f, 0.05075596531542492f, 0.007805626882787298f, 0.04069714789126111f};
        setInWeights(out_10, weights_o10);
        outputs[10] = out_10;

        //----output #11----------------
        NeuralOutput out_11 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.W);
        out_11.inputs.addAll(Arrays.asList(8, 9, 11, 13));
        double[] weights_o11 = new double[]{0.2897543360783973f, 0.7054876739889526f, 0.9928078171405734f, 0.9478846623861362f};
        setInWeights(out_11, weights_o11);
        outputs[11] = out_11;

        //----output #12----------------
        NeuralOutput out_12 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.NE);
        out_12.inputs.addAll(Arrays.asList(3, 7, 8, 10));
        double[] weights_o12 = new double[]{0.9207104785389933f, 0.993146084464388f, 0.2544113783465124f, 0.016346755365764243f};
        setInWeights(out_12, weights_o12);
        outputs[12] = out_12;

        //----output #13----------------
        NeuralOutput out_13 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.NW);
        out_13.inputs.addAll(Arrays.asList(1, 11));
        double[] weights_o13 = new double[]{0.050117265312224935f, 0.03009547791178191f};
        setInWeights(out_13, weights_o13);
        outputs[13] = out_13;

        //----output #14----------------
        NeuralOutput out_14 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.SE);
        out_14.inputs.addAll(Arrays.asList(2, 5, 6, 9));
        double[] weights_o14 = new double[]{0.0287877505957721f, 0.8854461377941075f, 0.18125795304277015f, 0.0f};
        setInWeights(out_14, weights_o14);
        outputs[14] = out_14;

        //----output #15----------------
        NeuralOutput out_15 = new NeuralOutput(NeuralNet.Dir.None, NeuralNet.Dir.SW);
        out_15.inputs.addAll(Arrays.asList(1, 10));
        double[] weights_o15 = new double[]{0.09462303268968197f, 0.43801587004954823f};
        setInWeights(out_15, weights_o15);
        outputs[15] = out_15;

        NeuralNet net = new NeuralNet(layers, outputs);
        net.type = NeuralNet.GenType.Mutated;
        net.generationNumber = 0;
        net.parent1 = 0;
        net.parent2 = 0;
        net.score = 58461;

        return net;
    }

    private static void setInWeights(NeuralNode out, double[] weights) {
        int i = 0;
        for (int in : out.inputs ) {
            out.inWeights.put(in, weights[i]);
            i++;
        }
    }

}