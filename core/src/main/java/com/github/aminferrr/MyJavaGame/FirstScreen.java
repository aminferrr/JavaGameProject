package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class FirstScreen extends ScreenAdapter {

    private final Main game;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    // ===== MAP =====
    private static final int MAP_TILES_W = 80;
    private static final int MAP_TILES_H = 45;
    private static final float TILE = 16f;

    private static final float WORLD_W = MAP_TILES_W * TILE;
    private static final float WORLD_H = MAP_TILES_H * TILE;

    // ===== CAMERA VIEW =====
    private static final float VIEW_W = 640f;
    private static final float VIEW_H = 360f;

    // ===== COLLISION: ONLY BUILDS =====
    private TiledMapTileLayer builds;

    // ===== PLAYER (логический хитбокс) =====
    private float playerW = 16f;
    private float playerH = 20f;

    private float playerX = 100f;
    private float playerY = 60f;

    // ===== FOOT HITBOX (коллизия только по ногам) =====
    // ширина ног чуть меньше, высота ног маленькая
    private float footW = 12f;
    private float footH = 6f;

    private float stateTime = 0f;

    // ===== SPRITE =====
    private static final int FRAME_W = 48;
    private static final int FRAME_H = 64;

    private static final float DRAW_W = 48f;
    private static final float DRAW_H = 64f;

    private Texture idleDownSheet;
    private Texture walkDownSheet;

    private Animation<TextureRegion> idleDownAnim;
    private Animation<TextureRegion> walkDownAnim;

    public FirstScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.apply(true);

        map = new TmxMapLoader().load("maps/MainMap/map..tmx");
        renderer = new OrthogonalTiledMapRenderer(map, 1f);

        builds = getTileLayer(map, "builds");

        idleDownSheet = new Texture("characters/player/Idle/Idle_Down.png");
        walkDownSheet = new Texture("characters/player/Walk/walk_Down.png");

        idleDownAnim = makeAnimAuto(idleDownSheet, FRAME_W, FRAME_H, 0.18f);
        walkDownAnim = makeAnimAuto(walkDownSheet, FRAME_W, FRAME_H, 0.12f);

        updateCameraClamped();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new PlayingScreen(game));
            return;
        }
        stateTime += delta;

        boolean moving = updatePlayerMovement(delta);
        updateCameraClamped();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.setView(camera);
        renderer.render();

        TextureRegion frame = moving
            ? walkDownAnim.getKeyFrame(stateTime, true)
            : idleDownAnim.getKeyFrame(stateTime, true);

        float drawX = playerX - (DRAW_W - playerW) / 2f;
        float drawY = playerY - (DRAW_H - playerH);

        renderer.getBatch().begin();
        renderer.getBatch().draw(frame, drawX, drawY, DRAW_W, DRAW_H);
        renderer.getBatch().end();
    }

    private boolean updatePlayerMovement(float delta) {
        float speed = 140f;

        float dx = 0f, dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  dx -= speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  dy -= speed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    dy += speed * delta;

        boolean moving = (dx != 0f || dy != 0f);

        if (builds == null) {
            playerX += dx;
            playerY += dy;
            clampPlayerToWorld();
            return moving;
        }

        // X отдельно
        float nextX = playerX + dx;
        if (!isBlockedFeet(nextX, playerY)) playerX = nextX;

        // Y отдельно
        float nextY = playerY + dy;
        if (!isBlockedFeet(playerX, nextY)) playerY = nextY;

        clampPlayerToWorld();
        return moving;
    }

    // ===== CAMERA FOLLOW + CLAMP =====
    private void updateCameraClamped() {
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;

        float targetX = playerX + playerW / 2f;
        float targetY = playerY + playerH / 2f;

        float camX = MathUtils.clamp(targetX, halfW, WORLD_W - halfW);
        float camY = MathUtils.clamp(targetY, halfH, WORLD_H - halfH);

        camera.position.set(camX, camY, 0);
        camera.update();
    }

    private void clampPlayerToWorld() {
        playerX = MathUtils.clamp(playerX, 0, WORLD_W - playerW);
        playerY = MathUtils.clamp(playerY, 0, WORLD_H - playerH);
    }

    // ===== COLLISION BY FEET ONLY =====
    private boolean isBlockedFeet(float px, float py) {
        // ноги находятся внизу хитбокса
        float footX = px + (playerW - footW) / 2f;
        float footY = py; // самый низ

        float eps = 0.01f;

        return isBlockedPoint(footX,              footY) ||
            isBlockedPoint(footX + footW - eps, footY) ||
            isBlockedPoint(footX,              footY + footH - eps) ||
            isBlockedPoint(footX + footW - eps, footY + footH - eps);
    }

    private boolean isBlockedPoint(float worldX, float worldY) {
        int tileX = (int)(worldX / TILE);
        int tileY = (int)(worldY / TILE);

        TiledMapTileLayer.Cell cell = builds.getCell(tileX, tileY);
        return cell != null && cell.getTile() != null;
    }

    // ===== ANIMATION =====
    private Animation<TextureRegion> makeAnimAuto(Texture sheet, int frameW, int frameH, float frameDuration) {
        int columns = sheet.getWidth() / frameW;
        TextureRegion[][] grid = TextureRegion.split(sheet, frameW, frameH);

        TextureRegion[] frames = new TextureRegion[columns];
        for (int i = 0; i < columns; i++) frames[i] = grid[0][i];

        return new Animation<>(frameDuration, frames);
    }

    private TiledMapTileLayer getTileLayer(TiledMap map, String name) {
        MapLayer layer = map.getLayers().get(name);
        if (layer instanceof TiledMapTileLayer) return (TiledMapTileLayer) layer;
        return null;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        updateCameraClamped();
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (map != null) map.dispose();
        if (idleDownSheet != null) idleDownSheet.dispose();
        if (walkDownSheet != null) walkDownSheet.dispose();
    }

}
