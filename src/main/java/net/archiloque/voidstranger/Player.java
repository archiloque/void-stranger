package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Player implements CharEntity {

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
        Action lastAction = result.get().actions.action();
        if (!lastAction.equals(action)) {
            throw new IllegalArgumentException(lastAction.name() + " should be " + action.name());
        }
        return result.get();
    }

    public static void play(Preparer.PreparationResult preparationResult) throws IOException {
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
        if (!move.playerPosition.equals(level.downStairsPosition())) {
            return false;
        }
        for (char entity : move.entities) {
            if (entity == ENTITY_CHEST_CLOSED) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFailingMove(@NotNull Level level, @NotNull Move move) {
        if (!move.playerPosition.equals(level.downStairsPosition())) {
            return false;
        }
        for (char entity : move.entities) {
            if (entity == ENTITY_CHEST_CLOSED) {
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
        Position targetPosition = initialMove.playerPosition.add(delta);
        int targetPositionIndex = level.positionIndex(targetPosition);
        if (targetPositionIndex < 0) {
            return false;
        }
        char targetEntity = initialMove.entities[targetPositionIndex];
        int initialPositionIndex = level.positionIndex(initialMove.playerPosition);
        switch (targetEntity) {
            case ENTITY_BOULDER, ENTITY_CHEST_OPEN, ENTITY_ENEMY_FACING_UP, ENTITY_ENEMY_FACING_DOWN, ENTITY_ENEMY_FACING_LEFT, ENTITY_ENEMY_FACING_RIGHT -> {
                return false;
            }
            case ENTITY_CHEST_CLOSED -> {
                return actionChestClosed(initialMove, targetPositionIndex, initialPositionIndex, callback);
            }
            case ENTITY_GROUND -> {
                return actionGround(level, initialMove, targetPositionIndex, initialPositionIndex, callback);
            }
            case ENTITY_HOLE -> {
                return actionHole(level, initialMove, targetPositionIndex, initialPositionIndex, callback);
            }
            default -> throw new IllegalStateException("Unexpected value: " + targetEntity);
        }
    }

    private static boolean actionHole(@NotNull Level level, @NotNull Move initialMove, int targetPositionIndex, int initialPositionIndex, @NotNull Function<Move, Boolean> callback) {
        if ((initialMove.playerState == Move.PlayerState.HOLD_GROUND) || (initialMove.playerState == Move.PlayerState.HOLD_GLASS)) {
            char[] newEntities = initialMove.entities.clone();
            newEntities[targetPositionIndex] = initialMove.playerState.entity;
            postMove(level, initialMove, newEntities, -1);
            if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                return false;
            }
            Move newMove = new Move(
                    initialMove.playerPosition,
                    initialMove.playerDirection,
                    Move.PlayerState.STANDARD,
                    newEntities,
                    new Move.ActionLinkedElement(Action.CREATE_GROUND, initialMove.actions)
            );
            return callback.apply(newMove);
        } else {
            return false;
        }
    }

    private static boolean actionGround(@NotNull Level level, @NotNull Move initialMove, int targetPositionIndex, int initialPositionIndex, @NotNull Function<Move, Boolean> callback) {
        if (initialMove.playerState == Move.PlayerState.STANDARD) {
            char[] newEntities = initialMove.entities.clone();
            newEntities[targetPositionIndex] = ENTITY_HOLE;
            postMove(level, initialMove, newEntities, -1);
            if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                return false;
            }
            Move newMove = new Move(
                    initialMove.playerPosition,
                    initialMove.playerDirection,
                    Move.PlayerState.HOLD_GROUND,
                    newEntities,
                    new Move.ActionLinkedElement(Action.ABSORB_GROUND, initialMove.actions)
            );
            return callback.apply(newMove);
        } else {
            return false;
        }
    }

    private static boolean actionChestClosed(@NotNull Move initialMove, int targetPositionIndex, int initialPositionIndex, @NotNull Function<Move, Boolean> callback) {
        if (!initialMove.playerDirection.equals(Direction.Up)) {
            return false;
        } else {
            char[] newEntities = initialMove.entities.clone();
            newEntities[targetPositionIndex] = ENTITY_CHEST_OPEN;
            if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                return false;
            }
            Move newMove = new Move(
                    initialMove.playerPosition,
                    initialMove.playerDirection,
                    initialMove.playerState,
                    newEntities,
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
        int initialPositionIndex = level.positionIndex(initialMove.playerPosition);
        char[] newEntities = initialMove.entities.clone();

        Position targetPosition = initialMove.playerPosition.add(delta);
        int targetPositionIndex = level.positionIndex(targetPosition);
        if (targetPositionIndex < 0) {
            postMove(level, initialMove, newEntities, -1);
            if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                return false;
            }

            // Moving toward a wall: just change your direction
            Move newMove = new Move(
                    initialMove.playerPosition,
                    direction,
                    initialMove.playerState,
                    newEntities,
                    new Move.ActionLinkedElement(action, initialMove.actions)
            );
            return callback.apply(newMove);
        }
        char targetEntity = initialMove.entities[targetPositionIndex];

        switch (targetEntity) {
            case ENTITY_BOULDER -> {
                return moveBoulder(level, initialMove, direction, delta, action, targetPosition, targetPositionIndex, newEntities, initialPositionIndex, callback);
            }
            case ENTITY_CHEST_OPEN, ENTITY_CHEST_CLOSED -> {
                postMove(level, initialMove, newEntities, -1);
                if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                    return false;
                }
                Move newMove = new Move(
                        initialMove.playerPosition,
                        direction,
                        initialMove.playerState,
                        newEntities,
                        new Move.ActionLinkedElement(action, initialMove.actions)
                );
                return callback.apply(newMove);

            }
            case ENTITY_ENEMY_FACING_UP, ENTITY_ENEMY_FACING_DOWN, ENTITY_ENEMY_FACING_LEFT, ENTITY_ENEMY_FACING_RIGHT, ENTITY_HOLE -> {
                return false;
            }
            case ENTITY_GROUND -> {
                postMove(level, initialMove, newEntities, -1);
                if (CharEntity.isEnemy(newEntities[targetPositionIndex]) || CharEntity.isEnemy(initialMove.entities[targetPositionIndex])) {
                    return false;
                }

                Move newMove = new Move(
                        targetPosition,
                        direction,
                        initialMove.playerState,
                        newEntities,
                        new Move.ActionLinkedElement(action, initialMove.actions)
                );
                if (isFailingMove(level, newMove)) {
                    return false;
                }
                return callback.apply(newMove);
            }
            default -> throw new IllegalStateException("Unexpected value: " + targetEntity);
        }
    }

    private static boolean moveBoulder(
            @NotNull Level level,
            @NotNull Move initialMove,
            @NotNull Direction direction,
            @NotNull Position delta,
            @NotNull Action action,
            @NotNull Position targetPosition,
            int targetPositionIndex,
            char @NotNull [] newEntities,
            int initialPositionIndex,
            @NotNull Function<Move, Boolean> callback) {
        Position beyondPosition = targetPosition.add(delta);
        int beyondPositionIndex = level.positionIndex(beyondPosition);
        if (beyondPositionIndex > 0) {
            char beyondPositionEntity = initialMove.entities[beyondPositionIndex];
            switch (beyondPositionEntity) {
                case ENTITY_BOULDER, ENTITY_CHEST_OPEN, ENTITY_CHEST_CLOSED -> {
                    Move newMove = new Move(
                            initialMove.playerPosition,
                            direction,
                            initialMove.playerState,
                            newEntities,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
                case ENTITY_GROUND, ENTITY_ENEMY_FACING_UP, ENTITY_ENEMY_FACING_DOWN, ENTITY_ENEMY_FACING_LEFT, ENTITY_ENEMY_FACING_RIGHT -> {
                    newEntities[targetPositionIndex] = ENTITY_GROUND;
                    newEntities[beyondPositionIndex] = ENTITY_BOULDER;
                    postMove(level, initialMove, newEntities, beyondPositionIndex);
                    if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPosition,
                            direction,
                            initialMove.playerState,
                            newEntities,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
                case ENTITY_HOLE -> {
                    newEntities[targetPositionIndex] = ENTITY_GROUND;
                    postMove(level, initialMove, newEntities, -1);
                    if (CharEntity.isEnemy(newEntities[initialPositionIndex])) {
                        return false;
                    }

                    Move newMove = new Move(
                            initialMove.playerPosition,
                            direction,
                            initialMove.playerState,
                            newEntities,
                            new Move.ActionLinkedElement(action, initialMove.actions)
                    );
                    return callback.apply(newMove);
                }
            }
        }
        return false;
    }

    private static void postMove(
            @NotNull Level level,
            @NotNull Move initialiMove,
            char @NotNull [] newEntities,
            int crushedEnemyIndex
    ) {
        for (int currentEntityIndex = 0; currentEntityIndex < newEntities.length; currentEntityIndex++) {
            char currentEntity = initialiMove.entities[currentEntityIndex];
            switch (currentEntity) {
                case ENTITY_ENEMY_FACING_DOWN -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        prepareNextMoveEnemy(
                                level,
                                newEntities,
                                currentEntityIndex,
                                DELTA_DOWN,
                                ENTITY_ENEMY_FACING_DOWN,
                                ENTITY_ENEMY_FACING_UP);
                    }
                }
                case ENTITY_ENEMY_FACING_LEFT -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        prepareNextMoveEnemy(
                                level,
                                newEntities,
                                currentEntityIndex,
                                DELTA_LEFT,
                                ENTITY_ENEMY_FACING_LEFT,
                                ENTITY_ENEMY_FACING_RIGHT);
                    }
                }
                case ENTITY_ENEMY_FACING_RIGHT -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        prepareNextMoveEnemy(
                                level,
                                newEntities,
                                currentEntityIndex,
                                DELTA_RIGHT,
                                ENTITY_ENEMY_FACING_RIGHT,
                                ENTITY_ENEMY_FACING_LEFT);
                    }
                }
                case ENTITY_ENEMY_FACING_UP -> {
                    if (currentEntityIndex != crushedEnemyIndex) {
                        prepareNextMoveEnemy(level,
                                newEntities,
                                currentEntityIndex,
                                DELTA_UP,
                                ENTITY_ENEMY_FACING_UP,
                                ENTITY_ENEMY_FACING_DOWN);
                    }
                }
                case ENTITY_GLASS -> {
                    throw new IllegalArgumentException("");
                }
            }
        }
    }

    private static void prepareNextMoveEnemy(
            @NotNull Level level,
            char @NotNull [] result,
            int currentEntityIndex,
            @NotNull Position delta,
            char currentEntity,
            char oppositeEnemy) {
        Position targetPosition = level.positions()[currentEntityIndex].add(delta);
        int targetPositionIndex = level.positionIndex(targetPosition);
        if (targetPositionIndex > 0) {
            result[currentEntityIndex] = oppositeEnemy;
        } else {
            switch (result[targetPositionIndex]) {
                case ENTITY_BOULDER -> {
                    result[currentEntityIndex] = oppositeEnemy;
                }
                case ENTITY_GROUND -> {
                    result[targetPositionIndex] = currentEntity;
                }
                default -> throw new IllegalStateException("Unexpected value: " + result[targetPositionIndex]);
            }
        }
    }
}
