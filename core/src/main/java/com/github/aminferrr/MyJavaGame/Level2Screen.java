package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Экран второго уровня: карта map3.tmx
 * Слой buildings = коллизия
 * Слои enemy3 = Toaster Bot (летающие)
 * Слои enemy4 = Wheel Bot (летающие)
 */
public class Level2Screen extends ScreenAdapter implements InputProcessor {

    private final Main game;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    private static final float VIEW_W = 40f;
    private static final float VIEW_H = 22.5f;
    private final float PPM = 16f;

    private World world;
    private Box2DDebugRenderer debugRenderer;

    private Player player;
    private Array<Enemy> enemies;

    private SpriteBatch batch;
    private BitmapFont font;

    private boolean leftPressed, rightPressed, jumpPressed, attackPressed;

    private float mapWidth;
    private float mapHeight;

    private float cameraLerp = 0.1f;
    private boolean debugMode = false;

    private boolean playerGrounded = false;

    private int score = 0;
    private Vector2 playerSpawn;
    private float playerAttackCooldown = 0.4f;
    private float playerAttackTimer = 0f;
    private float playerAttackRange = 1.5f;

    public Level2Screen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.apply();

        map = new TmxMapLoader().load("maps/mapAsset2/map3.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        world = new World(new Vector2(0, -15f), true);
        debugRenderer = new Box2DDebugRenderer();

        setupContactListener();

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(0.06f);

        int tilesW = map.getProperties().get("width", Integer.class);
        int tilesH = map.getProperties().get("height", Integer.class);
        int tileW  = map.getProperties().get("tilewidth", Integer.class);
        int tileH  = map.getProperties().get("tileheight", Integer.class);

        mapWidth  = (tilesW * tileW) / PPM;
        mapHeight = (tilesH * tileH) / PPM;

        // Выводим список слоев для отладки
        Gdx.app.log("MAP", "Слои карты:");
        for (MapLayer layer : map.getLayers()) {
            Gdx.app.log("MAP", " - " + layer.getName());
        }

        createCollisionsFromBuildingsLayer();

        player = new Player(world);
        playerSpawn = player.body.getPosition().cpy();

        enemies = new Array<>();
        createEnemiesFromTiled();

        // Устанавливаем обработчик ввода
        Gdx.input.setInputProcessor(this);

        updateCamera();
    }

