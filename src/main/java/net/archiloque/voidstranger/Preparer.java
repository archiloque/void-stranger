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
        Position playerPosition = getPosition(importedPlayerPosition);

        Position[] entitiesPosition = importedImporterLevel.getGroundEntities().
                map(entity -> new Position(getColumn(entity), getLine(entity))).
                sorted().
                toArray(Position[]::new);

        int[] rupeesIndexes = getRupees(importedImporterLevel, entitiesPosition);

        char[] groundEntities = new char[entitiesPosition.length];
        char[] upEntities = new char[entitiesPosition.length];
        for (int entityIndex = 0; entityIndex < entitiesPosition.length; entityIndex++) {
            groundEntities[entityIndex] = getGroundEntity(entitiesPosition[entityIndex], importedImporterLevel);
            upEntities[entityIndex] = getUpEntity(entitiesPosition[entityIndex], importedImporterLevel);
        }
        boolean hasChest = false;
        for (char upEntity : upEntities) {
            if (upEntity == ENTITY_UP_CHEST_CLOSED) {
                hasChest = true;
                break;
            }
        }

        Level level = new Level(
                importedImporterLevel.identifier(),
                width,
                height,
                entitiesPosition,
                rupeesIndexes,
                hasChest
                );

        Move move = new Move(
                Arrays.binarySearch(entitiesPosition, playerPosition),
                importedPlayerPosition.getDirection(),
                Move.PlayerState.EMPTY,
                groundEntities,
                upEntities,
                new boolean[rupeesIndexes.length],
                null);
        return new PreparationResult(level, move);
    }

    private static int[] getRupees(@NotNull ImporterLevel importedImporterLevel,
                                   @NotNull Position[] entitiesPosition) {
        List<SimpleImporterEntity> rupeesEntities = importedImporterLevel.entities().rupee();
        if(rupeesEntities == null) {
            return new int[0];
        } else {
            return rupeesEntities.stream().mapToInt(simpleImporterEntity ->
                    Arrays.binarySearch(entitiesPosition, getPosition(simpleImporterEntity))
            ).toArray();
        }
    }

    @NotNull
    private static Position getPosition(@NotNull ImporterEntity importerEntity) {
        return new Position(getColumn(importerEntity), getLine(importerEntity));
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

        Optional<SimpleImporterEntity> enemySeeker = find(position, importedImporterLevel.entities().enemySeeker());
        if(enemySeeker.isPresent()) {
            return ENTITY_UP_ENEMY_SEEKER;
        }

        Optional<ImporterEntityWithDirection> enemyBasic = find(position, importedImporterLevel.entities().enemyBasic());
        if (enemyBasic.isPresent()) {
            switch (enemyBasic.get().getDirection()) {
                case Up -> {
                    return ENTITY_UP_ENEMY_BASIC_FACING_UP;
                }
                case Down -> {
                    return ENTITY_UP_ENEMY_BASIC_FACING_DOWN;
                }
                case Left -> {
                    return ENTITY_UP_ENEMY_BASIC_FACING_LEFT;
                }
                case Right -> {
                    return ENTITY_UP_ENEMY_BASIC_FACING_RIGHT;
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
