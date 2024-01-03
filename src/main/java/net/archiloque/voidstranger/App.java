package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class App {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            File[] levelDirectories = new File("levels/VoidStranger/Simplified").listFiles();
            Arrays.sort(Objects.requireNonNull(levelDirectories));
            for (File levelDirectory : levelDirectories) {
                File[] files = levelDirectory.listFiles((dir, name) -> name.endsWith(".json"));
                processLevel(Objects.requireNonNull(files)[0]);
            }
        } else {
            File levelFile = new File("levels/VoidStranger/Simplified/" + args[0] + "/data.json");
            if (args.length == 1) {
                processLevel(levelFile);
            } else if (args.length == 2) {
                Preparer.PreparationResult preparationResult = Preparer.prepareLevel(levelFile);
                Move currentMove = preparationResult.move();
                Level level = preparationResult.level();
                for (String actionString : args[1].split(", ")) {
                    System.out.println("[" + actionString + "]");
                    Action action = Action.valueOf(actionString);
                    currentMove = Player.simulate(level, currentMove, action);
                    Printer.printLevel(level, currentMove, System.out);
                    System.out.println();
                }
                System.out.println("Over");
                System.out.println(Player.moveStatus(currentMove, level));
            } else {
                throw new IllegalArgumentException("");
            }
        }
    }

    public static void processLevel(@NotNull File levelFile) throws IOException {
        System.out.println("Process file " + levelFile);
        Preparer.PreparationResult preparationResult = Preparer.prepareLevel(levelFile);
        Player.play(preparationResult);
    }
}
