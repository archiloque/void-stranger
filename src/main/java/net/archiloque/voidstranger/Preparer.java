package net.archiloque.voidstranger;

import com.google.gson.Gson;
import net.archiloque.voidstranger.importer.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Preparer implements UpEntity, GroundEntity {

    public record PreparationResult(@NotNull Level level, @NotNull Move move) {
    }

    public static PreparationResult prepareLevel(@NotNull File levelFile) throws IOException {
        Gson gson = new Gson();

        ImporterLevel importedImporterLevel = gson.fromJson(Files.readString(levelFile.toPath()), ImporterLevel.class);

        int height = importedImporterLevel.getAllEntities().mapToInt(Preparer::getLine).max().getAsInt();
        int width = importedImporterLevel.getAllEntities().mapToInt(Preparer::getColumn).max().getAsInt();

        ImporterEntities entities = importedImporterLevel.entities();
        ImporterEntityWithDirection importedPlayerPosition = entities.playerStartPosition().getFirst();
        Position playerPosition = new Position(getColumn(importedPlayerPosition), getLine(importedPlayerPosition));

        Position[] entitiesPosition = importedImporterLevel.getGroundEntities().
                map(entity -> new Position(getColumn(entity), getLine(entity))).
                sorted().
                toArray(Position[]::new);
        Level level = new Level(
                importedImporterLevel.identifier(),
                width,
                height,
                entitiesPosition);
        char[] groundEntities = new char[entitiesPosition.length];
        char[] upEntities = new char[entitiesPosition.length];
        for (int entityIndex = 0; entityIndex < entitiesPosition.length; entityIndex++) {
            groundEntities[entityIndex] = getGroundEntity(entitiesPosition[entityIndex], importedImporterLevel);
            upEntities[entityIndex] = getUpEntity(entitiesPosition[entityIndex], importedImporterLevel);
        }

        Move move = new Move(
                Arrays.binarySearch(entitiesPosition, playerPosition),
                importedPlayerPosition.getDirection(),
                Move.PlayerState.EMPTY,
                groundEntities,
                upEntities,
                null);
        return new PreparationResult(level, move);
    }

    private static int getColumn(ImporterEntity importerEntity) {
        return importerEntity.getX() / 16;
    }

    private static int getLine(ImporterEntity importerEntity) {
        return importerEntity.getY() / 16;
    }


    private static char getUpEntity(@NotNull Position position, @NotNull ImporterLevel importedImporterLevel) {
        Optional<SimpleImporterEntity> chest = find(position, importedImporterLevel.entities().chest());
        if (chest.isPresent()) {
            return ENTITY_UP_CHEST_CLOSED;
        }

        Optional<SimpleImporterEntity> boulder = find(position, importedImporterLevel.entities().boulder());
        if (boulder.isPresent()) {
            return ENTITY_UP_BOULDER;
        }

        Optional<ImporterEntityWithDirection> enemy = find(position, importedImporterLevel.entities().enemy());
        if (enemy.isPresent()) {
            switch (enemy.get().getDirection()) {
                case Up -> {
                    return ENTITY_UP_ENEMY_FACING_UP;
                }
                case Down -> {
                    return ENTITY_UP_ENEMY_FACING_DOWN;
                }
                case Left -> {
                    return ENTITY_UP_ENEMY_FACING_LEFT;
                }
                case Right -> {
                    return ENTITY_UP_ENEMY_FACING_RIGHT;
                }
            }
        }
        return ENTITY_UP_EMPTY;
    }

    private static char getGroundEntity(@NotNull Position position, @NotNull ImporterLevel importedImporterLevel) {
        Optional<SimpleImporterEntity> ground = find(position, importedImporterLevel.entities().ground());
        if (ground.isPresent()) {
            return ENTITY_GROUND_GROUND;
        }

        Optional<SimpleImporterEntity> hole = find(position, importedImporterLevel.entities().hole());
        if (hole.isPresent()) {
            return ENTITY_GROUND_HOLE;
        }

        Optional<SimpleImporterEntity> glass = find(position, importedImporterLevel.entities().glass());
        if (glass.isPresent()) {
            return ENTITY_GROUND_GLASS;
        }

        Optional<SimpleImporterEntity> downStair = find(position, importedImporterLevel.entities().downStairs());
        if (downStair.isPresent()) {
            return ENTITY_GROUND_DOWNSTAIR;
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
