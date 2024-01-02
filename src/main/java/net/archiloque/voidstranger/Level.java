package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record Level(
        @NotNull String identifier,
        int width,
        int height,
        @NotNull Position[] positions
) {

    public int positionIndex(@NotNull Position position) {
        return Arrays.binarySearch(positions, position);
    }

}
