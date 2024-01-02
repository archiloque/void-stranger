package net.archiloque.voidstranger;

public interface UpEntity {
    char ENTITY_UP_EMPTY = ' ';
    char ENTITY_UP_BOULDER = 'B';
    char ENTITY_UP_CHEST_CLOSED = 'C';
    char ENTITY_UP_CHEST_OPEN = 'c';
    char ENTITY_UP_ENEMY_FACING_DOWN = 'd';
    char ENTITY_UP_ENEMY_FACING_LEFT = 'l';
    char ENTITY_UP_ENEMY_FACING_RIGHT = 'r';
    char ENTITY_UP_ENEMY_FACING_UP = 'u';

    static boolean isEnemy(char charEntity) {
        return (charEntity == ENTITY_UP_ENEMY_FACING_DOWN) || (charEntity == ENTITY_UP_ENEMY_FACING_UP) || (charEntity == ENTITY_UP_ENEMY_FACING_LEFT) || (charEntity == ENTITY_UP_ENEMY_FACING_RIGHT);
    }
}
