package com.github.aminferrr.MyJavaGame.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class Player {

    // ===== Размеры =====
    private final float playerW = 16f;
    private final float playerH = 32f;

    // ===== Хитбокс ног =====
    private final float footW = 12f;
    private final float footH = 6f;

    private float playerX = 230f;
    private float playerY = 60f;
    private float spawnX = 230f;
    private float spawnY = 60f;

    private float stateTime = 0f;

    private static final int FRAME_W = 16;
    private static final int FRAME_H = 32;

    // ===== Здоровье и статус =====
    private int health = 100;
    private int maxHealth = 100;
    private boolean alive = true;
    private float speed = 200f;

    // ===== Атака =====
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private static final float ATTACK_DURATION = 0.3f;

    // ===== Звуки =====
    private Sound attackSound;
    private Sound jumpSound;
    private Sound hurtSound;

    // ===== Анимации =====
    private final Texture texture;
    private final Animation<TextureRegion> walkUp;
    private final Animation<TextureRegion> walkDown;
    private final Animation<TextureRegion> walkLeft;
    private final Animation<TextureRegion> walkRight;
    private final TextureRegion idleUp;
    private final TextureRegion idleDown;
    private final TextureRegion idleLeft;
    private final TextureRegion idleRight;
    private TextureRegion currentFrame;

    private enum Direction {UP, DOWN, LEFT, RIGHT}
    private Direction lastDirection = Direction.DOWN;

    private final PlayerStats stats;

    // ===== Коллизии =====
    private TiledMapTileLayer collisionLayer;
    private List<NPC> npcs = new ArrayList<>();

    public Player(PlayerStats stats) {
        this.stats = stats;
        this.health = stats.getHp();
        this.maxHealth = stats.getHp();
        this.speed = stats.getSpeed();

        texture = new Texture("character.png");
        TextureRegion[][] frames = TextureRegion.split(texture, FRAME_W, FRAME_H);

        walkDown  = new Animation<>(0.15f, frames[0]);
        walkRight = new Animation<>(0.15f, frames[1]);
        walkUp    = new Animation<>(0.15f, frames[2]);
        walkLeft  = new Animation<>(0.15f, frames[3]);

        idleDown  = frames[0][1];
        idleRight = frames[1][1];
        idleUp    = frames[2][1];
        idleLeft  = frames[3][1];

        currentFrame = idleDown;
    }

    public void setCollisionLayer(TiledMapTileLayer layer) {
        this.collisionLayer = layer;
    }

    public void setNpcs(List<NPC> npcs) {
        this.npcs = npcs;
    }

    public void setSounds(Sound attack, Sound jump, Sound hurt) {
        this.attackSound = attack;
        this.jumpSound = jump;
        this.hurtSound = hurt;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setPosition(float x, float y) {
        this.playerX = x;
        this.playerY = y;
        this.spawnX = x;
        this.spawnY = y;
    }

    public void attack() {
        if (!isAttacking) {
            isAttacking = true;
            attackTimer = 0f;
            if (attackSound != null) {
                attackSound.play();
            }
        }
    }

    public void jump() {
        // В этой реализации прыжка нет, но оставим метод для совместимости
        if (jumpSound != null) {
            jumpSound.play();
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (hurtSound != null) {
            hurtSound.play();
        }
        if (health <= 0) {
            alive = false;
        }
    }

    public boolean isDead() {
        return !alive || health <= 0;
    }

    public void respawn() {
        playerX = spawnX;
        playerY = spawnY;
        health = maxHealth;
        alive = true;
    }

    public void update(float delta) {
        if (!alive) return;

        stateTime += delta;

        // Обновление атаки
        if (isAttacking) {
            attackTimer += delta;
            if (attackTimer >= ATTACK_DURATION) {
                isAttacking = false;
            }
        }

        float currentSpeed = speed * delta;

        float dx = 0f;
        float dy = 0f;
        boolean moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= currentSpeed;
            currentFrame = walkLeft.getKeyFrame(stateTime, true);
            lastDirection = Direction.LEFT;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += currentSpeed;
            currentFrame = walkRight.getKeyFrame(stateTime, true);
            lastDirection = Direction.RIGHT;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += currentSpeed;
            currentFrame = walkUp.getKeyFrame(stateTime, true);
            lastDirection = Direction.UP;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= currentSpeed;
            currentFrame = walkDown.getKeyFrame(stateTime, true);
            lastDirection = Direction.DOWN;
            moving = true;
        }

        // ===== Коллизия X =====
        float nextX = playerX + dx;
        if (!isBlockedFeet(nextX, playerY) && !isCollidingWithNPC(nextX, playerY)) {
            playerX = nextX;
        }

        // ===== Коллизия Y =====
        float nextY = playerY + dy;
        if (!isBlockedFeet(playerX, nextY) && !isCollidingWithNPC(playerX, nextY)) {
            playerY = nextY;
        }

        clampToWorld();

        if (!moving) {
            stateTime = 0f;
            switch (lastDirection) {
                case UP -> currentFrame = idleUp;
                case DOWN -> currentFrame = idleDown;
                case LEFT -> currentFrame = idleLeft;
                case RIGHT -> currentFrame = idleRight;
            }
        }
    }

    private boolean isCollidingWithNPC(float nextX, float nextY) {
        if (npcs == null || npcs.isEmpty()) return false;

        float footX = nextX + (playerW - footW) / 2f;
        float footY = nextY;

        for (NPC npc : npcs) {
            float npcFootW = 12f;
            float npcFootH = 6f;
            float npcFootX = npc.getX() + (npc.getWidth() - npcFootW) / 2f;
            float npcFootY = npc.getY();

            boolean overlap = footX < npcFootX + npcFootW &&
                footX + footW > npcFootX &&
                footY < npcFootY + npcFootH &&
                footY + footH > npcFootY;

            if (overlap) return true;
        }
        return false;
    }

    private void clampToWorld() {
        if (collisionLayer == null) return;

        float mapWidth = collisionLayer.getWidth() * collisionLayer.getTileWidth();
        float mapHeight = collisionLayer.getHeight() * collisionLayer.getTileHeight();

        playerX = MathUtils.clamp(playerX, 0, mapWidth - playerW);
        playerY = MathUtils.clamp(playerY, 0, mapHeight - playerH);
    }

    private boolean isBlockedFeet(float px, float py) {
        float footX = px + (playerW - footW) / 2f;
        float footY = py;
        float eps = 0.01f;

        return isBlockedPoint(footX, footY) ||
            isBlockedPoint(footX + footW - eps, footY) ||
            isBlockedPoint(footX, footY + footH - eps) ||
            isBlockedPoint(footX + footW - eps, footY + footH - eps);
    }

    private boolean isBlockedPoint(float worldX, float worldY) {
        if (collisionLayer == null) return false;

        int cellX = (int)(worldX / collisionLayer.getTileWidth());
        int cellY = (int)(worldY / collisionLayer.getTileHeight());

        TiledMapTileLayer.Cell cell = collisionLayer.getCell(cellX, cellY);
        return cell != null && cell.getTile() != null;
    }

    public void render(Batch batch) {
        if (!alive) return;
        batch.draw(currentFrame, playerX, playerY, playerW, playerH);
    }

    // ===== Геттеры =====
    public float getX() { return playerX; }
    public float getY() { return playerY; }
    public float getWidth() { return playerW; }
    public float getHeight() { return playerH; }
    public float getSpeed() { return speed; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isAlive() { return alive; }
    public boolean isAttacking() { return isAttacking; }

    public void dispose() {
        texture.dispose();
    }
}
