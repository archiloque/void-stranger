package net.archiloque.voidstranger.importer;

public record SimpleImporterEntity(int x, int y) implements ImporterEntity {
    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
