import java.util.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {
    public static HashMap<Integer, Buster> bustersMap = new HashMap<>();
    public static HashMap<Integer, Ghost> ghostsMap = new HashMap<>();
    public static HashMap<Integer, EnemyBuster> enemyBustersMap = new HashMap<>();
    public static HashMap<Integer, Ghost> targetedGhostsMap = new HashMap<>();
    public static HashMap<Integer, EnemyBuster> targetedEnemyBusters = new HashMap<>();
    public static HashMap<Integer, Checkpoint> targetedCheckpointsMap = new HashMap<>();

    public static int homeDistSQR = 2560000;
    public static int viewDistSQR = 4840000;
    public static int ghostMove = 400;
    public static int busterMove = 800;
    public static int ghostMoveSQR = 160000;
    public static int busterMoveSQR = 640000;
    public static int effectiveBusterHitSQR = 921600;  //(1760-800)^2
    public static int effectiveGhostHitSQR = 1849600;  //(1760-400)^2
    public static int closestGhostHitSQR = 810000;  //900^2
    public static int checkpointHitSQR = 219024;//168100; //410
    public static int checkpointLength = 2260;//1550; //3110;
    public static int checkpointRadius = 1130;//775;  //1555;
    public static int checkpointRadiusSQR = 1276900;//600625;
    public static int checkpointsRows = 6;
    public static int checkpointsCols = 11;


    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right

        Home home = new Home(0, 0);
        Home enemyBase = new Home(16000, 9000);
        if (myTeamId == 1){
            home.setX(16000);
            home.setY(9000);
            enemyBase.setX(0);
            enemyBase.setY(0);
        }
        Map map = new Map(myTeamId, home, enemyBase);
        StunCounter stunCounter = new StunCounter();

        // game loop
        while (true) {
            stunCounter.updateTurn();

            targetedEnemyBusters.clear();
            targetedGhostsMap.clear();
            ghostsMap.clear();
            enemyBustersMap.clear();


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
                DecisionMaker decisionMaker = new DecisionMaker();
                System.out.println(decisionMaker.getDecision(buster, home, enemyBase, map, stunCounter));

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

                //System.out.println("MOVE 8000 4500"); // MOVE x y | BUST id | RELEASE
            }
        }
    }

    private static Checkpoint findNonVisitedNearestCheckpoint(Buster buster, Map map) {
        Checkpoint checkpoint = null;
        List<Checkpoint> nonVisitedCheckpoints = findNonVisitedCheckpoints(map.getCheckpoints());
        if (nonVisitedCheckpoints.size() != 0){
            checkpoint = findNearestCheckpoint(buster, nonVisitedCheckpoints);
        }
        return checkpoint;
    }

    private static Checkpoint findNearestCheckpoint(Entity entity, Collection<Checkpoint> entityList){
        Checkpoint e = null;

        Iterator<Checkpoint> it = entityList.iterator();
        e = it.next();
        int dist = calculateSqDistance(entity, e);
        while (it.hasNext()){
            Checkpoint tmp = it.next();
            int tmpDist = calculateSqDistance(entity, tmp);
            if (tmpDist < dist){
                dist = tmpDist;
                e = tmp;
            }
        }
        return e;
    }

    private static List<Checkpoint> findNonVisitedCheckpoints(Collection<Checkpoint> checkpoints) {
        List<Checkpoint> nonVisitedList = new ArrayList<>();
        Iterator<Checkpoint> it = checkpoints.iterator();
        while (it.hasNext()){
            Checkpoint c = it.next();
            if (!c.isVisited()){
                nonVisitedList.add(c);
            }
        }
        return nonVisitedList;
    }

    private static List<Ghost> getGhostsInReach(Buster buster, Collection<Ghost> ghosts, Map map){
        List<Ghost> list = new ArrayList<>();
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()){
            Ghost g = it.next();
            if (!isInDistance(buster, g, closestGhostHitSQR) && isInDistance(buster, g, effectiveGhostHitSQR)){
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

    private static List<EnemyBuster> getEnemyBustersInDistance(Buster buster, Collection<EnemyBuster> enemyBusters, int distSQR){
        List<EnemyBuster> enemyBustersList = new ArrayList<>();
        Iterator<EnemyBuster> it = enemyBusters.iterator();
        while (it.hasNext()){
            EnemyBuster enemy = it.next();
            if (isInDistance(buster, enemy, distSQR)){
                enemyBustersList.add(enemy);
            }
        }
        return enemyBustersList;
    }

    private static List<EnemyBuster> getEnemyBustersWithGhosts(Collection<EnemyBuster> enemyBusters){
        List<EnemyBuster> enemyBustersList = new ArrayList<>();
        Iterator<EnemyBuster> it = enemyBusters.iterator();
        while (it.hasNext()){
            EnemyBuster enemy = it.next();
            if (enemy.getState() == 1){
                enemyBustersList.add(enemy);
            }
        }
        return enemyBustersList;
    }

    private static List<EnemyBuster> getEnemyBustersTrapping(Collection<EnemyBuster> enemyBusters){
        List<EnemyBuster> enemyBustersList = new ArrayList<>();
        Iterator<EnemyBuster> it = enemyBusters.iterator();
        while (it.hasNext()){
            EnemyBuster enemy = it.next();
            if (enemy.getState() == 3){
                enemyBustersList.add(enemy);
            }
        }
        return enemyBustersList;
    }

    private static List<EnemyBuster> getNonTargetedEnemyBusters(Collection<EnemyBuster> enemyBusters){
        List<EnemyBuster> enemyBustersList = new ArrayList<>();
        Iterator<EnemyBuster> it = enemyBusters.iterator();
        while (it.hasNext()){
            EnemyBuster enemy = it.next();
            if (targetedEnemyBusters.get(enemy.getEntityId()) == null){
                enemyBustersList.add(enemy);
            }
        }
        return enemyBustersList;
    }


    public static int calculateSqDistance(Entity e1, int x, int y) {
        return calculateSqDistance(e1.getX(), e1.getY(), x, y);
    }

    public static int calculateSqDistance(Entity e1, Entity e2) {
        return calculateSqDistance(e1.getX(), e1.getY(), e2.getX(), e2.getY());
    }

    public static int calculateSqDistance(int x1, int y1, int x2, int y2) {
        int deltaX = x1 - x2;
        int deltaY = y1 - y2;

        return deltaX * deltaX + deltaY * deltaY;
    }

    public static boolean isInDistance(Entity e, Entity f, int distSQR){
        return isInDistance(e.getX(), e.getY(), f.getX(), f.getY(), distSQR);
    }

    public static boolean isInDistance(Entity e, int x, int y, int distSQR){
        return isInDistance(e.getX(), e.getY(), x, y, distSQR);
    }

    public static boolean isInDistance(int x1, int y1, int x2, int y2, int distSQR){
        int deltaX = x1 - x2;
        int deltaY = y1 - y2;
        return (deltaX * deltaX + deltaY * deltaY) <= distSQR;
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
        public boolean isCarrying() {
            return state == 1;
        }
        public boolean isStuned() {
            return state == 2;
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


    public static class DecisionMaker{
        public String getDecision(Buster buster, Home home, Home enemyBase, Map map, StunCounter stunCounter){
            String action = "";
            switch (buster.getState()){
                case 0:
                    //idle
                    if (stunCounter.isStunAvailable(buster)) {
                        List<EnemyBuster> enemyBustersInDistance = getEnemyBustersInDistance(buster, enemyBustersMap.values(), effectiveBusterHitSQR);
                        List<EnemyBuster> enemyBustersWithGhosts = getEnemyBustersWithGhosts(enemyBustersInDistance);
                        List<EnemyBuster> enemyBustersNonTargeted = getNonTargetedEnemyBusters(enemyBustersWithGhosts);
                        if (enemyBustersNonTargeted.size() != 0) {
                            EnemyBuster enemy = getNearestEnemyBuster(buster, enemyBustersNonTargeted);
                            targetedEnemyBusters.put(enemy.getEntityId(), enemy);
                            action =  stunEnemy(buster, enemy.getEntityId());
                        } else {
                            List<EnemyBuster> enemyBustersTrapping = getEnemyBustersTrapping(enemyBustersNonTargeted);
                            if (enemyBustersTrapping.size() != 0) {
                                EnemyBuster enemy = getNearestEnemyBuster(buster, enemyBustersNonTargeted);
                                targetedEnemyBusters.put(enemy.getEntityId(), enemy);
                                action =  stunEnemy(buster, enemy.getEntityId());
                            } else {
                                action =  findGhost(buster, map);
                            }
                        }
                    }
                    else {
                        action = findGhost(buster, map);
                    }
                    break;
                case 1:
                    //carrying
                    if (isInDistance(buster, home, homeDistSQR)){
                        action = releaseGhost(buster);
                    }
                    else {
                        action = moveHome(buster, home);
                    }
                    break;
                case 2:
                    //stunned
                    action = tryToMove(buster, home);
                    break;
                case 3:
                    //busting
                    action = bustGhost(buster, ghostsMap.get(buster.getValue()));
                    break;
            }

            return action;
        }

        private String findGhost(Buster buster, Map map) {
            if (ghostsMap.size() != 0){
                //TODO non compete logic
                Ghost ghost = getNearestGhost(buster, ghostsMap.values());
                if (isInDistance(buster, ghost, closestGhostHitSQR)){
                    return moveAwayFromEntity(buster, ghost, 400) + " I'm too close";
                }
                else {
                    if (isInDistance(buster, ghost, effectiveGhostHitSQR)){
                        return bustGhost(buster, ghost);
                    }
                    else {
                        return moveToEntity(buster, ghost);
                    }
                }
            }
            else {
                return findCheckpoint(buster, map);
            }
        }

        private String moveAwayFromEntity(Buster buster, Entity entity, int i) {
            //TODO move away logic
            int x1 = buster.getX();
            int y1 = buster.getY();
            int x2 = entity.getX();
            int y2 = entity.getY();

            if (x1 >= x2 && y1 >= y2 ){
                return moveToCoordinates(buster, x2 + 637, y2 + 637) + " 1 ";
            }
            else {
                if (x1 <= x2 && y1 >= y2 ){
                    return moveToCoordinates(buster, x2 - 637, y2 + 637 ) + " 2 ";
                }
                else {
                    if (x1 >= x2 && y1 <= y2 ){
                        return moveToCoordinates(buster, x2 + 637, y2 - 637) + " 3 ";
                    }
                    else {
                        if (x1 <= x2 && y1 <= y2 ){
                            return moveToCoordinates(buster, x2 - 637, y2 - 637) + " 4 ";
                        }
                    }
                }
            }

            return null;
        }

        private String findCheckpoint(Buster buster, Map map) {
            List<Checkpoint> checkpoints = map.getCheckpoints();
            List<Checkpoint> notVisitedCheckpoints = getNonVisitedCheckpoints(checkpoints);
            List<Checkpoint> nonTargetedCheckpoints = getNonTargetedCheckpoints(notVisitedCheckpoints);
            if (nonTargetedCheckpoints.size() != 0){
                Checkpoint checkpoint = getNearestCheckpoint(buster, nonTargetedCheckpoints);
                targetedCheckpointsMap.put(checkpoint.getEntityId(), checkpoint);
                return moveToEntity(buster, checkpoint);
            }
            else {
                Checkpoint checkpoint = map.getRandomCheckpoint();
                targetedCheckpointsMap.put(checkpoint.getEntityId(), checkpoint);
                return moveToEntity(buster, checkpoint);
            }
        }

        private Checkpoint getNearestCheckpoint(Buster buster, Collection<Checkpoint> checkpoints) {
            List<Checkpoint> chList = new ArrayList<>();
            chList.addAll(checkpoints);
            Checkpoint ch = chList.get(0);
            int dist = calculateSqDistance(buster, ch);

            for (int i = 1; i < chList.size(); i++){
                Checkpoint tmp = chList.get(i);
                int tmpDist = calculateSqDistance(buster, tmp);
                if (dist > tmpDist){
                    ch = tmp;
                }
            }
            return ch;
        }

        private List<Checkpoint> getNonTargetedCheckpoints(Collection<Checkpoint> checkpoints) {
            List<Checkpoint> list = new ArrayList<>();
            Iterator<Checkpoint> it = checkpoints.iterator();
            while (it.hasNext()){
                Checkpoint checkpoint = it.next();
                if (targetedCheckpointsMap.get(checkpoint.getEntityId()) == null){
                    list.add(checkpoint);
                }
            }

            return list;        }

        private List<Checkpoint> getNonVisitedCheckpoints(Collection<Checkpoint> checkpoints) {
            List<Checkpoint> list = new ArrayList<>();
            Iterator<Checkpoint> it = checkpoints.iterator();
            while (it.hasNext()){
                Checkpoint checkpoint = it.next();
                if (!checkpoint.isVisited()){
                    list.add(checkpoint);
                }
            }

            return list;
        }

        private String moveToCoordinates(Buster buster, int x, int y){
            return "MOVE " + String.valueOf(x) + " " +String.valueOf(y);// + " Moving to " + String.valueOf(x) + ";" + String.valueOf(y);
        }

        private String moveToEntity(Buster buster, Entity entity){
            return "MOVE " + String.valueOf(entity.getX()) + " " + String.valueOf(entity.getY());// + " Moving to e" + String.valueOf(entity.getEntityId());
        }

        private String stunEnemy(Buster buster, int enemyId){
            return "STUN " + String.valueOf(enemyId);// + " Stunning " + String.valueOf(enemyId);
        }

        private String bustGhost(Buster buster, Ghost ghost) {
            return "BUST " + String.valueOf(ghost.entityId) + " Bust in " + String.valueOf(ghost.getState());
        }

        private String moveHome(Buster buster, Home home) {
            return "MOVE " + String.valueOf(home.getX()) + " " + String.valueOf(home.getY());// + " Going home " + String.valueOf(buster.getEntityId());
        }

        private String releaseGhost(Buster buster) {
            return "RELEASE";// + " " + String.valueOf(buster.getEntityId()) +"Releasing " + String.valueOf(buster.getValue());
        }

        private String tryToMove(Buster buster, Home home) {
            return "MOVE " + String.valueOf(home.getX()) + " " + String.valueOf(home.getY());// + " Stunned " + String.valueOf(buster.getEntityId());
        }
    }



    public static class Map{
        private int[] row0 = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1};
        private int[] row1 = {0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        private int[] row2 = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1};
        private int[] row3 = {0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        private int[] row4 = {1, 0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0, 1};
        private int[] row5 = {0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        private int[] row6 = {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1};
        private int[][] pomts = {row0, row1, row2, row3, row4, row5, row6};

        private LinkedHashMap<Integer, Checkpoint> checkpointsMap = new LinkedHashMap<>();
        private Checkpoint[][] checkpoints;

        public Map(int teamId, Home home, Home enemyBase) {
            checkpoints = new Checkpoint[checkpointsRows][checkpointsCols];
            if (teamId == 0){
                fillCheckpoints0(home, enemyBase);
            }
            else {
                fillCheckpoints1(home, enemyBase);
            }

            printCheckpoints();
        }
        private void printCheckpoints() {
            String x = "X: ";
            String y = "Y: ";
            Iterator<Checkpoint> it = checkpointsMap.values().iterator();
            while (it.hasNext()){
                Checkpoint checkpoint = it.next();
                x += String.valueOf(checkpoint.getX()) + " ; ";
                y += String.valueOf(checkpoint.getY()) + " ; ";
            }


            /*for (int i = 0; i < checkpointsRows; i ++){
                String x = "";
                String y = "";
                for (int j = 0; j < checkpointsCols; j++){
                    Checkpoint checkpoint = checkpoints[i][j];
                    if (checkpoint != null) {
                        x += String.valueOf(checkpoint.getX()) + "  ";
                        y += String.valueOf(checkpoint.getY()) + "  ";
                    }
                    else {
                        x = "null";
                        y = "null";
                    }
                }
                System.err.println(x);
                System.err.println(y);
            }*/
        }
        private void fillCheckpoints0(Home home,Home enemyBase) {
            printHeat();

            int id = -10;
            for (int i = 0; i < 7; i++){
                for (int j = 0; j < 13; j++){
                    int h = heat[i][j];
                    System.err.println(i + " " + j);
                    if (h != 0){
                        id--;
                        int x = checkpointRadius * (i + 1);
                        int y = checkpointRadius * (j + 1);
                        if (x > 16000) {
                            x = 16000;
                        }
                        if (y > 9000){
                            y = 9000;
                        }
                        Checkpoint checkpoint = new Checkpoint(
                                id,
                                x,
                                y,
                                false);
                        checkpointsMap.put(id, checkpoint);
                    }
                }
            }
            /*int chkId = -10;
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
                    if (isInDistance(checkpoint, home, homeDistSQR) || isInDistance(checkpoint, enemyBase, homeDistSQR)){
                        System.err.println("Setting checkpoint " + String.valueOf(checkpoint.getEntityId()) + " to null" );
                        checkpoint = null;
                    }
                    checkpoints[i][j] = checkpoint;
                }
            }*/
        }

        private void printHeat() {

            for (int i = 0; i < 7; i++){
                String line = String.valueOf(i) + ": ";
                for (int j = 0; j < 13; j++){
                    line += String.valueOf(heat[i][j]);
                }
                System.err.println(line);
            }
        }

        private void fillCheckpoints1(Home home, Home enemyBase) {
            int id = -10;
            for (int i = 0; i < 7; i++){
                for (int j = 0; j < 13; j++){
                    int h = heat[i][j];
                    if (h != 0){
                        id--;
                        int x = 16000 - checkpointRadius * (i + 1);
                        int y = 9000 - checkpointRadius * (j + 1);
                        if (x < 0) {
                            x = 0;
                        }
                        if (y < 0){
                            y = 0;
                        }
                        Checkpoint checkpoint = new Checkpoint(
                                id,
                                x,
                                y,
                                false);
                        checkpointsMap.put(id, checkpoint);
                    }
                }
            }

            /*int chkId = -10;
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
                    if (isInDistance(checkpoint, home, homeDistSQR) || isInDistance(checkpoint, enemyBase, homeDistSQR)){
                        System.err.println("Setting checkpoint " + String.valueOf(checkpoint.getEntityId()) + " to null" );
                        checkpoint = null;
                    }
                    checkpoints[i][j] = checkpoint;
                }
            }*/
        }
        public Checkpoint findCurrentCheckpoint(Buster buster){
            for (int i = 0; i < checkpointsRows; i++ ){
                for (int j = 0; j < checkpointsCols; j++){
                    Checkpoint tmp = checkpoints[i][j];
                    if (tmp != null && isInDistance(buster, tmp, checkpointRadiusSQR)){
                        return tmp;
                    }
                }
            }
            return null;
        }


        public void updateCheckpoints(Buster buster) {
            Checkpoint checkpoint = findCurrentCheckpoint(buster);
            if (checkpoint != null){
                if (isInDistance(buster, checkpoint, checkpointHitSQR)){
                    checkpoint.setVisited(true);
                }
            }
            else {
                System.err.println("Unable to find checkpoint for buster " + String.valueOf(buster.getEntityId()));
            }
        }


        public List<Checkpoint> getCheckpoints() {
            List<Checkpoint> checkpointList = new ArrayList<>();
            for (int i = 0; i < checkpointsRows; i++){
                for (int j = 0; j < checkpointsCols; j++){
                    Checkpoint c = checkpoints[i][j];
                    if (c != null){
                        checkpointList.add(c);
                    }
                }
            }
            return checkpointList;
        }

        public Checkpoint getRandomCheckpoint() {
            Random random = new Random();
            boolean gen = true;
            int i = 0;
            int j = 0;
            while (gen){
                i = random.nextInt(checkpointsRows);
                j = random.nextInt(checkpointsCols);
                if (checkpoints[i][j] != null){
                    gen = false;
                }
            }
            return checkpoints[i][j];
        }
    }

    public static class Checkpoint extends Entity{
        private int arrX[];
        private int arrY[];
        private int x;
        private int y;
        private boolean visited;
        private int type;

        public Checkpoint(int checkpintId, int x1, int x2, int x3, int y1, int y2, int y3, boolean visited){
            super(checkpintId, 11);

            this.x = (x1 + x2 + x3)/3;
            this.y = (y1 + y2 + y3)/3;



            arrX = new int[]{x1, x2, x3};
            arrY = new int[]{y1, y2, y3};
            this.visited = visited;
            this.type = calcType(x1, x2, x3, y1, y2, y3);

        }

        private int calcType(int x1, int x2, int x3, int y1, int y2, int y3) {
            if (x1 > x2 && x1 < x3 && y1 > y2 && y1 > y3){
                return 0;
            }
            if (x1 < x2 && x1 < x3 && y1 > y2 && y1 < y3){
                return 1;
            }
            if (x1 < x2 && x1 > x3 && y1 < y2 && y1 < y3){
                return 2;
            }
            if (x1 > x2 && x1 > x3 && y1 < y2 && y1 > y3){
                return 3;
            }
            return 0;
        }


        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        public boolean isInside(Entity e){
            return isInside(e.getX(), e.getY());
        }

        public boolean isInside(int x, int y){
            boolean inside = false;
            int x1 = arrX[0];
            int x2 = arrX[1];
            int x3 = arrX[2];
            int y1 = arrY[0];
            int y2 = arrY[1];
            int y3 = arrY[2];



            return inside;
        }

        /*float sign (fPoint p1, fPoint p2, fPoint p3)
        {
            return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
        }

        bool PointInTriangle (fPoint pt, fPoint v1, fPoint v2, fPoint v3)
        {
            bool b1, b2, b3;

            b1 = sign(pt, v1, v2) < 0.0f;
            b2 = sign(pt, v2, v3) < 0.0f;
            b3 = sign(pt, v3, v1) < 0.0f;

            return ((b1 == b2) && (b2 == b3));
        }*/


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
