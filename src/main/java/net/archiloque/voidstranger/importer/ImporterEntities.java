package net.archiloque.voidstranger.importer;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record ImporterEntities(
        @SerializedName("Boulder")
        @Nullable
        List<SimpleImporterEntity> boulder,

        @SerializedName("Chest")
        @Nullable
        List<SimpleImporterEntity> chest,

        @SerializedName("Down_stairs")
        @Nullable
        List<SimpleImporterEntity> downStairs,

        @SerializedName("Enemy")
        @Nullable
        List<ImporterEntityWithDirection> enemy,

        @SerializedName("Glass")
        @Nullable
        List<SimpleImporterEntity> glass,

        @SerializedName("Ground")
        @Nullable
        List<SimpleImporterEntity> ground,

        @SerializedName("Hole")
        @Nullable
        List<SimpleImporterEntity> hole,

        @SerializedName("Player_start_position")
        @Nullable
        List<ImporterEntityWithDirection> playerStartPosition,

        @SerializedName("Rupee")
        @Nullable
        List<SimpleImporterEntity> rupee,

        @SerializedName("Wall")
        @Nullable
        List<SimpleImporterEntity> wall
) {

    public Stream<ImporterEntity> getAllEntities() {
        return Stream.of(
                boulder,
                chest,
                downStairs,
                enemy,
                glass,
                ground,
                hole,
                playerStartPosition,
                rupee,
                wall
        ).filter(Objects::nonNull).flatMap(Collection::stream);
    }

    public Stream<ImporterEntity> getGroundEntities() {
        return Stream.of(
                downStairs,
                glass,
                ground,
                hole
        ).filter(Objects::nonNull).flatMap(Collection::stream);
    }

}
