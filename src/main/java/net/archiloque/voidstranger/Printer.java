package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Printer implements CharEntity{

    public static void printPath(Level level, Move initialMove, Move finalMove) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("solutions/" + level.identifier() + ".txt"))) {
            printSolutionDescription(level, finalMove, writer);
            writer.newLine();
            printLevel(level, initialMove, writer);
            writer.newLine();
            Move currentMove = initialMove;
            for(Action action : finalMove.getActions()) {
                currentMove = Player.simulate(level, currentMove, action);
                writer.write(action.name());
                writer.newLine();
                printLevel(level, currentMove, writer);
                writer.newLine();
            }
        }
    }

    private static void printSolutionDescription(@NotNull Level level, @NotNull Move move, @NotNull BufferedWriter writer) throws IOException {
        List<Action> actions = move.getActions();
        Action currentAction = null;
        int timesCurrentAction = -1;
        List<String> formattedActions = new ArrayList<>();
        for (Action action : actions) {
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
        writer.write(String.join("\n", formattedActions));
        writer.newLine();
    }

    private static void printLevel(Level level, Move move, BufferedWriter writer) throws IOException {
        for (int lineIndex = 0; lineIndex <= level.height(); lineIndex++) {
            for (int columnIndex = 0; columnIndex <= level.width(); columnIndex++) {
                writer.write(new String(charAtPosition(level, move, columnIndex, lineIndex)));
            }
            writer.newLine();
        }
    }

    private static String charAtPosition(Level level, Move move, int columnIndex, int lineIndex) {
        Position position = new Position(columnIndex, lineIndex);
        int entityIndex = level.positionIndex(position);
        if (entityIndex < 0) {
            return "#";
        } else if (position.equals(move.playerPosition)) {
            return "@";
        }

        char entity = move.entities[entityIndex];
        switch (entity) {
            case ENTITY_BOULDER, ENTITY_HOLE, ENTITY_CHEST_OPEN, ENTITY_CHEST_CLOSED -> {
                return Character.toString(entity);
            }
            case ENTITY_ENEMY_FACING_UP -> {
                return "⇑";
            }
            case ENTITY_ENEMY_FACING_RIGHT -> {
                return "⇒";
            }
            case ENTITY_ENEMY_FACING_LEFT -> {
                return "⇐";
            }
            case ENTITY_ENEMY_FACING_DOWN -> {
                return "⇓";
            }
            case ENTITY_GROUND -> {
                if (position.equals(level.downStairsPosition())) {
                    return "↓";
                } else {
                    return " ";
                }
            }
        }
        throw new IllegalArgumentException("");
    }

}
