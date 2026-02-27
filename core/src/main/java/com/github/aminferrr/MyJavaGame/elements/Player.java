package com.github.aminferrr.MyJavaGame.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;

public class Player {

    // ===== Размеры =====
    private final float playerW = 16f;
    private final float playerH = 32f;

    // ===== Хитбокс ног =====
    private final float footW = 12f;
    private final float footH = 6f;

    private float playerX = 230f;
    private float playerY = 60f;

    private float stateTime = 0f;

    private static final int FRAME_W = 16;
    private static final int FRAME_H = 32;

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

    // Последнее направление
    private enum Direction {UP, DOWN, LEFT, RIGHT}
    private Direction lastDirection = Direction.DOWN;

    private final PlayerStats stats;

    // ===== Коллизии =====
    private TiledMapTileLayer collisionLayer;
    private NPC npc; // <-- добавили NPC

    public Player(PlayerStats stats) {
        this.stats = stats;

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

    public void setNpc(NPC npc) {
        this.npc = npc;
    }

    public void update(float delta) {

        stateTime += delta;
        float speed = stats.getSpeed();

        float dx = 0f;
        float dy = 0f;
        boolean moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= speed * delta;
            currentFrame = walkLeft.getKeyFrame(stateTime, true);
            lastDirection = Direction.LEFT;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += speed * delta;
            currentFrame = walkRight.getKeyFrame(stateTime, true);
            lastDirection = Direction.RIGHT;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += speed * delta;
            currentFrame = walkUp.getKeyFrame(stateTime, true);
            lastDirection = Direction.UP;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= speed * delta;
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

        // ===== Idle =====
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

    // ===== Коллизия с NPC =====
    private boolean isCollidingWithNPC(float nextX, float nextY) {

        if (npc == null) return false;

        float footX = nextX + (playerW - footW) / 2f;
        float footY = nextY;

        return footX < npc.getX() + npc.getWidth() &&
            footX + footW > npc.getX() &&
            footY < npc.getY() + npc.getHeight() &&
            footY + footH > npc.getY();
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
        batch.draw(currentFrame, playerX, playerY, playerW, playerH);
    }

    public float getX() { return playerX; }
    public float getY() { return playerY; }
    public float getWidth() { return playerW; }
    public float getHeight() { return playerH; }

    public void dispose() {
        texture.dispose();
    }
}
