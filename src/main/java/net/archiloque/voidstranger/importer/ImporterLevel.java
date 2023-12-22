package net.archiloque.voidstranger.importer;

import com.google.gson.annotations.SerializedName;

public record ImporterLevel(
        @SerializedName("identifier")
        String identifier,

        @SerializedName("entities")
        ImporterEntities entities) {

    public java.util.stream.Stream<ImporterEntity> getAllEntities() {
        return entities().getAllEntities();
    }
}
