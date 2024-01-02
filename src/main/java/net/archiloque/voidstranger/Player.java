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
        while (!currentMoves.isEmpty()) {
            Move currentMove = currentMoves.removeFirst();
            if (nextMoves(level, currentMove, newMove -> {
                if (knownMoves.add(newMove)) {
                    if (isWinningMove(level, newMove)) {
                        try {
                            Printer.printPath(level, preparationResult.move(), newMove);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    } else {
                        currentMoves.addLast(newMove);
                    }
                }
                return false;
            })) {
                return;
            }
        }
        System.out.println(" ### Fail !");
    }

    private static boolean isWinningMove(@NotNull Level level, @NotNull Move move) {
        if (!(move.groundEntities[move.playerPositionIndex] == ENTITY_GROUND_DOWNSTAIR)) {
            return false;
        }
        for (char entity : move.upEntities) {
            if (entity == ENTITY_UP_CHEST_CLOSED) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFailingMove(@NotNull Move move) {
        if (move.groundEntities[move.playerPositionIndex] != ENTITY_GROUND_DOWNSTAIR) {
            return false;
        }
        for (char entity : move.upEntities) {
            if (entity == ENTITY_UP_CHEST_CLOSED) {
                return true;
            }
        }
        return false;
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
        int initialPositionIndex = initialMove.playerPositionIndex;

        char upTargetEntity = initialMove.upEntities[targetPositionIndex];
        switch (upTargetEntity) {
            case ENTITY_UP_BOULDER, ENTITY_UP_CHEST_OPEN, ENTITY_UP_ENEMY_FACING_UP, ENTITY_UP_ENEMY_FACING_DOWN, ENTITY_UP_ENEMY_FACING_LEFT, ENTITY_UP_ENEMY_FACING_RIGHT -> {
                return false;
            }
            case ENTITY_UP_CHEST_CLOSED -> {
                return actionChestClosed(initialMove, targetPositionIndex, initialPositionIndex, callback);
            }
        }

        char groundTargetEntity = initialMove.groundEntities[targetPositionIndex];
        switch (groundTargetEntity) {
            case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR, ENTITY_GROUND_GLASS -> {
                return actionAbsorbGround(level, initialMove, targetPositionIndex, initialPositionIndex, callback);
            }
            case ENTITY_GROUND_HOLE -> {
                return actionCreateGround(level, initialMove, targetPositionIndex, initialPositionIndex, callback);
            }
        }
        throw new IllegalStateException("Unexpected value: [" + upTargetEntity + "] [" + groundTargetEntity + "]");
    }

    private static boolean actionCreateGround(@NotNull Level level,
                                              @NotNull Move initialMove,
                                              int targetPositionIndex,
                                              int initialPositionIndex,
                                              @NotNull Function<Move, Boolean> callback) {
        if (initialMove.playerState == Move.PlayerState.EMPTY) {
            return false;
        }
        char[] newGroundEntities = initialMove.groundEntities.clone();
        char[] newUpEntities = initialMove.upEntities.clone();
        newGroundEntities[targetPositionIndex] = initialMove.playerState.entity;
        postMove(level, initialMove, newGroundEntities, newUpEntities, -1);
        if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
            return false;
        }
        Move newMove = new Move(
                initialMove.playerPositionIndex,
                initialMove.playerDirection,
                Move.PlayerState.EMPTY,
                newGroundEntities,
                newUpEntities,
                new Move.ActionLinkedElement(Action.CREATE_GROUND, initialMove.actions)
        );
        return callback.apply(newMove);

    }

    private static boolean actionAbsorbGround(
            @NotNull Level level,
            @NotNull Move initialMove,
            int targetPositionIndex,
            int initialPositionIndex,
            @NotNull Function<Move, Boolean> callback) {
        if (initialMove.playerState != Move.PlayerState.EMPTY) {
            return false;
        }
        char[] newGroundEntities = initialMove.groundEntities.clone();
        char[] newUpEntities = initialMove.upEntities.clone();
        newGroundEntities[targetPositionIndex] = ENTITY_GROUND_HOLE;
        postMove(level, initialMove, newGroundEntities, newUpEntities, -1);
        if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
            return false;
        }
        Move newMove = new Move(
                initialMove.playerPositionIndex,
                initialMove.playerDirection,
                Move.fromGroundEntity(initialMove.groundEntities[targetPositionIndex]),
                newGroundEntities, newUpEntities, new Move.ActionLinkedElement(Action.ABSORB_GROUND, initialMove.actions)
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
            postMove(level, initialMove, newGroundEntities, newUpEntities, -1);
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
                postMove(level, initialMove, newGroundEntities, newUpEntities, -1);
                if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                    return false;
                }
                Move newMove = new Move(
                        initialMove.playerPositionIndex,
                        direction,
                        initialMove.playerState,
                        newGroundEntities,
                        newUpEntities,
                        new Move.ActionLinkedElement(action, initialMove.actions)
                );
                return callback.apply(newMove);

            }
            case ENTITY_UP_ENEMY_FACING_UP, ENTITY_UP_ENEMY_FACING_DOWN, ENTITY_UP_ENEMY_FACING_LEFT, ENTITY_UP_ENEMY_FACING_RIGHT -> {
                return false;
            }
        }
        char targetGroundEntity = initialMove.groundEntities[targetPositionIndex];
        switch (targetGroundEntity) {
            case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR -> {
                postMove(level, initialMove, newGroundEntities, newUpEntities, -1);
                if (UpEntity.isEnemy(newUpEntities[targetPositionIndex]) || UpEntity.isEnemy(initialMove.upEntities[targetPositionIndex])) {
                    return false;
                }

                Move newMove = new Move(
                        Arrays.binarySearch(level.positions(), targetPosition),
                        direction,
                        initialMove.playerState,
                        newGroundEntities,
                        newUpEntities,
                        new Move.ActionLinkedElement(action, initialMove.actions)
                );
                if (isFailingMove(newMove)) {
                    return false;
                }
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
                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
                case ENTITY_UP_ENEMY_FACING_UP, ENTITY_UP_ENEMY_FACING_DOWN, ENTITY_UP_ENEMY_FACING_LEFT, ENTITY_UP_ENEMY_FACING_RIGHT -> {
                    newUpEntities[targetPositionIndex] = ENTITY_UP_EMPTY;
                    newUpEntities[beyondPositionIndex] = ENTITY_UP_BOULDER;
                    postMove(level, initialMove, newGroundEntities, newUpEntities, beyondPositionIndex);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
            }
            char beyondPositionGroundEntity = initialMove.groundEntities[beyondPositionIndex];
            switch (beyondPositionGroundEntity) {
                case ENTITY_GROUND_GROUND, ENTITY_GROUND_DOWNSTAIR -> {
                    newUpEntities[targetPositionIndex] = ENTITY_UP_EMPTY;
                    newUpEntities[beyondPositionIndex] = ENTITY_UP_BOULDER;
                    postMove(level, initialMove, newGroundEntities, newUpEntities, beyondPositionIndex);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities,
                            newUpEntities,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
                case ENTITY_GROUND_HOLE -> {
                    newUpEntities[targetPositionIndex] = ENTITY_UP_EMPTY;
                    postMove(level, initialMove, newGroundEntities, newUpEntities, -1);
                    if (UpEntity.isEnemy(newUpEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPositionIndex,
                            direction,
                            initialMove.playerState,
                            newGroundEntities, newUpEntities, new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
            }
        }
        return false;
    }

    private static void postMove(
            @NotNull Level level,
            @NotNull Move initialMove,
            char @NotNull [] newGroundEntities,
            char @NotNull [] newUpEntities,
            int crushedEnemyIndex
    ) {
        for (int currentEntityIndex = 0; currentEntityIndex < newUpEntities.length; currentEntityIndex++) {
            char currentUpEntity = initialMove.upEntities[currentEntityIndex];
            switch (currentUpEntity) {
                case ENTITY_UP_ENEMY_FACING_DOWN -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveEnemy(
                                level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_DOWN,
                                ENTITY_UP_ENEMY_FACING_DOWN,
                                ENTITY_UP_ENEMY_FACING_UP);
                    }
                }
                case ENTITY_UP_ENEMY_FACING_LEFT -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveEnemy(
                                level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_LEFT,
                                ENTITY_UP_ENEMY_FACING_LEFT,
                                ENTITY_UP_ENEMY_FACING_RIGHT);
                    }
                }
                case ENTITY_UP_ENEMY_FACING_RIGHT -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveEnemy(
                                level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_RIGHT,
                                ENTITY_UP_ENEMY_FACING_RIGHT,
                                ENTITY_UP_ENEMY_FACING_LEFT);
                    }
                }
                case ENTITY_UP_ENEMY_FACING_UP -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        postMoveEnemy(level,
                                newGroundEntities,
                                newUpEntities,
                                currentEntityIndex,
                                DELTA_UP,
                                ENTITY_UP_ENEMY_FACING_UP,
                                ENTITY_UP_ENEMY_FACING_DOWN);
                    }
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

    private static void postMoveEnemy(
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
                case ENTITY_UP_BOULDER, ENTITY_UP_CHEST_CLOSED, ENTITY_UP_CHEST_OPEN, ENTITY_UP_ENEMY_FACING_DOWN, ENTITY_UP_ENEMY_FACING_LEFT, ENTITY_UP_ENEMY_FACING_RIGHT, ENTITY_UP_ENEMY_FACING_UP -> {
                    newUpEntities[currentEntityIndex] = oppositeEnemy;
                }
            }
            char targeGroundEntity = newGroundEntities[targetPositionIndex];
            switch (targeGroundEntity) {
                case ENTITY_GROUND_HOLE -> {
                    newUpEntities[currentEntityIndex] = oppositeEnemy;
                }
                case ENTITY_GROUND_GROUND -> {
                    newUpEntities[currentEntityIndex] = ENTITY_UP_EMPTY;
                    newUpEntities[targetPositionIndex] = currentEntity;
                }
            }
        }
    }
}
