package net.archiloque.voidstranger;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

public record Position(int column, int line) implements Comparable<Position> {

    public static final Position DELTA_UP = new Position(0, -1);
    public static final Position DELTA_DOWN = new Position(0, 1);
    public static final Position DELTA_LEFT = new Position(-1, 0);
    public static final Position DELTA_RIGHT = new Position(1, 0);

    @Override
    public int compareTo(@NotNull Position position) {
        return Objects.compare(this, position, Comparator.comparingInt(Position::line).thenComparingInt(Position::column));
    }

    public Position add(@NotNull Position position) {
        return new Position(
                column + position.column,
                line + position.line
        );
    }
}
