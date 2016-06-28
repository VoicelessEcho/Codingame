import java.util.*;
import java.io.*;
import java.math.*;

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
            for (int i = 0; i < bustersPerPlayer; i++) {

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

                System.out.println("MOVE 8000 4500"); // MOVE x y | BUST id | RELEASE
            }
        }
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
    }

    public static class Map{

    }

    public static class MapCell{

    }
}

