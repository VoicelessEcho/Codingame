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
        Home home = new Home(0, 0);
        if (myTeamId == 1){
            home.setX(16000);
            home.setY(9000);
        }
        Map map = new Map(myTeamId);
        // game loop
        while (true) {
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
                }
                else {
                    if (map.isInReach(buster, home, map.getHomeDist())){
                        System.out.println("RELEASE");
                    }
                    else {
                        System.out.println("MOVE " + String.valueOf(home.getX()) + " " + String.valueOf(home.getY()));
                    }
                }

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

                System.out.println("MOVE 8000 4500"); // MOVE x y | BUST id | RELEASE
            }
        }
    }



    private static Ghost getNearestGhost(Buster buster, Collection<Ghost> ghosts) {
        Ghost ghost = null;
        int dist = Integer.MAX_VALUE;
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()){
            Ghost tmpGhost = it.next();
            //int distance = getVectorQ()
        }

        return ghost;
    }

    public static class Buster extends Entity{
        public Buster(int entityId, int entityType) {
            super(entityId, entityType);
        }

        @Override
        public void printParameters() {
            String stateSt = "idle";
            if (state != 0){
                stateSt = "carrying a ghost";
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
            if (state != 0){
                stateSt = "carrying a ghost";
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
            checkpoints = new Checkpoint[checkpointsRows][checkpointsCols];
            if (teamId == 0){
                fillCheckpoints0();
            }
            else {
                fillCheckpoints1();
            }

            printCheckpoints();
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
                    Checkpoint checkpoint = new Checkpoint(x, y, false);
                    checkpoints[i][j] = checkpoint;
                }
            }
        }

        private void fillCheckpoints1() {
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
                    Checkpoint checkpoint = new Checkpoint(x, y, false);
                    checkpoints[i][j] = checkpoint;
                }
            }
        }

        public void updateCheckpoints(Buster buster) {
            Checkpoint checkpoint = findCheckpoint(buster.getX(), buster.getY());
            if (checkpoint != null){
                int deltaX = buster.getX() - checkpoint.getX();
                int deltaY = buster.getY() - checkpoint.getY();
                int dist = deltaX * deltaX + deltaY * deltaY;
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
                    if (!checkpoint.isVisited()){
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

        private Checkpoint findCheckpoint(int x, int y) {
            Checkpoint checkpoint = null;
            for (int i = 0; i < checkpointsRows; i++){
                for (int j = 0; j < checkpointsCols; j++){
                    Checkpoint tmp = checkpoints[i][j];
                    int deltaX = x - checkpoint.getX();
                    int deltaY = y - checkpoint.getY();
                    int dist = deltaX * deltaX + deltaY * deltaY;
                    if (dist <= chRadius){
                        checkpoint = tmp;
                        return checkpoint;
                    }
                }
            }
            return null;
        }

        public boolean isInReach(Entity e1, Entity e2, int dist){
            int deltaX = e1.getX() - e2.getX();
            int deltaY = e1.getY() - e2.getY();

            return (deltaX * deltaX + deltaY * deltaY ) >= dist;
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

    public static class Checkpoint{
        private int x;
        private int y;
        private boolean visited;

        public Checkpoint(int x, int y, boolean visited) {
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
}

