package net.archiloque.voidstranger;

public interface CharEntity {
    char ENTITY_BOULDER = 'B';
    char ENTITY_CHEST_CLOSED = 'C';
    char ENTITY_CHEST_OPEN = 'c';
    char ENTITY_ENEMY_FACING_DOWN = 'd';
    char ENTITY_ENEMY_FACING_LEFT = 'l';
    char ENTITY_ENEMY_FACING_RIGHT =  'r';
    char ENTITY_ENEMY_FACING_UP = 'u';
    char ENTITY_GROUND = ' ';
    char ENTITY_HOLE = 'x';
    char ENTITY_GLASS = 'G';

    static boolean isEnemy(char charEntity) {
        return (charEntity == ENTITY_ENEMY_FACING_DOWN) || (charEntity == ENTITY_ENEMY_FACING_UP) || (charEntity == ENTITY_ENEMY_FACING_LEFT) || (charEntity == ENTITY_ENEMY_FACING_RIGHT);
    }
}
