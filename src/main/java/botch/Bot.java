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
        /* RETREAT OR DODGE */
        if(inDanger()){
            Cell cellToMove = dodgeGaming(constructDangerCell(5, opponentCurrentWorm.position));
            return new MoveCommand(cellToMove.x, cellToMove.y);
        }

        /* BANANA-ING */
        if(currentWorm.bananas != null){
            if(currentWorm.bananas.count > 0 ){
                List<Worm> enemyWorm = getEnemyWormInRange(currentWorm.bananas.range);
                Worm wormToBomb = getLowestHP(enemyWorm);
                if(enemyWorm.size() !=0 && !InRadius(wormToBomb.position, currentWorm.bananas.damageRadius)){
                    return new BananaBombCommand(enemyWorm.get(0).position);
                }
            }
        }

        /* SNOWBALL-ING */
        if(currentWorm.snowball != null){
            if(currentWorm.snowball.count > 0 ){
                List<Worm> enemyWorm = getEnemyWormInRange(currentWorm.snowball.range);
                if(enemyWorm.size() !=0 && enemyWorm.get(0).roundsUntilUnfrozen <=1 && !InRadius(enemyWorm.get(0).position, currentWorm.snowball.freezeRadius)){
                    return new SnowballCommand(enemyWorm.get(0).position);
                }
            }
        }

        /* SHOOTING */
        List<Worm> enemyWormsInRange = getEnemyWormInRange();
        if (enemyWormsInRange.size() >0) {
            return shootEnemy(enemyWormsInRange);
        }

        /* DIGGING IF THERE IS DIRT SURROUNDING */
        List<Cell> surrounding = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        Cell toDig = getDirtCell(surrounding);
        if(toDig != null){
            return new DigCommand(toDig.x, toDig.y);
        }

        /* REBUTAN SEMBAKO (Power UP) */
        Cell power = getPowerUp(gameState);
        if(power != null){
            return moveAndDig(power.x, power.y);
        }

        /* MOVING TO CENTER OF DE MAP */
        if(gameState.currentRound >150){
            Position center = new Position();
            center.x = 16;
            center.y = 16;
            return moveAndDig(center.x, center.y);
        }

        /* HUNTING */
        List<Worm> enemyWorms = new ArrayList<>();
        for (Worm ew : opponent.worms){
            if(ew.health>0){
                enemyWorms.add(ew);
            }
        }
        Worm enemyToHunt = getLowestHP(enemyWorms);
        return moveAndDig(enemyToHunt.position.x, enemyToHunt.position.y);



        /* FUNGSI BAWAAN YANG DISIMPAN-SIMPAN */
        /*
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand(); */
    }

    private Worm getFirstWormInRange() {
        // MENCARI OPPONENT WORM TERDEKAT
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
        // MEMBUAT LIST<WORM> DARI SEMUA OPPONENT WORM YANG DAPAT DITEMBAK
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

    private List<Worm> getFriendlyWormInRange(Direction direction, int shootRange){
        // MEMBUAT LIST<WORM> DARI SEMUA FRIENDLY WORM YANG MUNGKIN DAPAT TERTEMBAK
        List<Worm> listFriendlyWorm = new ArrayList<>();

        for(int directionMultiplier = 1; directionMultiplier <= shootRange; directionMultiplier++){
            int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
            int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

            for(Worm friendly : gameState.myPlayer.worms){
                if(friendly.position.x == coordinateX && friendly.position.y == coordinateY){
                    listFriendlyWorm.add(friendly);
                }
            }

        }

        return listFriendlyWorm;
    }


    private Worm getLowestHP(List<Worm> worms){
        // MENGEMBALIKAN WORM DENGAN HP TERKECIL DARI LIST<WORM>
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
        // MEMUAT LIST<WORM> DARI SEMUA OPPONENT WORM YANG DAPAT DI BOM
        List<Worm> listWorm = new ArrayList<>();
        for(Worm enemy : opponent.worms){
            if(euclideanDistance(currentWorm.position.x, currentWorm.position.y,enemy.position.x,enemy.position.y)< range){
                listWorm.add(enemy);
            }
        }
        return listWorm;
    }

    private Boolean InRadius(Position position, int radius){
        // MENGEMBALIKAN TRUE JIKA ADA FRINDLY WORM DI DALAM RADIUS
        // DIGUNAKAN UNTUK MENGHINDARI FRIENDLY FIRE
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
        // MENGEMBALIKAN OPPONENT WORM TERDEKAT
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
        // Mengembalikan posisi cell adjacent untuk dijadikan path untuk mencapai cell destination
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
        // MENGEMBALIKAN TRUE JIKA ADA WORM DI CELL
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
        // MEMBUAT POSISI BARU
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


    private Cell getDirtCell(List<Cell> surroundingCells){
        // MENGEMBALIKAN CELL BERTIPE DIRT DI SURROUNDING
        for(Cell c : surroundingCells){
            if(c.type == CellType.DIRT){
                return c;
            }
        }
        return null;
    }


    private List<List<Cell>> constructFireDirectionLines(int range) {
        // MEMBUAT FIRE DIRECTION LINES
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
        // MEMBUAT LIST<CELL> DARI SURROUNDING CELL
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
        // MEMBUAT DIRECTION
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
        // MEMBUAT LIST<CELL> YANG BERISI CELL YANG AMAN UNTUK DITUJU
        List<Cell> safeCell = getSurroundingCells(dodgeWorm.position.x, dodgeWorm.position.y);
        for(Cell c : dangerCell){
            safeCell.remove(c);
        }

        safeCell.removeIf(safe -> safe.type != CellType.AIR);
        return safeCell;
    }

    private int countEnemyWormAllive(Opponent opponent){
        // MENGHITUNG JUMLAH OPPONENT WORM YANG MASIH HIDUP
        int count = 0;
        for(Worm w: opponent.worms){
            if(w.health > 0){
                count += 1;
            }
        }
        return count;
    }

    private int countMyWormAllive(MyPlayer player){
        // MENGHITUNG JUMLAH WORM YANG MASIH HIDUP
        int count = 0;
        for(Worm w: player.worms){
            if(w.health > 0){
                count += 1;
            }
        }
        return count;
    }

    private Boolean inDanger(){
        // MENGEMBALIKAN TRUE JIKA WORM DALAM BAHAYA DENGAN PARAMETER TERTENTU
        if(currentWorm.health < 14 && opponentCurrentWorm.roundsUntilUnfrozen < 1){
            if (countEnemyWormAllive(opponent) > countMyWormAllive(gameState.myPlayer)){
                if(opponentCurrentWorm.health > 8){
                    List<Cell> dangerCell = constructDangerCell(5, opponentCurrentWorm.position);
                    for(Cell c : dangerCell){
                        if(currentWorm.position.x == c.x && currentWorm.position.y == c.y){
                            return true;
                        }
                    }
                }

            }
        }


        return false;
    }

    private Cell dodgeGaming(List<Cell> dangerCell){
        // DODGE GAMING :V
        List<Cell> safeCell = getSafeCell(dangerCell, currentWorm);
        if(!safeCell.isEmpty()){    // ada cell untuk kabur
            int idx = random.nextInt(safeCell.size());
            return safeCell.get(idx);
        }
        else{                       // gabisa kabur :((
            List<Cell> randommove = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
            randommove.removeIf(c -> c.type != CellType.AIR);
            randommove.removeIf(this::thereIsWorm);
            return randommove.get(random.nextInt(randommove.size()));
        }

    }

    private List<Cell> constructDangerCell(int range, Position enemyPosition) {
        // MENGEMBALIKAN LIST<CELL> YANG MUNGKIN DI SERANG OPPONENT
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

    private Cell getPowerUp(GameState gameState){
        // MENGEMBALIKAN CELL YANG MEMILIKI SEMBAKO (POWER UP)
        for(Cell[] cells : gameState.map){
            for (Cell cell : cells){
                if(cell.powerUp != null){
                    return cell;
                }
            }
        }
        return null;
    }

    /* COMMAND - COMMAND */

    /* MOVING */
    private Command moveAndDig(int xdes, int ydes){
        Position destination = new Position();
        destination.x = xdes;
        destination.y = ydes;
        Position path = getPath(currentWorm.position, destination);
        Cell blocktomove = gameState.map[path.y][path.x];
        if(blocktomove.type == CellType.AIR && !thereIsWorm(blocktomove)){
            return new MoveCommand(blocktomove.x, blocktomove.y);
        }else if(blocktomove.type == CellType.AIR && thereIsWorm(blocktomove)){
            blocktomove.x = miror(path, currentWorm).x;
            blocktomove.y = miror(path, currentWorm).y;
            return new MoveCommand(blocktomove.x, blocktomove.y);
        } else if (blocktomove.type == CellType.DIRT){
            return new DigCommand(blocktomove.x, blocktomove.y);
        }else{
            blocktomove.x = miror(path, currentWorm).x;
            blocktomove.y = miror(path, currentWorm).y;
            return new MoveCommand(blocktomove.x, blocktomove.y);
        }
    }

    /* SHOOTING */
    private Command shootEnemy(List<Worm> enemyWormsInRange){
        Worm wormToShoot = getLowestHP(enemyWormsInRange);
        Direction direction = resolveDirection(currentWorm.position, wormToShoot.position);
        List<Worm> friendlyWorm = getFriendlyWormInRange(direction, currentWorm.weapon.range);
        if(friendlyWorm.size()==0){
            return new ShootCommand(direction);
        }
        else {
            return moveAndDig(16,16);
        }
    }

}
