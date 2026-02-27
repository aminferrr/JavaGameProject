package com.github.aminferrr.MyJavaGame.elements;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;

public class NPC {

    protected final float npcW = 16f;
    protected final float npcH = 16f;

    protected final float footW = 12f;
    protected final float footH = 6f;

    protected float x;
    protected float y;

    protected float stateTime = 0f;

    protected static final int FRAME_W = 16;
    protected static final int FRAME_H = 16;

    protected Texture texture;

    protected Animation<TextureRegion> walkUp;
    protected Animation<TextureRegion> walkDown;
    protected Animation<TextureRegion> walkLeft;
    protected Animation<TextureRegion> walkRight;

    protected TextureRegion idleUp;
    protected TextureRegion idleDown;
    protected TextureRegion idleLeft;
    protected TextureRegion idleRight;

    protected TextureRegion currentFrame;

    protected TiledMapTileLayer collisionLayer;

    protected float speed = 40f;
    protected boolean movingRight = true;
    protected boolean canMove = false;

    // ===== Патруль =====
    private float minX, maxX;

    /**
     * @param startX стартовая позиция X
     * @param startY стартовая позиция Y
     * @param minX минимальная X для патруля
     * @param maxX максимальная X для патруля
     * @param texturePath путь к изображению NPC
     */
    public NPC(float startX, float startY, float minX, float maxX, String texturePath) {
        this.x = startX;
        this.y = startY;
        this.minX = minX;
        this.maxX = maxX;

        // Загружаем текстуру
        texture = new Texture(texturePath);
        TextureRegion[][] frames = TextureRegion.split(texture, FRAME_W, FRAME_H);

        // ===== Настройка анимаций =====
        walkDown  = new Animation<>(0.2f, frames[0]);
        walkRight = new Animation<>(0.2f, frames[2]);
        walkUp    = new Animation<>(0.2f, frames[3]);
        walkLeft  = new Animation<>(0.2f, frames[1]);

        idleDown  = frames[0][1];
        idleRight = frames[1][1];
        idleUp    = frames[2][1];
        idleLeft  = frames[3][1];

        currentFrame = idleDown;
    }

    public void setCollisionLayer(TiledMapTileLayer layer) {
        this.collisionLayer = layer;
    }

    public void setCanMove(boolean value) {
        this.canMove = value;
    }

    public void update(float delta) {
        stateTime += delta;

        if (!canMove) {
            currentFrame = idleDown;
            return;
        }

        float dx = speed * delta;

        if (movingRight) {
            float nextX = x + dx;
            if (nextX >= maxX || isBlockedFeet(nextX, y)) {
                movingRight = false;
            } else {
                x = nextX;
                currentFrame = walkRight.getKeyFrame(stateTime, true);
            }
        } else {
            float nextX = x - dx;
            if (nextX <= minX || isBlockedFeet(nextX, y)) {
                movingRight = true;
            } else {
                x = nextX;
                currentFrame = walkLeft.getKeyFrame(stateTime, true);
            }
        }

        clampToWorld();
    }

    protected void clampToWorld() {
        if (collisionLayer == null) return;

        float mapWidth = collisionLayer.getWidth() * collisionLayer.getTileWidth();
        float mapHeight = collisionLayer.getHeight() * collisionLayer.getTileHeight();

        x = MathUtils.clamp(x, 0, mapWidth - npcW);
        y = MathUtils.clamp(y, 0, mapHeight - npcH);
    }

    protected boolean isBlockedFeet(float px, float py) {
        float footX = px + (npcW - footW) / 2f;
        float footY = py;
        float eps = 0.01f;

        return isBlockedPoint(footX, footY) ||
            isBlockedPoint(footX + footW - eps, footY) ||
            isBlockedPoint(footX, footY + footH - eps) ||
            isBlockedPoint(footX + footW - eps, footY + footH - eps);
    }

    protected boolean isBlockedPoint(float worldX, float worldY) {
        if (collisionLayer == null) return false;

        int cellX = (int)(worldX / collisionLayer.getTileWidth());
        int cellY = (int)(worldY / collisionLayer.getTileHeight());

        TiledMapTileLayer.Cell cell = collisionLayer.getCell(cellX, cellY);
        return cell != null && cell.getTile() != null;
    }

    public void render(Batch batch) {
        batch.draw(currentFrame, x, y, npcW, npcH);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return npcW; }
    public float getHeight() { return npcH; }

    public void dispose() {
        texture.dispose();
    }

    public void setPatrolRange(float minX, float maxX) {
        this.minX = minX;
        this.maxX = maxX;
    }
}
