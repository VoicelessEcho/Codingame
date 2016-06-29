import java.util.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right

        HashMap<Integer, Buster> bustersMap = new HashMap<>();
        HashMap<Integer, Ghost> ghostsMap = new HashMap<>();
        HashMap<Integer, EnemyBuster> enemyBustersMap = new HashMap<>();
        HashMap<Integer, Ghost> targetedGhostsMap = new HashMap<>();
        HashMap<Integer, EnemyBuster> targetedEnemyBusters = new HashMap<>();
        Home home = new Home(0, 0);
        if (myTeamId == 1){
            home.setX(16000);
            home.setY(9000);
        }
        Map map = new Map(myTeamId);
        StunCounter stunCounter = new StunCounter();

        // game loop
        while (true) {
            stunCounter.updateTurn();
            map.clearTargetedCheckpoints();
            targetedEnemyBusters.clear();
            targetedGhostsMap.clear();
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.

                Entity e = null;
                if (entityType == -1){
                    e = ghostsMap.get(entityId);
                    if (e == null){
                        e = new Ghost(entityId, entityType);
                        ghostsMap.put(entityId, (Ghost) e);
                    }
                }
                else {
                    if (myTeamId == entityType){
                        e = bustersMap.get(entityId);
                        if (e == null){
                            e = new Buster(entityId, entityType);
                            bustersMap.put(entityId, (Buster) e);
                        }

                        map.updateCheckpoints((Buster)e);
                        stunCounter.updateBuster((Buster) e);

                        Buster b = (Buster) e;
                        if (!b.isIdle()){
                            ghostsMap.remove(b.getValue());
                        }

                    }
                    else {
                        e = enemyBustersMap.get(entityId);
                        if (e == null){
                            e = new EnemyBuster(entityId, entityType);
                            enemyBustersMap.put(entityId, (EnemyBuster) e);
                        }
                    }
                }

                e.updateParametersLoud(x, y, state, value);
            }

            List<Buster> busters = new ArrayList<>();
            busters.addAll(bustersMap.values());
            for (int i = 0; i < bustersPerPlayer; i++) {
                Buster buster = busters.get(i);
                if (buster.isIdle()){
                    Ghost ghost = getNearestGhost(buster, ghostsMap.values());
                    List<Ghost> nonTargetedGhosts = getNonTargetedGhosts(ghostsMap, targetedGhostsMap);
                    if (ghost == null){
                        //No ghosts around
                        List<EnemyBuster> enemyBusters = getEnemyBustersWithGhosts(enemyBustersMap);
                        EnemyBuster enemyBuster = getNearestNonTargetedEnemyBuster(buster, enemyBusters, targetedEnemyBusters);
                        if (enemyBuster != null && map.isInReach(buster, enemyBuster, map.getGhostReachMax())){
                            //Suitable enemy with ghost found
                            targetedEnemyBusters.put(enemyBuster.getEntityId(), enemyBuster);
                            System.out.println("STUN " + String.valueOf(enemyBuster.getEntityId()) + " Stun enemy " + String.valueOf(enemyBuster.getEntityId()));
                        }
                        else{
                            Checkpoint checkpoint = map.findNonVisitedNearestCheckpoint(buster);
                            map.targetCheckpoint(checkpoint);
                            System.out.println("MOVE " + String.valueOf(checkpoint.getX()) + " " + String.valueOf(checkpoint.getY()) + " " + String.valueOf(buster.getEntityId()));
                        }
                    }
                    else {
                        //Ghost around
                        List<EnemyBuster> enemyBusters = getEnemyBustersWithGhosts(enemyBustersMap);
                        EnemyBuster enemyBuster = getNearestNonTargetedEnemyBuster(buster, enemyBusters, targetedEnemyBusters);
                        if (enemyBuster != null && map.isInReach(buster, enemyBuster, map.getGhostReachMax())){
                            //Suitable enemy with ghost found
                            targetedEnemyBusters.put(enemyBuster.getEntityId(), enemyBuster);
                            System.out.println("STUN " + String.valueOf(enemyBuster.getEntityId()) + " Stun enemy " + String.valueOf(enemyBuster.getEntityId()));
                        }
                        else {
                            //No suitable enemy with ghost
                            if (map.isInReach(buster, ghost, map.getGhostReachMin())){
                                //Ghost is too close
                                System.out.println("MOVE " + String.valueOf(home.getX()) + " " + String.valueOf(home.getY()) + " " + String.valueOf(buster.getEntityId()));
                                List<Ghost> nonTargetedInHitDistGhosts = getGhostsInReach(buster, nonTargetedGhosts, map);
                                if (nonTargetedInHitDistGhosts.size() != 0){
                                    //Suitable other ghost

                                    Ghost otherGhost = getNearestGhost(buster, nonTargetedInHitDistGhosts);
                                    targetedGhostsMap.put(otherGhost.getEntityId(), otherGhost);
                                    System.out.println("BUST " + String.valueOf(otherGhost.getEntityId()) + " " + String.valueOf(buster.getEntityId()) +  " Trap ghost " + String.valueOf(otherGhost.getEntityId()));
                                }
                                else {
                                    //no other suitable ghost
                                    System.out.println("MOVE " + String.valueOf(home.getX()) + " " + String.valueOf(home.getY()) + " " + String.valueOf(buster.getEntityId()));
                                }
                            }
                            else {
                                //Ghost is ! too close
                                if (map.isInReach(buster, ghost, map.getGhostReachMax())){
                                    //Ghost in hit distance
                                    System.out.println("BUST " + String.valueOf(ghost.getEntityId()) + " " + String.valueOf(buster.getEntityId()) + " Trap ghost " + String.valueOf(ghost.getEntityId()));
                                }
                                else {
                                    System.out.println("MOVE " + String.valueOf(ghost.getX()) + " " + String.valueOf(ghost.getY()) + " " + String.valueOf(buster.getEntityId()));
                                }
                            }
                        }
                    }
                }
                else {
                    if (map.isInReach(buster, home, map.getHomeDist())){
                        System.out.println("RELEASE " + " " + String.valueOf(buster.getEntityId()));
                    }
                    else {
                        System.out.println("MOVE " + String.valueOf(home.getX()) + " " + String.valueOf(home.getY()) +
                                " Going home" + String.valueOf(buster.getEntityId()));
                    }
                }

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

                //System.out.println("MOVE 8000 4500"); // MOVE x y | BUST id | RELEASE
            }
        }
    }

    private static List<Ghost> getGhostsInReach(Buster buster, Collection<Ghost> ghosts, Map map){
        List<Ghost> list = new ArrayList<>();
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()){
            Ghost g = it.next();
            if (!map.isInReach(buster, g, map.getGhostReachMin()) && map.isInReach(buster, g, map.getGhostReachMax())){
                list.add(g);
            }
        }

        return list;
    }

    private static List<Ghost> getNonTargetedGhosts(HashMap<Integer, Ghost> ghostMap, HashMap<Integer, Ghost> targetedGhostMap) {
        List<Ghost> list = new ArrayList<>();
        Iterator<Integer> it = ghostMap.keySet().iterator();
        while (it.hasNext()){
            Integer id = it.next();
            if (targetedGhostMap.get(id) == null){
                list.add(ghostMap.get(id));
            }
        }

        return list;
    }

    private static List<EnemyBuster> getEnemyBustersWithGhosts(HashMap<Integer, EnemyBuster> enemyBustersMap) {
        List<EnemyBuster> list = new ArrayList<>();
        Iterator<Integer> it = enemyBustersMap.keySet().iterator();
        while (it.hasNext()){
            Integer id = it.next();
            EnemyBuster enemyBuster = enemyBustersMap.get(id);
            if (enemyBuster.getState() == 1){
                list.add(enemyBuster);
            }
        }

        return list;
    }


    private static Ghost getNearestGhost(Buster buster, Collection<Ghost> ghosts) {
        Ghost ghost = null;
        int dist = Integer.MAX_VALUE;
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()){
            Ghost tmpGhost = it.next();
            int distance = calculateSqDistance(buster, tmpGhost);
            if (distance < dist){
                dist = distance;
                ghost = tmpGhost;
            }
        }

        return ghost;
    }

    private static EnemyBuster getNearestNonTargetedEnemyBuster(Buster buster, Collection<EnemyBuster> enemyBusters, HashMap<Integer, EnemyBuster> targetedEnemyBusters) {
        EnemyBuster enemyBuster = null;
        int dist = Integer.MAX_VALUE;
        Iterator<EnemyBuster> it = enemyBusters.iterator();
        while (it.hasNext()){
            EnemyBuster tmpEnemy = it.next();
            int distance = calculateSqDistance(buster, tmpEnemy);
            if (targetedEnemyBusters.get(tmpEnemy.getEntityId()) == null && distance < dist){
                dist = distance;
                enemyBuster = tmpEnemy;
            }
        }

        return enemyBuster;
    }

    private static EnemyBuster getNearestEnemyBuster(Buster buster, Collection<EnemyBuster> enemyBusters) {
        EnemyBuster enemyBuster = null;
        int dist = Integer.MAX_VALUE;
        Iterator<EnemyBuster> it = enemyBusters.iterator();
        while (it.hasNext()){
            EnemyBuster tmpEnemy = it.next();
            int distance = calculateSqDistance(buster, tmpEnemy);
            if (distance < dist){
                dist = distance;
                enemyBuster = tmpEnemy;
            }
        }

        return enemyBuster;
    }



    private static int calculateSqDistance(Entity e1, int x, int y) {
        return calculateSqDistance(e1.getX(), e1.getY(), x, y);
    }

    private static int calculateSqDistance(Entity e1, Entity e2) {
        return calculateSqDistance(e1.getX(), e1.getY(), e2.getX(), e2.getY());
    }

    private static int calculateSqDistance(int x1, int y1, int x2, int y2) {
        int deltaX = x1 - x2;
        int deltaY = y1 - y2;

        return deltaX * deltaX + deltaY * deltaY;
    }

    public static class Buster extends Entity{
        public Buster(int entityId, int entityType) {
            super(entityId, entityType);
        }

        @Override
        public void printParameters() {
            String stateSt = "idle";
            if (state == 1){
                stateSt = "carrying a ghost";
            }
            if (state == 2){
                stateSt = "stuned";
            }
            System.err.println("Buster " + entityId + "\n" +
                    "x: " + x + "\n" +
                    "y: " + y + "\n" +
                    "state: " + stateSt + "\n" +
                    "value: " + value);
        }

        public boolean isIdle() {
            return state == 0;
        }


    }

    public static class Ghost extends Entity{
        public Ghost(int entityId, int entityType) {
            super(entityId, entityType);
        }

        @Override
        public void printParameters() {
            System.err.println("Ghost " + entityId + "\n" +
                    "x: " + x + "\n" +
                    "y: " + y + "\n" +
                    "value: " + value);
        }
    }

    public static class EnemyBuster extends Entity{
        public EnemyBuster(int entityId, int entityType) {
            super(entityId, entityType);
        }

        @Override
        public void printParameters() {
            String stateSt = "idle";
            if (state == 1){
                stateSt = "carrying a ghost";
            }
            if (state == 2){
                stateSt = "stuned";
            }
            System.err.println("Enemy " + entityId + "\n" +
                    "x: " + x + "\n" +
                    "y: " + y + "\n" +
                    "state: " + stateSt + "\n" +
                    "value: " + value);
        }
    }

    public static class Home extends Entity{
        public Home(int x, int y) {
            super(-10, 10);
            this.x = x;
            this.y = y;
            this.value = 0;
        }

        public void ghostDelivered(){
            value++;
        }
    }

    public static class Entity{
        int entityId;
        int x;
        int y;
        int entityType;
        int state;
        int value;

        public Entity(int entityId, int entityType) {
            this.entityId = entityId;
            this.x = -1;
            this.y = -1;
            this.entityType = entityType;
            this.state = -1;
            this.value = -1;
        }

        public void updateParameters(int x, int y, int state, int value){
            this.x = x;
            this.y = y;
            this.state = state;
            this.value = value;
        }

        public void updateParametersLoud(int x, int y, int state, int value){
            updateParameters(x, y, state, value);
            printParameters();
        }

        public void printParameters(){}

        public int getEntityId() {
            return entityId;
        }

        public void setEntityId(int entityId) {
            this.entityId = entityId;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getEntityType() {
            return entityType;
        }

        public void setEntityType(int entityType) {
            this.entityType = entityType;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class Map{
        private HashMap<Integer, Checkpoint> targetedCheckpoints;
        private Checkpoint[][] checkpoints;
        private final int checkpointLength = 3110;
        private final int checkpointRadius = 1555;
        private final int checkpointHitDistance = 205;
        private final int checkpointsRows = 3;
        private final int checkpointsCols = 6;

        private final int chHitDistance = 42025;
        private final int chRadius = 2418025;
        private final int homeDist = 2560000;
        private final int ghostReachMax = 3097600;
        private final int ghostReachMin = 810000;

        public Map(int teamId) {
            targetedCheckpoints = new HashMap<>();
            checkpoints = new Checkpoint[checkpointsRows][checkpointsCols];
            if (teamId == 0){
                fillCheckpoints0();
            }
            else {
                fillCheckpoints1();
            }

            printCheckpoints();
        }

        public void clearTargetedCheckpoints(){
            targetedCheckpoints.clear();
        }

        public void targetCheckpoint(Checkpoint checkpoint){
            targetedCheckpoints.put(checkpoint.getEntityId(), checkpoint);
        }

        private void printCheckpoints() {
            for (int i = 0; i < checkpointsRows; i ++){
                String x = "";
                String y = "";
                for (int j = 0; j < checkpointsCols; j++){
                    Checkpoint checkpoint = checkpoints[i][j];
                    x += String.valueOf(checkpoint.getX()) + "  ";
                    y += String.valueOf(checkpoint.getY()) + "  ";
                }
                System.err.println(x);
                System.err.println(y);
            }
        }

        private void fillCheckpoints0() {
            int chkId = -10;
            for (int i = 0; i < checkpointsRows; i++){
                for (int j = 0; j < checkpointsCols; j++){
                    int x = checkpointRadius + j * checkpointLength;
                    int y = checkpointRadius + i * checkpointLength;

                    if (x > 16000) {
                        x = 16000;
                    }
                    if (y > 9000){
                        y = 9000;
                    }
                    chkId--;
                    Checkpoint checkpoint = new Checkpoint(chkId, x, y, false);
                    checkpoints[i][j] = checkpoint;
                }
            }
        }

        private void fillCheckpoints1() {
            int chkId = -10;
            for (int i = 0; i < checkpointsRows; i++){
                for (int j = 0; j < checkpointsCols; j++){
                    int x = 16000 - (checkpointRadius + j * checkpointLength);
                    int y = 9000 - (checkpointRadius + i * checkpointLength);

                    if (x < 0) {
                        x = 0;
                    }
                    if (y < 0){
                        y = 0;
                    }
                    chkId--;
                    Checkpoint checkpoint = new Checkpoint(chkId, x, y, false);
                    checkpoints[i][j] = checkpoint;
                }
            }
        }

        public void updateCheckpoints(Buster buster) {
            Checkpoint checkpoint = findCheckpoint(buster.getX(), buster.getY());
            if (checkpoint != null){
                int deltaX = buster.getX() - checkpoint.getX();
                int deltaY = buster.getY() - checkpoint.getY();
                int dist = calculateSqDistance(buster, checkpoint);
                if (dist <= chHitDistance){
                    checkpoint.setVisited(true);
                }
            }
            else {
                System.err.println("Unable to find checkpoint for buster " + String.valueOf(buster.getEntityId()));
            }
        }

        public Checkpoint findNonVisitedNearestCheckpoint(Buster buster){
            int x = buster.getX();
            int y = buster.getY();
            Checkpoint checkpoint = null;
            int dist = Integer.MAX_VALUE;
            for (int i = 0; i < checkpointsCols; i++){
                for (int j = 0; j < checkpointsRows; j++) {
                    Checkpoint tmp = checkpoints[j][i];
                    if (i == checkpointsCols - 1 && j == checkpointsRows - 1){
                        tmp.setVisited(true);
                    }
                    if (!tmp.isVisited() && !isCheckpointTargeted(tmp)){
                        int deltaX = x - tmp.getX();
                        int deltaY = y - tmp.getY();
                        int distTmp = (deltaX * deltaX + deltaY * deltaY);
                        if (distTmp < dist){
                            checkpoint = tmp;
                        }
                        if (distTmp > dist){
                            break;
                        }
                    }
                }
            }
            if (checkpoint == null){
                Random random = new Random();
                int i = random.nextInt(checkpointsRows - 1);
                int j = random.nextInt(checkpointsCols - 1);
                checkpoint = checkpoints[i][j];
                System.err.println("Returning random checkpoint: " + checkpoint.getX() + "," + checkpoint.getY());
            }
            return checkpoint;
        }

        private boolean isCheckpointTargeted(Checkpoint tmp) {
            Checkpoint checkpoint = targetedCheckpoints.get(tmp.getEntityId());
            return checkpoint != null;
        }

        private Checkpoint findCheckpoint(int x, int y) {
            Checkpoint checkpoint = null;
            for (int i = 0; i < checkpointsRows; i++){
                for (int j = 0; j < checkpointsCols; j++){
                    Checkpoint tmp = checkpoints[i][j];
                    int deltaX = x - tmp.getX();
                    int deltaY = y - tmp.getY();
                    int dist = deltaX * deltaX + deltaY * deltaY;
                    if (dist <= chRadius){
                        checkpoint = tmp;
                        return checkpoint;
                    }
                }
            }
            return checkpoint;
        }

        public boolean isInReach(Entity e1, Entity e2, int dist){
            int deltaX = e1.getX() - e2.getX();
            int deltaY = e1.getY() - e2.getY();
            System.err.println("Is in reach: " + String.valueOf(e1.entityId) + " " + String.valueOf(e2.entityId) +
            " " + String.valueOf(deltaX) + " " + String.valueOf(deltaY));
            return (deltaX * deltaX + deltaY * deltaY ) <= dist;
        }

        public int getChHitDistance() {
            return chHitDistance;
        }

        public int getChRadius() {
            return chRadius;
        }

        public int getHomeDist() {
            return homeDist;
        }

        public Checkpoint[][] getCheckpoints() {
            return checkpoints;
        }

        public int getCheckpointLength() {
            return checkpointLength;
        }

        public int getCheckpointRadius() {
            return checkpointRadius;
        }

        public int getCheckpointHitDistance() {
            return checkpointHitDistance;
        }

        public int getCheckpointsRows() {
            return checkpointsRows;
        }

        public int getCheckpointsCols() {
            return checkpointsCols;
        }

        public int getGhostReachMax() {
            return ghostReachMax;
        }

        public int getGhostReachMin() {
            return ghostReachMin;
        }
    }

    public static class Checkpoint extends Entity{
        private int x;
        private int y;
        private boolean visited;

        public Checkpoint(int checkpointId, int x, int y, boolean visited) {
            super(checkpointId, 11);
            this.x = x;
            this.y = y;
            this.visited = visited;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }
    }

    public static class StunCounter{
        private int currentTurn;
        private HashMap<Integer, Integer> stuns;

        public StunCounter() {
            currentTurn = 0;
            stuns = new HashMap<>();

        }

        public void updateTurn(){
            currentTurn++;
        }

        public int getCurrentTurn() {
            return currentTurn;
        }

        public void updateBuster(Buster buster){
            Integer stunTurn = stuns.get(buster.getEntityId());
            if (stunTurn == null){
                stuns.put(buster.getEntityId(), currentTurn);
            }
            stunTurn = stuns.get(buster.getEntityId());
        }

        public void useStun(Buster buster){
            stuns.put(buster.getEntityId(), currentTurn + 20);
        }

        public boolean isStunAvailable(Buster buster){
            return stuns.get(buster.getEntityId()) <= currentTurn;
        }
    }
}

