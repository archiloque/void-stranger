package net.archiloque.voidstranger.importer;


import com.google.gson.annotations.SerializedName;
import net.archiloque.voidstranger.Direction;

public record ImporterEntityWithDirection(int x, int y,
                                          @SerializedName("customFields") CustomFields customFields) implements ImporterEntity {
    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    public Direction getDirection() {
        return customFields.direction();
    }
}
