package net.archiloque.voidstranger;

public interface UpEntity {
    char ENTITY_UP_EMPTY = ' ';
    char ENTITY_UP_BOULDER = 'B';
    char ENTITY_UP_CHEST_CLOSED = 'C';
    char ENTITY_UP_CHEST_OPEN = 'c';
    char ENTITY_UP_ENEMY_BASIC_FACING_DOWN = 'd';
    char ENTITY_UP_ENEMY_BASIC_FACING_LEFT = 'l';
    char ENTITY_UP_ENEMY_BASIC_FACING_RIGHT = 'r';
    char ENTITY_UP_ENEMY_BASIC_FACING_UP = 'u';

    char ENTITY_UP_ENEMY_SEEKER = 's';

    static boolean isEnemy(char charEntity) {
        switch (charEntity) {
            case ENTITY_UP_ENEMY_SEEKER, ENTITY_UP_ENEMY_BASIC_FACING_DOWN, ENTITY_UP_ENEMY_BASIC_FACING_UP, ENTITY_UP_ENEMY_BASIC_FACING_LEFT, ENTITY_UP_ENEMY_BASIC_FACING_RIGHT -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
