package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Printer implements UpEntity, GroundEntity {

    public static void printPath(Level level, Move initialMove, Move finalMove) throws IOException {
        try (PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream("solutions/" + level.identifier() + ".txt")))) {
            printSolutionDescription(level, finalMove, printStream);
            printStream.println();
            printLevel(level, initialMove, printStream);
            printStream.println();
            Move currentMove = initialMove;
            for (Action action : finalMove.getActions()) {
                currentMove = Player.simulate(level, currentMove, action);
                printStream.println(action.name());
                printLevel(level, currentMove, printStream);
                printStream.println();
            }
        }
    }

    private static void printSolutionDescription(@NotNull Level level, @NotNull Move move, @NotNull PrintStream printStream) throws IOException {
        List<Action> actions = move.getActions();
        Action currentAction = null;
        int timesCurrentAction = -1;
        List<String> formattedActions = new ArrayList<>();
        List<String> rawActions = new ArrayList<>();
        for (Action action : actions) {
            rawActions.add(action.name());
            if (action.equals(currentAction)) {
                timesCurrentAction += 1;
            } else {
                switch (timesCurrentAction) {
                    case -1 -> {
                        // Initial case
                    }
                    case 1 -> formattedActions.add(currentAction.name());
                    default -> formattedActions.add(currentAction.name() + " x " + timesCurrentAction);
                }
                timesCurrentAction = 1;
                currentAction = action;
            }
        }
        if (timesCurrentAction == 1) {
            formattedActions.add(currentAction.name());
        } else {
            formattedActions.add(currentAction + " x " + timesCurrentAction);
        }
        printStream.println(String.join(", ", rawActions));
        printStream.println();
        printStream.println(String.join("\n", formattedActions));
    }

    public static void printLevel(Level level, Move move, PrintStream printStream) throws IOException {
        for (int lineIndex = 0; lineIndex <= level.height(); lineIndex++) {
            for (int columnIndex = 0; columnIndex <= level.width(); columnIndex++) {
                printStream.print(charAtPosition(level, move, columnIndex, lineIndex));
            }
            printStream.println();
        }
    }

    private static String charAtPosition(Level level, Move move, int columnIndex, int lineIndex) {
        Position position = new Position(columnIndex, lineIndex);
        int entityIndex = level.positionIndex(position);
        if (entityIndex < 0) {
            return "#";
        } else if (position.equals(level.positions()[move.playerPositionIndex])) {
            return "@";
        }

        char upEntity = move.upEntities[entityIndex];
        switch (upEntity) {
            case ENTITY_UP_BOULDER, ENTITY_UP_CHEST_OPEN, ENTITY_UP_CHEST_CLOSED -> {
                return Character.toString(upEntity);
            }
            case ENTITY_UP_ENEMY_FACING_UP -> {
                return "⇑";
            }
            case ENTITY_UP_ENEMY_FACING_RIGHT -> {
                return "⇒";
            }
            case ENTITY_UP_ENEMY_FACING_LEFT -> {
                return "⇐";
            }
            case ENTITY_UP_ENEMY_FACING_DOWN -> {
                return "⇓";
            }
        }
        char groundEntity = move.groundEntities[entityIndex];
        switch (groundEntity) {
            case ENTITY_GROUND_HOLE -> {
                return "□";
            }
            case ENTITY_GROUND_DOWNSTAIR -> {
                return "↓";
            }
            case ENTITY_GROUND_GROUND -> {
                return " ";
            }
            case ENTITY_GROUND_GLASS -> {
                return "■";
            }
        }
        throw new IllegalArgumentException("[" + upEntity + "] [" + groundEntity + "]");
    }

}
