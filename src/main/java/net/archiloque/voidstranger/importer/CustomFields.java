package net.archiloque.voidstranger.importer;

import com.google.gson.annotations.SerializedName;
import net.archiloque.voidstranger.Direction;

public record CustomFields(@SerializedName("Direction") Direction direction) {
}