    private void createCollisionsFromBuildingsLayer() {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("buildings");
        if (layer == null) {
            Gdx.app.error("ERROR", "Слой 'buildings' не найден в map3.tmx!");
            return;
        }

        int collisionCount = 0;
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    BodyDef bodyDef = new BodyDef();
                    bodyDef.type = BodyDef.BodyType.StaticBody;
                    bodyDef.position.set(
                        (x + 0.5f) * layer.getTileWidth() / PPM,
                        (y + 0.5f) * layer.getTileHeight() / PPM
                    );

                    Body body = world.createBody(bodyDef);

                    PolygonShape shape = new PolygonShape();
                    shape.setAsBox(layer.getTileWidth() / 2f / PPM, layer.getTileHeight() / 2f / PPM);

                    FixtureDef fixtureDef = new FixtureDef();
                    fixtureDef.shape = shape;
                    fixtureDef.friction = 0.5f;
                    fixtureDef.restitution = 0f;

                    Fixture fixture = body.createFixture(fixtureDef);
                    fixture.setUserData("collision");
                    shape.dispose();

                    collisionCount++;
                }
            }
        }
        Gdx.app.log("COLLISION", "Создано коллизий: " + collisionCount);
    }

    private void setupContactListener() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                Object ua = a.getUserData();
                Object ub = b.getUserData();

                if ("foot".equals(ua) || "foot".equals(ub)) {
                    playerGrounded = true;
                }

                // Проверка на урон от врагов
                if (("player".equals(ua) && "enemy".equals(ub)) ||
                    ("enemy".equals(ua) && "player".equals(ub))) {
                    if (player.alive) {
                        player.takeDamage(10);
                        Gdx.app.log("DAMAGE", "Игрок получил урон!");
                    }
                }
            }

            @Override
            public void endContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                Object ua = a.getUserData();
                Object ub = b.getUserData();

                if ("foot".equals(ua) || "foot".equals(ub)) {
                    playerGrounded = false;
                }
            }

            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }


    private void createEnemiesFromTiled() {
        // enemy3 → Toaster Bot (точки)
        MapLayer enemy3Layer = map.getLayers().get("enemy3");
        if (enemy3Layer != null) {
            Gdx.app.log("ENEMY", "Слой 'enemy3' содержит " + enemy3Layer.getObjects().getCount() + " объектов (Toaster Bot)");

            for (MapObject obj : enemy3Layer.getObjects()) {
                float x, y;

                // Получаем координаты в зависимости от типа объекта
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    x = rect.x + rect.width/2f;
                    y = rect.y + rect.height/2f;
                } else {
                    // Для точечных объектов
                    x = obj.getProperties().get("x", Float.class);
                    y = obj.getProperties().get("y", Float.class);
                }

                // ПРЯМОЕ преобразование в Box2D координаты (БЕЗ ИНВЕРСИИ!)
                float box2dX = x / PPM;
                float box2dY = y / PPM;  // Просто делим на PPM, как в первом уровне

                Gdx.app.log("ENEMY_DEBUG", "Toaster Bot - позиция Y: " + box2dY);

                // Для Toaster Bot используем тип "zapper"
                String enemyType = "zapper";

                Gdx.app.log("ENEMY", String.format(
                    "Создан Toaster Bot: позиция (%.2f, %.2f) из Tiled (%.0f, %.0f)",
                    box2dX, box2dY, x, y));

                enemies.add(new Enemy(world, new Vector2(box2dX, box2dY), enemyType));
            }
        } else {
            Gdx.app.log("ENEMY", "Слой 'enemy3' не найден");
        }

        // enemy4 → Wheel Bot (точки)
        MapLayer enemy4Layer = map.getLayers().get("enemy4");
        if (enemy4Layer != null) {
            Gdx.app.log("ENEMY", "Слой 'enemy4' содержит " + enemy4Layer.getObjects().getCount() + " объектов (Wheel Bot)");

            for (MapObject obj : enemy4Layer.getObjects()) {
                float x, y;

                // Получаем координаты в зависимости от типа объекта
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    x = rect.x + rect.width/2f;
                    y = rect.y + rect.height/2f;
                } else {
                    // Для точечных объектов
                    x = obj.getProperties().get("x", Float.class);
                    y = obj.getProperties().get("y", Float.class);
                }

                // ПРЯМОЕ преобразование в Box2D координаты (БЕЗ ИНВЕРСИИ!)
                float box2dX = x / PPM;
                float box2dY = y / PPM;  // Просто делим на PPM, как в первом уровне

                Gdx.app.log("ENEMY_DEBUG", "Wheel Bot - позиция Y: " + box2dY);

                // Для Wheel Bot используем тип "wheel"
                String enemyType = "wheel";

                Gdx.app.log("ENEMY", String.format(
                    "Создан Wheel Bot: позиция (%.2f, %.2f) из Tiled (%.0f, %.0f)",
                    box2dX, box2dY, x, y));

                enemies.add(new Enemy(world, new Vector2(box2dX, box2dY), enemyType));
            }
        } else {
            Gdx.app.log("ENEMY", "Слой 'enemy4' не найден");
        }

        Gdx.app.log("ENEMY", "Всего создано врагов: " + enemies.size);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1);

        world.step(1/60f, 6, 2);

        playerAttackTimer += delta;
        player.update(delta, leftPressed, rightPressed, jumpPressed, attackPressed, playerGrounded);

        for (Enemy enemy : enemies) {
            if (enemy.alive) enemy.update(delta, player);
        }

        // Атака игрока
        if (attackPressed && playerAttackTimer >= playerAttackCooldown && player.alive) {
            boolean hit = false;
            Vector2 pPos = player.body.getPosition();
            for (Enemy e : enemies) {
                if (!e.alive) continue;
                if (pPos.dst(e.body.getPosition()) <= playerAttackRange) {
                    e.takeDamage(25);
                    hit = true;
                    Gdx.app.log("ATTACK", "Попадание по врагу!");
                }
            }
            if (hit) playerAttackTimer = 0f;
        }

        // Удаление мертвых врагов
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.alive) {
                enemies.removeIndex(i);
                score++;
                Gdx.app.log("SCORE", "Враг уничтожен! Счет: " + score);
            }
        }

        // Проверка смерти игрока
        Vector2 pos = player.body.getPosition();
        if (pos.y < -2f || !player.alive) {
            respawnPlayer();
        }

        updateCamera();
        renderer.setView(camera);
        renderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        for (Enemy e : enemies) e.render(batch);

        // Отображение счета и здоровья
        font.draw(batch, "Score: " + score,
            camera.position.x - VIEW_W/2f + 0.5f,
            camera.position.y + VIEW_H/2f - 0.5f);
        font.draw(batch, "HP: " + player.health,
            camera.position.x - VIEW_W/2f + 0.5f,
            camera.position.y + VIEW_H/2f - 1.5f);
        batch.end();

        if (debugMode) debugRenderer.render(world, camera.combined);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugMode = !debugMode;
            Gdx.app.log("DEBUG", "Режим отладки: " + (debugMode ? "вкл" : "выкл"));
        }
    }

    private void updateCamera() {
        float halfW = viewport.getWorldWidth()/2f;
        float halfH = viewport.getWorldHeight()/2f;
        Vector2 pos = player.body.getPosition();
        float targetX = MathUtils.clamp(pos.x, halfW, mapWidth-halfW);
        float targetY = MathUtils.clamp(pos.y, halfH, mapHeight-halfH);

        camera.position.x += (targetX - camera.position.x)*cameraLerp;
        camera.position.y += (targetY - camera.position.y)*cameraLerp;
        camera.update();
    }

    private void respawnPlayer() {
        if (playerSpawn == null) return;
        player.body.setTransform(playerSpawn, 0);
        player.body.setLinearVelocity(0, 0);
        player.health = 100;
        player.alive = true;
        playerGrounded = false;
        Gdx.app.log("PLAYER", "Респаун игрока");
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
        world.dispose();
        debugRenderer.dispose();
        batch.dispose();
        player.dispose();
        for (Enemy e : enemies) e.dispose();
        if (font != null) font.dispose();
    }

    // ===== InputProcessor =====
    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.A) leftPressed = true;
        if (keycode == Input.Keys.D) rightPressed = true;
        if (keycode == Input.Keys.SPACE) jumpPressed = true;
        if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) attackPressed = true;
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.A) leftPressed = false;
        if (keycode == Input.Keys.D) rightPressed = false;
        if (keycode == Input.Keys.SPACE) jumpPressed = false;
        if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) attackPressed = false;
        return true;
    }

    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
}
