package botch;

import botch.command.*;
import botch.entities.*;
import botch.enums.CellType;
import botch.enums.Direction;


import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;
    private Worm opponentCurrentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.opponentCurrentWorm = getEnemyCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private Worm getEnemyCurrentWorm(GameState gameState){
        return Arrays.stream(gameState.opponents[0].worms)
                .filter(enWorm -> enWorm.id == gameState.opponents[0].currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        if(inDanger()){
            Cell cellToMove = dodgeGaming(constructDangerCell(5, opponentCurrentWorm.position));
            return new MoveCommand(cellToMove.x, cellToMove.y);
        }

        if(currentWorm.bananas != null){
            if(currentWorm.bananas.count > 0 ){
                List<Worm> enemyWorm = getEnemyWormInRange(currentWorm.bananas.range);
                if(enemyWorm.size() !=0 && !InRadius(enemyWorm.get(0).position, currentWorm.bananas.damageRadius)){
                    return new BananaBombCommand(enemyWorm.get(0).position);
                }
            }
        }

        if(currentWorm.snowball != null){
            if(currentWorm.snowball.count > 0 ){
                List<Worm> enemyWorm = getEnemyWormInRange(currentWorm.snowball.range);
                if(enemyWorm.size() !=0 && enemyWorm.get(0).roundUntilUnfrozen <=1 && !InRadius(enemyWorm.get(0).position, currentWorm.snowball.freezeRadius)){
                    return new SnowballCommand(enemyWorm.get(0).position);
                }
            }
        }


        List<Worm> enemyWorms = getEnemyWormInRange();
        if (enemyWorms.size() >0) {
            List<Worm> friendlyWorm = getFriendlyWormInRange();
            Worm wormToShoot = getLowestHP(enemyWorms);
            Direction direction = resolveDirection(currentWorm.position, wormToShoot.position);
            if(friendlyWorm.size()==0 || mayShoot(friendlyWorm,direction)){
                return new ShootCommand(direction);
            }
        }

        Position target = new Position();
        target.x = opponentCurrentWorm.position.x;
        target.y = opponentCurrentWorm.position.y;
        Position path = getPath(currentWorm.position, target);
        Cell blocktomove = gameState.map[path.y][path.x];
        if(blocktomove.type == CellType.AIR && !thereIsWorm(blocktomove)){
            return new MoveCommand(blocktomove.x, blocktomove.y);
        }else if(blocktomove.type == CellType.AIR && thereIsWorm(blocktomove)){
            blocktomove.x = miror(path, currentWorm).x;
            blocktomove.y = miror(path, currentWorm).y;
            return new MoveCommand(blocktomove.x, blocktomove.y);
        }
        else if (blocktomove.type == CellType.DIRT){
            return new DigCommand(blocktomove.x, blocktomove.y);
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health >0) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<Worm> getEnemyWormInRange() {

        List<Worm> listEnemyWorm = new ArrayList<>();

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                listEnemyWorm.add(enemyWorm);
            }
        }
        return listEnemyWorm;
    }

    private List<Worm> getFriendlyWormInRange(){
        List<Worm> listFriendlyWorm = new ArrayList<>();

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm friendlyWorm : gameState.myPlayer.worms) {
            String enemyPosition = String.format("%d_%d", friendlyWorm.position.x, friendlyWorm.position.y);
            if (cells.contains(enemyPosition) && friendlyWorm.health > 0) {
                listFriendlyWorm.add(friendlyWorm);
            }
        }
        return listFriendlyWorm;
    }

    private Boolean mayShoot(List<Worm> friendly, Direction direction){
        for( Worm w : friendly){
            if(resolveDirection(currentWorm.position, w.position) == direction){
                return false;
            }
        }
        return true;
    }

    private Worm getLowestHP(List<Worm> worms){
        Worm lowest = new Worm();
        int Hp = 200;
        for(Worm w: worms){
            if(w.health < Hp && w.health>0){
                lowest = w;
            }
        }
        return lowest;
    }

    private List<Worm> getEnemyWormInRange(int range){
        List<Worm> listWorm = new ArrayList<>();
        for(Worm enemy : opponent.worms){
            if(euclideanDistance(currentWorm.position.x, currentWorm.position.y,enemy.position.x,enemy.position.y)< range){
                listWorm.add(enemy);
            }
        }
        return listWorm;
    }

    private Boolean InRadius(Position position, int radius){
        int x = position.x;
        int y = position.y;
        for (int i = x - radius; i <= x+radius; i++){
            for(int j = y - radius; j <= y+radius; j++){
                if(Math.abs(x-i) + Math.abs(y-j)<= radius){
                    for(Worm w : gameState.myPlayer.worms){
                        if(w.position.x == i && w.position.y == j){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Worm getNearestOpponentWorm(){
        Worm w = new Worm();
        for(Worm enemyworm : opponent.worms){
            int dist=99;
            if(euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyworm.position.x, enemyworm.position.y)< dist && enemyworm.health>0){
                w = enemyworm;
            }
        }
        return w;
    }

    private Position getPath(Position origin, Position destination){
        // Mengembalikan posisi cell path untuk mencapai cell destination
        Position path = new Position();
        path.x = origin.x;
        path.y = origin.y;
        if(origin.x != destination.x || origin.y != destination.y) {
            int x = destination.x - origin.x;
            int y = destination.y - origin.y;
            if (x > 0) {
                path.x = path.x + 1;
            } else if (x < 0) {
                path.x = path.x - 1;
            }

            if (y > 0){
                path.y = path.y + 1;
            } else if(y < 0){
                path.y = path.y - 1;
            }
        }
        return path;
    }

    private Boolean thereIsWorm(Cell cell){
        for (MyWorm w : gameState.myPlayer.worms){
            if(w.position.x == cell.x && w.position.y == cell.y && w.health>0){
                return true;
            }
        }

        for (Worm ew : opponent.worms){
            if(ew.position.x == cell.x && ew.position.y == cell.y && ew.health>0){
                return true;
            }
        }

        return false;
    }

    private Position miror(Position position, Worm worm){
        int x = worm.position.x - position.x;
        int y = worm.position.y - position.y;
        Position miror = new Position();
        miror.x = x * -1;
        miror.y = y;
        if( gameState.map[miror.y][miror.x].type != CellType.AIR){
            miror.x = x;
            miror.y = y * -1;
        }
        return miror;
    }


    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private List<Cell> getSafeCell(List<Cell> dangerCell, Worm dodgeWorm){
        List<Cell> safeCell = getSurroundingCells(dodgeWorm.position.x, dodgeWorm.position.y);
        for(Cell c : dangerCell){
            safeCell.remove(c);
        }

        safeCell.removeIf(safe -> safe.type != CellType.AIR);
        return safeCell;
    }

    private Boolean inDanger(){

        if(currentWorm.health < 14 && (gameState.myPlayer.score > gameState.opponents[0].score || currentWorm.health < opponentCurrentWorm.health)){
            List<Cell> dangerCell = constructDangerCell(5, opponentCurrentWorm.position);
            for(Cell c : dangerCell){
                if(currentWorm.position.x == c.x && currentWorm.position.y == c.y){
                    return true;
                }
            }
        }

        return false;
    }

    private Cell dodgeGaming(List<Cell> dangerCell){
        List<Cell> safeCell = getSafeCell(dangerCell, currentWorm);
        if(!safeCell.isEmpty()){    // ada cell untuk kabur
            int idx = random.nextInt(safeCell.size());
            return safeCell.get(idx);
        }
        else{                       // you're dead
            List<Cell> randommove = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
            randommove.removeIf(c -> c.type != CellType.AIR);
            randommove.removeIf(this::thereIsWorm);
            return randommove.get(random.nextInt(randommove.size()));
        }

    }

    private List<Cell> constructDangerCell(int range, Position enemyPosition) {
        List<Cell> dangerCell = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = enemyPosition.x + (directionMultiplier * direction.x);
                int coordinateY = enemyPosition.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(enemyPosition.x, enemyPosition.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                dangerCell.add(cell);
            }
        }

        return dangerCell;
    }
}
