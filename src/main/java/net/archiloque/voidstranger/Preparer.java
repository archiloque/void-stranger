package net.archiloque.voidstranger;

import com.google.gson.Gson;
import net.archiloque.voidstranger.importer.ImporterEntity;
import net.archiloque.voidstranger.importer.ImporterEntityWithDirection;
import net.archiloque.voidstranger.importer.ImporterLevel;
import net.archiloque.voidstranger.importer.SimpleImporterEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

public class Preparer implements CharEntity {

    public record PreparationResult(@NotNull Level level, @NotNull Move move) {
    }

    public static PreparationResult prepareLevel(@NotNull File levelFile) throws IOException {
        Gson gson = new Gson();

        ImporterLevel importedImporterLevel = gson.fromJson(Files.readString(levelFile.toPath()), ImporterLevel.class);

        int height = importedImporterLevel.getAllEntities().mapToInt(Preparer::getLine).max().getAsInt();
        int width = importedImporterLevel.getAllEntities().mapToInt(Preparer::getColumn).max().getAsInt();

        ImporterEntityWithDirection importedPlayerPosition = importedImporterLevel.entities().playerStartPosition().getFirst();
        Position playerPosition = new Position(getColumn(importedPlayerPosition), getLine(importedPlayerPosition));

        SimpleImporterEntity importedDownStairsPosition = importedImporterLevel.entities().downStairs().getFirst();
        Position downStairsPosition = new Position(getColumn(importedDownStairsPosition), getLine(importedDownStairsPosition));

        Position[] entitiesPosition = Stream.of(
                        importedImporterLevel.entities().boulder(),
                        importedImporterLevel.entities().chest(),
                        importedImporterLevel.entities().enemy(),
                        importedImporterLevel.entities().ground(),
                        importedImporterLevel.entities().hole()
                ).
                filter(Objects::nonNull).flatMap(Collection::stream).
                map(entity -> new Position(getColumn(entity), getLine(entity))).
                sorted().
                toArray(Position[]::new);
        Level level = new Level(
                importedImporterLevel.identifier(),
                width,
                height,
                downStairsPosition,
                entitiesPosition);
        char[] moveEntities = new char[entitiesPosition.length];
        for(int entityIndex = 0 ; entityIndex < entitiesPosition.length; entityIndex++) {
            moveEntities[entityIndex] = getEntity(entitiesPosition[entityIndex], importedImporterLevel);
        }
        Move move = new Move(
                playerPosition,
                importedPlayerPosition.getDirection(),
                Move.PlayerState.STANDARD,
                moveEntities,
                null);
        return new PreparationResult(level, move);
    }


    private static int getColumn(ImporterEntity importerEntity) {
        return importerEntity.getX() / 16;
    }

    private static int getLine(ImporterEntity importerEntity) {
        return importerEntity.getY() / 16;
    }


    private static char getEntity(@NotNull Position position, @NotNull ImporterLevel importedImporterLevel) {
        Optional<SimpleImporterEntity> boulder = find(position, importedImporterLevel.entities().boulder());
        if (boulder.isPresent()) {
            return ENTITY_BOULDER;
        }

        Optional<SimpleImporterEntity> chest = find(position, importedImporterLevel.entities().chest());
        if (chest.isPresent()) {
            return ENTITY_CHEST_CLOSED;
        }

        Optional<SimpleImporterEntity> glass = find(position, importedImporterLevel.entities().glass());
        if (glass.isPresent()) {
            return ENTITY_GLASS;
        }

        Optional<ImporterEntityWithDirection> enemy = find(position, importedImporterLevel.entities().enemy());
        if (enemy.isPresent()) {
            switch (enemy.get().getDirection()) {
                case Up -> {
                    return ENTITY_ENEMY_FACING_UP;
                }
                case Down -> {
                    return ENTITY_ENEMY_FACING_DOWN;
                }
                case Left -> {
                    return ENTITY_ENEMY_FACING_LEFT;
                }
                case Right -> {
                    return ENTITY_ENEMY_FACING_RIGHT;
                }
            }
        }

        Optional<SimpleImporterEntity> ground = find(position, importedImporterLevel.entities().ground());
        if (ground.isPresent()) {
            return ENTITY_GROUND;
        }

        Optional<SimpleImporterEntity> hole = find(position, importedImporterLevel.entities().hole());
        if (hole.isPresent()) {
            return ENTITY_HOLE;
        }

        throw new IllegalArgumentException(position.toString());
    }

    private static <T extends ImporterEntity> @NotNull Optional<T> find(@NotNull Position position, @Nullable List<T> entities) {
        if (entities == null) {
            return Optional.empty();
        } else {
            return entities.stream().filter(e -> getColumn(e) == position.column() && getLine(e) == position.line()).findFirst();
        }
    }

}
