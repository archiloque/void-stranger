package net.archiloque.voidstranger;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ToString
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY, callSuper = false, doNotUseGetters = true)
public final class Move {
    @NotNull
    final Position playerPosition;
    @NotNull
    final Direction playerDirection;
    @NotNull
    final PlayerState playerState;
    final char @NotNull [] entities;

    @Nullable
    @EqualsAndHashCode.Exclude
    final ActionLinkedElement actions;

    public Move(
            @NotNull Position playerPosition,
            @NotNull Direction playerDirection,
            @NotNull PlayerState playerState,
            char @NotNull [] entities,
            @Nullable ActionLinkedElement actions) {
        this.playerPosition = playerPosition;
        this.playerDirection = playerDirection;
        this.playerState = playerState;
        this.entities = entities;
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
        STANDARD('!'),
        HOLD_GROUND(CharEntity.ENTITY_GROUND),
        HOLD_GLASS(CharEntity.ENTITY_GLASS),
        ;

        public final char entity;

        PlayerState(char entity) {
            this.entity = entity;
        }
    }



    public record ActionLinkedElement(@NotNull Action action, @Nullable ActionLinkedElement parent) {
    }
}
