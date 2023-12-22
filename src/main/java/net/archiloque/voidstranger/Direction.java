package net.archiloque.voidstranger;

public enum Direction {

    Up(Position.DELTA_UP),
    Down(Position.DELTA_DOWN),
    Left(Position.DELTA_LEFT),
    Right(Position.DELTA_RIGHT);

    public final Position delta;

    Direction(Position delta) {
        this.delta = delta;
    }
}
