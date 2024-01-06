package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Player implements GroundEntity, UpEntity {

    public static final Position DELTA_UP = new Position(0, -1);
    public static final Position DELTA_DOWN = new Position(0, 1);
    public static final Position DELTA_LEFT = new Position(-1, 0);
    public static final Position DELTA_RIGHT = new Position(1, 0);

    public static Move simulate(Level level, Move move, Action action) {
        AtomicReference<Move> result = new AtomicReference<>();
        switch (action) {
            case UP -> {
                moveUp(level, move, moveResult -> {
                    result.set(moveResult);
                    return false;
                });
            }
            case DOWN -> {
                moveDown(level, move, moveResult -> {
                    result.set(moveResult);
                    return false;
                });
            }
            case LEFT -> {
                moveLeft(level, move, moveResult -> {
                    result.set(moveResult);
                    return false;
                });
            }
            case RIGHT -> {
                moveRight(level, move, moveResult -> {
                    result.set(moveResult);
                    return false;
                });
            }
            case ABSORB_GROUND, CREATE_GROUND, OPEN_CHEST -> {
                action(level, move, moveResult -> {
                    result.set(moveResult);
                    return false;
                });
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }
        Move resultMove = result.get();
        if (resultMove == null) {
            throw new IllegalArgumentException("No result move, expected was " + action.name());
        }
        Action lastAction = resultMove.actions.action();
        if (!lastAction.equals(action)) {
            throw new IllegalArgumentException("Current move is " + lastAction.name() + " but is expected to be " + action.name());
        }
        return resultMove;
    }

    public static void play(Preparer.PreparationResult preparationResult) {
        Deque<Move> currentMoves = new LinkedList<>();
        Set<Move> knownMoves = new HashSet<>();
        currentMoves.addFirst(preparationResult.move());
        knownMoves.add(preparationResult.move());
        Level level = preparationResult.level();

        final AtomicReference<Boolean> reachedExit = new AtomicReference<>();
        reachedExit.set(false);
        final AtomicReference<Boolean> reachedRupee = new AtomicReference<>();
        reachedRupee.set(false);
        final AtomicReference<Boolean> reachedChest = new AtomicReference<>();
        reachedChest.set(false);

        while (!currentMoves.isEmpty()) {
            Move currentMove = currentMoves.removeFirst();
            if (nextMoves(level, currentMove, newMove -> {
                if (knownMoves.add(newMove)) {
                    MoveStatus moveStatus = moveStatus(newMove, level);
                    switch (moveStatus) {
                        case WIN -> {
                            printPath(preparationResult, newMove, level, "");
                            return true;
                        }
                        case RUPEE -> {
                            if (!reachedRupee.get()) {
                                printPath(preparationResult, newMove, level, "_rupee");
                                reachedRupee.set(true);
                            }
                        }
                        case CHEST -> {
                            if (!reachedChest.get()) {
                                printPath(preparationResult, newMove, level, "_chest");
                                reachedChest.set(true);
                            }
                        }
                        case EXIT -> {
                            if (!reachedExit.get()) {
                                printPath(preparationResult, newMove, level, "_exit");
                                reachedExit.set(true);
                            }
                        }
                        case LOOSE -> {
                            currentMoves.addLast(newMove);
                        }
                    }
                    return false;
                }
                return false;
            })) {
                return;
            }
        }
        System.out.println(" ### Fail !");
    }

    private static void printPath(@NotNull Preparer.PreparationResult preparationResult, @NotNull Move newMove, @NotNull Level level, @NotNull String suffix) {
        try {
            Printer.printPath(level, preparationResult.move(), newMove, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    enum MoveStatus {
        WIN,
        LOOSE,
        RUPEE,
        CHEST,
        EXIT,
    }

    public static MoveStatus moveStatus(@NotNull Move move, @NotNull Level level) {
        if (!(move.groundEntities[move.playerPositionIndex] == ENTITY_GROUND_DOWNSTAIR)) {
            return MoveStatus.LOOSE;
        }

        boolean closedChest = false;
        boolean missingRupee = false;

        if (level.hasChest()) {
            for (char entity : move.upEntities) {
                if (entity == ENTITY_UP_CHEST_CLOSED) {
                    closedChest = true;
                    break;
                }
            }
        }

        for (boolean rupeeFound : move.rupeesFound) {
            if (!rupeeFound) {
                missingRupee = true;
                break;
            }
        }

        if ((!closedChest) && (!missingRupee)) {
            return MoveStatus.WIN;
        } else if ((move.rupeesFound.length > 0) && (!missingRupee)) {
            return MoveStatus.RUPEE;
        } else if (level.hasChest() && (!closedChest)) {
            return MoveStatus.CHEST;
        } else {
            return MoveStatus.EXIT;
        }
    }

    private static boolean nextMoves(@NotNull Level level, @NotNull Move move, @NotNull Function<Move, Boolean> callback) {
        return
                moveUp(level, move, callback) ||
                        moveDown(level, move, callback) ||
                        moveLeft(level, move, callback) ||
                        moveRight(level, move, callback) ||
                        action(level, move, callback);
    }

    private static boolean moveRight(@NotNull Level level, @NotNull Move move, @NotNull Function<Move, Boolean> callback) {
        return move(level, move, Direction.Right, DELTA_RIGHT, Action.RIGHT, callback);
    }

    private static boolean moveLeft(@NotNull Level level, @NotNull Move move, @NotNull Function<Move, Boolean> callback) {
        return move(level, move, Direction.Left, DELTA_LEFT, Action.LEFT, callback);
    }

    private static boolean moveDown(@NotNull Level level, @NotNull Move move, @NotNull Function<Move, Boolean> callback) {
        return move(level, move, Direction.Down, DELTA_DOWN, Action.DOWN, callback);
    }

    private static boolean moveUp(@NotNull Level level, @NotNull Move move, @NotNull Function<Move, Boolean> callback) {
        return move(level, move, Direction.Up, DELTA_UP, Action.UP, callback);
    }

    private static boolean action(@NotNull Level level, @NotNull Move initialMove, @NotNull Function<Move, Boolean> callback) {
        Position delta = initialMove.playerDirection.delta;
        Position targetPosition = level.positions()[initialMove.playerPositionIndex].add(delta);
        int targetPositionIndex = level.positionIndex(targetPosition);
        if (targetPositionIndex < 0) {
            return false;
        }

        char upTargetEntity = initialMove.upEntities[targetPositionIndex];
        switch (upTargetEntity) {
            case ENTITY_UP_BOULDER, ENTITY_UP_CHEST_OPEN, ENTITY_UP_ENEMY_BASIC_FACING_UP, ENTITY_UP_ENEMY_BASIC_FACING_DOWN, ENTITY_UP_ENEMY_BASIC_FACING_LEFT, ENTITY_UP_ENEMY_BASIC_FACING_RIGHT -> {
                return false;
            }
            case ENTITY_UP_CHEST_CLOSED -> {
                return actionChestClosed(initialMove, targetPositionIndex, initialMove.playerPositionIndex, callback);
            }
        }

        char groundTargetEntity = initialMove.groundEntities[targetPositionIndex];
        switch (groundTargetEntity) {
            case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR, ENTITY_GROUND_GLASS -> {
                return actionAbsorbGround(level, initialMove, targetPositionIndex, initialMove.playerPositionIndex, callback);
            }
            case ENTITY_GROUND_HOLE -> {
                return actionCreateGround(level, initialMove, targetPositionIndex, initialMove.playerPositionIndex, callback);
            }
        }
        throw new IllegalStateException("Unexpected value: [" + upTargetEntity + "] [" + groundTargetEntity + "]");
    }

    private static boolean actionCreateGround(@NotNull Level level,
                                              @NotNull Move initialMove,
                                              int targetPositionIndex,
                                              int playerPositionIndex,
                                              @NotNull Function<Move, Boolean> callback) {
        if (initialMove.playerState == Move.PlayerState.EMPTY) {
            return false;
        }
        char[] newGroundEntities = initialMove.groundEntities.clone();
        char[] newUpEntities = initialMove.upEntities.clone();
        newGroundEntities[targetPositionIndex] = initialMove.playerState.entity;
        postMove(level, initialMove, playerPositionIndex, newGroundEntities, newUpEntities, -1);
        if (UpEntity.isEnemy(newUpEntities[playerPositionIndex])) {
            return false;
        }
        Move newMove = new Move(
                initialMove.playerPositionIndex,
                initialMove.playerDirection,
                Move.PlayerState.EMPTY,
                newGroundEntities,
                newUpEntities,
                initialMove.rupeesFound,
                new Move.ActionLinkedElement(Action.CREATE_GROUND, initialMove.actions)
        );
        return callback.apply(newMove);

    }

    private static boolean actionAbsorbGround(
            @NotNull Level level,
            @NotNull Move initialMove,
            int targetPositionIndex,
            int playerPositionIndex,
            @NotNull Function<Move, Boolean> callback) {
        if (initialMove.playerState != Move.PlayerState.EMPTY) {
            return false;
        }
        char[] newGroundEntities = initialMove.groundEntities.clone();
        char[] newUpEntities = initialMove.upEntities.clone();
        boolean[] newRupeesFound = initialMove.rupeesFound;
        int rupeeIndex = Arrays.binarySearch(level.rupeesIndexes(), targetPositionIndex);
        if ((rupeeIndex >= 0) && (!newRupeesFound[rupeeIndex])) {
            newRupeesFound = newRupeesFound.clone();
            newRupeesFound[rupeeIndex] = true;
        }
        newGroundEntities[targetPositionIndex] = ENTITY_GROUND_HOLE;
        postMove(level, initialMove, playerPositionIndex, newGroundEntities, newUpEntities, -1);
        if (UpEntity.isEnemy(newUpEntities[playerPositionIndex])) {
            return false;
        }
        Move newMove = new Move(
                initialMove.playerPositionIndex,
                initialMove.playerDirection,
                Move.fromGroundEntity(initialMove.groundEntities[targetPositionIndex]),
                newGroundEntities,
                newUpEntities,
                newRupeesFound,
                new Move.ActionLinkedElement(Action.ABSORB_GROUND, initialMove.actions)
        );
        return callback.apply(newMove);

    }

    private static boolean actionChestClosed(
            @NotNull Move initialMove,
            int targetPositionIndex,
            int initialPositionIndex,
            @NotNull Function<Move, Boolean> callback) {
        if (!initialMove.playerDirection.equals(Direction.Up)) {
            return false;
        } else {
            char[] newGroundEntities = initialMove.groundEntities.clone();
            char[] newUpEntities = initialMove.upEntities.clone();
            newUpEntities[targetPositionIndex] = ENTITY_UP_CHEST_OPEN;
            if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                return false;
            }
            Move newMove = new Move(
                    initialMove.playerPositionIndex,
                    initialMove.playerDirection,
                    initialMove.playerState,
                    newGroundEntities,
                    newUpEntities,
                    initialMove.rupeesFound,
                    new Move.ActionLinkedElement(Action.OPEN_CHEST, initialMove.actions)
            );
            return callback.apply(newMove);
        }
    }

    private static boolean move(
            @NotNull Level level,
            @NotNull Move initialMove,
            @NotNull Direction direction,
            @NotNull Position delta,
            @NotNull Action action,
            @NotNull Function<Move, Boolean> callback) {
        int initialPositionIndex = initialMove.playerPositionIndex;
        char[] newGroundEntities = initialMove.groundEntities.clone();
        char[] newUpEntities = initialMove.upEntities.clone();

        Position targetPosition = level.positions()[initialMove.playerPositionIndex].add(delta);
        int targetPositionIndex = level.positionIndex(targetPosition);
        if (targetPositionIndex < 0) {
            postMove(level, initialMove, initialMove.playerPositionIndex, newGroundEntities, newUpEntities, -1);
            if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                return false;
            }

            // Moving toward a wall: just change your direction
            Move newMove = new Move(
                    initialMove.playerPositionIndex,
                    direction,
                    initialMove.playerState,
                    newGroundEntities,
                    newUpEntities,
                    initialMove.rupeesFound,
                    new Move.ActionLinkedElement(action, initialMove.actions)
            );
            return callback.apply(newMove);
        }
        char targetUpEntity = initialMove.upEntities[targetPositionIndex];

        switch (targetUpEntity) {
            case ENTITY_UP_BOULDER -> {
                return moveBoulder(level, initialMove, direction, delta, action, targetPosition, targetPositionIndex, newGroundEntities, newUpEntities, initialPositionIndex, callback);
            }
            case ENTITY_UP_CHEST_OPEN, ENTITY_UP_CHEST_CLOSED -> {
                postMove(level, initialMove, initialMove.playerPositionIndex, newGroundEntities, newUpEntities, -1);
                if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                    return false;
                }
                Move newMove = new Move(
                        initialMove.playerPositionIndex,
                        direction,
                        initialMove.playerState,
                        newGroundEntities,
                        newUpEntities,
                        initialMove.rupeesFound,
                        new Move.ActionLinkedElement(action, initialMove.actions)
                );
                return callback.apply(newMove);

            }
            case ENTITY_UP_ENEMY_BASIC_FACING_UP, ENTITY_UP_ENEMY_BASIC_FACING_DOWN, ENTITY_UP_ENEMY_BASIC_FACING_LEFT, ENTITY_UP_ENEMY_BASIC_FACING_RIGHT -> {
                return false;
            }
        }
        char targetGroundEntity = initialMove.groundEntities[targetPositionIndex];
        switch (targetGroundEntity) {
            case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR, ENTITY_GROUND_GLASS -> {
                postMove(level, initialMove, targetPositionIndex, newGroundEntities, newUpEntities, -1);
                if (UpEntity.isEnemy(newUpEntities[targetPositionIndex]) || UpEntity.isEnemy(initialMove.upEntities[targetPositionIndex])) {
                    return false;
                }

                Move newMove = new Move(
                        Arrays.binarySearch(level.positions(), targetPosition),
                        direction,
                        initialMove.playerState,
                        newGroundEntities,
                        newUpEntities,
                        initialMove.rupeesFound,
                        new Move.ActionLinkedElement(action, initialMove.actions)
                );
                return callback.apply(newMove);
            }
            case ENTITY_GROUND_HOLE -> {
                return false;
            }
        }
        throw new IllegalStateException("Unexpected value: [" + targetUpEntity + "] [" + targetGroundEntity + "]");
    }

    private static boolean moveBoulder(
            @NotNull Level level,
            @NotNull Move initialMove,
            @NotNull Direction direction,
            @NotNull Position delta,
            @NotNull Action action,
            @NotNull Position targetPosition,
            int targetPositionIndex,
            char @NotNull [] newGroundEntities,
            char @NotNull [] newUpEntities,
            int initialPositionIndex,
            @NotNull Function<Move, Boolean> callback) {
        Position beyondPosition = targetPosition.add(delta);
        int beyondPositionIndex = level.positionIndex(beyondPosition);
        if (beyondPositionIndex > 0) {
            char beyondPositionUpEntity = initialMove.upEntities[beyondPositionIndex];
            switch (beyondPositionUpEntity) {
                case ENTITY_UP_BOULDER, ENTITY_UP_CHEST_OPEN, ENTITY_UP_CHEST_CLOSED -> {
                    postMove(level, initialMove, initialMove.playerPositionIndex, newGroundEntities, newUpEntities, beyondPositionIndex);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            initialMove.rupeesFound,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
                case ENTITY_UP_ENEMY_BASIC_FACING_UP, ENTITY_UP_ENEMY_BASIC_FACING_DOWN, ENTITY_UP_ENEMY_BASIC_FACING_LEFT, ENTITY_UP_ENEMY_BASIC_FACING_RIGHT -> {
                    newUpEntities[targetPositionIndex] = ENTITY_UP_EMPTY;
                    newUpEntities[beyondPositionIndex] = ENTITY_UP_BOULDER;
                    postMove(level, initialMove, targetPositionIndex, newGroundEntities, newUpEntities, beyondPositionIndex);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            initialMove.rupeesFound,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
            }
            char beyondPositionGroundEntity = initialMove.groundEntities[beyondPositionIndex];
            switch (beyondPositionGroundEntity) {
                case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR, ENTITY_GROUND_GLASS -> {
                    newUpEntities[targetPositionIndex] = ENTITY_UP_EMPTY;
                    newUpEntities[beyondPositionIndex] = ENTITY_UP_BOULDER;
                    postMove(level, initialMove, targetPositionIndex, newGroundEntities, newUpEntities, beyondPositionIndex);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    if(initialMove.groundEntities[targetPositionIndex] == ENTITY_GROUND_GLASS) {
                        newGroundEntities[targetPositionIndex] = ENTITY_GROUND_HOLE;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            initialMove.rupeesFound,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
                case ENTITY_GROUND_HOLE -> {
                    newUpEntities[targetPositionIndex] = ENTITY_UP_EMPTY;
                    postMove(level, initialMove, targetPositionIndex, newGroundEntities, newUpEntities, -1);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    if(initialMove.groundEntities[targetPositionIndex] == ENTITY_GROUND_GLASS) {
                        newGroundEntities[targetPositionIndex] = ENTITY_GROUND_HOLE;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            initialMove.rupeesFound,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
            }
        } else {
            postMove(level, initialMove, initialMove.playerPositionIndex, newGroundEntities, newUpEntities, beyondPositionIndex);
            if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                return false;
            }
            Move newMove = new Move(
                    initialMove.playerPositionIndex,
                    direction,
                    initialMove.playerState,
                    newGroundEntities,
                    newUpEntities,
                    initialMove.rupeesFound,
                    new Move.ActionLinkedElement(action, initialMove.actions)
            );
            return callback.apply(newMove);
        }
        return false;
    }

    private static void postMove(
            @NotNull Level level,
            @NotNull Move initialMove,
            int newPlayerPositionIndex,
            char @NotNull [] newGroundEntities,
            char @NotNull [] newUpEntities,
            int crushedEnemyIndex
    ) {
        for (int currentEntityIndex = 0; currentEntityIndex < newUpEntities.length; currentEntityIndex++) {
            char currentUpEntity = initialMove.upEntities[currentEntityIndex];
            switch (currentUpEntity) {
                case ENTITY_UP_ENEMY_BASIC_FACING_DOWN -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveBasicEnemy(
                                level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_DOWN,
                                ENTITY_UP_ENEMY_BASIC_FACING_DOWN,
                                ENTITY_UP_ENEMY_BASIC_FACING_UP);
                    }
                }
                case ENTITY_UP_ENEMY_BASIC_FACING_LEFT -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveBasicEnemy(
                                level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_LEFT,
                                ENTITY_UP_ENEMY_BASIC_FACING_LEFT,
                                ENTITY_UP_ENEMY_BASIC_FACING_RIGHT);
                    }
                }
                case ENTITY_UP_ENEMY_BASIC_FACING_RIGHT -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveBasicEnemy(
                                level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_RIGHT,
                                ENTITY_UP_ENEMY_BASIC_FACING_RIGHT,
                                ENTITY_UP_ENEMY_BASIC_FACING_LEFT);
                    }
                }
                case ENTITY_UP_ENEMY_BASIC_FACING_UP -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveBasicEnemy(level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_UP,
                                ENTITY_UP_ENEMY_BASIC_FACING_UP,
                                ENTITY_UP_ENEMY_BASIC_FACING_DOWN);
                    }
                }
                case ENTITY_UP_ENEMY_SEEKER -> {
                    postMoveSeekerEnemy(
                            level,
                            newPlayerPositionIndex,
                            newGroundEntities,
                            newUpEntities,
                            currentEntityIndex
                    );
                }
            }
            char currentGroundEntity = initialMove.groundEntities[currentEntityIndex];
            switch (currentGroundEntity) {
                case ENTITY_GROUND_GLASS -> {
                    if (currentEntityIndex == initialMove.playerPositionIndex) {
                        newGroundEntities[currentEntityIndex] = ENTITY_GROUND_HOLE;
                    }
                }
            }
        }
    }

    private static void postMoveSeekerEnemy(
            @NotNull Level level,
            int newPlayerPositionIndex,
            char @NotNull [] newGroundEntities,
            char @NotNull [] newUpEntities,
            int currentEntityIndex) {
        // Is the played in the same line or column than the enemy
        Position playerPosition = level.positions()[newPlayerPositionIndex];
        Position enemyPosition = level.positions()[currentEntityIndex];
        Position delta;
        if (playerPosition.line() == enemyPosition.line()) {
            delta = (playerPosition.column() > enemyPosition.column()) ? DELTA_RIGHT : DELTA_LEFT;
        } else if (playerPosition.column() == enemyPosition.column()) {
            delta = (playerPosition.line() > enemyPosition.line()) ? DELTA_DOWN : DELTA_UP;
        } else {
            return;
        }

        // Check if the player is visible
        Position currentlyCheckedPosition = enemyPosition.add(delta);
        while (!currentlyCheckedPosition.equals(playerPosition)) {
            int currentlyCheckedPositionIndex = level.positionIndex(currentlyCheckedPosition);
            if (currentlyCheckedPositionIndex < 0) {
                return;
            }
            if (newUpEntities[currentlyCheckedPositionIndex] != ENTITY_UP_EMPTY) {
                return;
            }
            currentlyCheckedPosition = currentlyCheckedPosition.add(delta);
        }

        // Move the enemy
        Position targetPosition = enemyPosition.add(delta);
        int targetEntity = level.positionIndex(targetPosition);
        char currentGroundEntity = newGroundEntities[targetEntity];
        switch (currentGroundEntity) {
            case ENTITY_GROUND_HOLE -> {
                newUpEntities[currentEntityIndex] = ENTITY_UP_EMPTY;
                if (newGroundEntities[currentEntityIndex] == ENTITY_GROUND_GLASS) {
                    newGroundEntities[currentEntityIndex] = ENTITY_GROUND_HOLE;
                }
                return;
            }
            case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR, ENTITY_GROUND_GLASS -> {
                newUpEntities[currentEntityIndex] = ENTITY_UP_EMPTY;
                newUpEntities[targetEntity] = ENTITY_UP_ENEMY_SEEKER;
                if (newGroundEntities[currentEntityIndex] == ENTITY_GROUND_GLASS) {
                    newGroundEntities[currentEntityIndex] = ENTITY_GROUND_HOLE;
                }
                return;
            }
        }
        throw new IllegalStateException("Unexpected value: [" + currentGroundEntity + "]");
    }

    private static void postMoveBasicEnemy(
            @NotNull Level level,
            char @NotNull [] newGroundEntities,
            char @NotNull [] newUpEntities,
            int currentEntityIndex,
            @NotNull Position delta,
            char currentEntity,
            char oppositeEnemy) {
        Position targetPosition = level.positions()[currentEntityIndex].add(delta);
        int targetPositionIndex = level.positionIndex(targetPosition);
        if (targetPositionIndex < 0) {
            newUpEntities[currentEntityIndex] = oppositeEnemy;
        } else {
            char targetUpEntity = newUpEntities[targetPositionIndex];
            switch (targetUpEntity) {
                case ENTITY_UP_BOULDER, ENTITY_UP_CHEST_CLOSED, ENTITY_UP_CHEST_OPEN, ENTITY_UP_ENEMY_BASIC_FACING_DOWN, ENTITY_UP_ENEMY_BASIC_FACING_LEFT, ENTITY_UP_ENEMY_BASIC_FACING_RIGHT, ENTITY_UP_ENEMY_BASIC_FACING_UP -> {
                    newUpEntities[currentEntityIndex] = oppositeEnemy;
                    return;
                }
            }
            char targetGroundEntity = newGroundEntities[targetPositionIndex];
            switch (targetGroundEntity) {
                case ENTITY_GROUND_HOLE -> {
                    newUpEntities[currentEntityIndex] = oppositeEnemy;
                }
                case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR, ENTITY_GROUND_GLASS -> {
                    newUpEntities[currentEntityIndex] = ENTITY_UP_EMPTY;
                    newUpEntities[targetPositionIndex] = currentEntity;
                    if (newGroundEntities[currentEntityIndex] == ENTITY_GROUND_GLASS) {
                        newGroundEntities[currentEntityIndex] = ENTITY_GROUND_HOLE;
                    }
                }
            }
        }
    }
}
