package net.archiloque.voidstranger;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.archiloque.voidstranger.GroundEntity.*;

@ToString
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, callSuper = false, doNotUseGetters = true)
public final class Move {
    final int playerPositionIndex;
    @NotNull
    final Direction playerDirection;
    @NotNull
    final PlayerState playerState;
    final char @NotNull [] groundEntities;
    final char @NotNull [] upEntities;

    final boolean @NotNull [] rupeesFound;

    @Nullable
    @EqualsAndHashCode.Exclude
    final ActionLinkedElement actions;

    public Move(
            int playerPositionIndex,
            @NotNull Direction playerDirection,
            @NotNull PlayerState playerState,
            char @NotNull [] groundEntities,
            char @NotNull [] upEntities,
            boolean @NotNull [] rupeesFound,
            @Nullable ActionLinkedElement actions
    ) {
        this.playerPositionIndex = playerPositionIndex;
        this.playerDirection = playerDirection;
        this.playerState = playerState;
        this.groundEntities = groundEntities;
        this.upEntities = upEntities;
        this.rupeesFound = rupeesFound;
        this.actions = actions;
    }

    public List<Action> getActions() {
        Move.ActionLinkedElement currentAction = actions;
        List<Action> actionsList = new ArrayList<>();
        while (currentAction != null) {
            actionsList.add(currentAction.action());
            currentAction = currentAction.parent();
        }
        return actionsList.reversed();
    }

    public enum PlayerState {
        EMPTY('!'),
        HOLD_GROUND(ENTITY_GROUND_GROUND),
        HOLD_GLASS(GroundEntity.ENTITY_GROUND_GLASS),
        HOLD_DOWNSTAIR(ENTITY_GROUND_DOWNSTAIR),
        ;

        public final char entity;

        PlayerState(char entity) {
            this.entity = entity;
        }
    }

    public static PlayerState fromGroundEntity(char groundEntity) {
        switch (groundEntity) {
            case ENTITY_GROUND_GROUND -> {
                return PlayerState.HOLD_GROUND;
            }
            case ENTITY_GROUND_GLASS -> {
                return PlayerState.HOLD_GLASS;
            }
            case ENTITY_GROUND_DOWNSTAIR -> {
                return PlayerState.HOLD_DOWNSTAIR;
            }
            default -> throw new IllegalStateException("Unexpected value: " + groundEntity);
        }
    }


    public record ActionLinkedElement(@NotNull Action action, @Nullable ActionLinkedElement parent) {
    }
}
