package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

public class PlayingScreen extends ScreenAdapter implements InputProcessor {

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

    // ===== ДОБАВЛЕНО: для отслеживания земли =====
    private boolean playerGrounded = false;

    // Боевая система игрока
    private int score = 0;
    private Vector2 playerSpawn;
    private float playerAttackCooldown = 0.4f;
    private float playerAttackTimer = 0f;
    private float playerAttackRange = 1.5f;

    public PlayingScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.apply();

        map = new TmxMapLoader().load("maps/mapAsset2/PlayingMap.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        world = new World(new Vector2(0, -15f), true);
        debugRenderer = new Box2DDebugRenderer();

        setupContactListener(); // ← теперь используется обновленный listener

        batch = new SpriteBatch();
        font = new BitmapFont();
        // Шрифт сильно уменьшаем под масштаб мира (PPM = 16)
        font.getData().setScale(0.06f);

        int tilesW = map.getProperties().get("width", Integer.class);
        int tilesH = map.getProperties().get("height", Integer.class);
        int tileW  = map.getProperties().get("tilewidth", Integer.class);
        int tileH  = map.getProperties().get("tileheight", Integer.class);

        mapWidth  = (tilesW * tileW) / PPM;
        mapHeight = (tilesH * tileH) / PPM;

        createCollisionsFromTileLayer();

        player = new Player(world);
        playerSpawn = player.body.getPosition().cpy();

        enemies = new Array<>();
        createEnemiesFromTiled();

        Gdx.input.setInputProcessor(this);

        updateCamera();

        Gdx.app.log("INFO", "Игра загружена. Размер карты: " + mapWidth + " x " + mapHeight);
    }

    private void createCollisionsFromTileLayer() {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("collis");
        if (layer == null) {
            Gdx.app.error("ERROR", "Слой 'collis' не найден!");
            return;
        }

        int collisionCount = 0;
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    BodyDef bodyDef = new BodyDef();
                    bodyDef.type = BodyDef.BodyType.StaticBody;
                    bodyDef.position.set((x + 0.5f) * layer.getTileWidth() / PPM,
                        (y + 0.5f) * layer.getTileHeight() / PPM);

                    Body body = world.createBody(bodyDef);

                    PolygonShape shape = new PolygonShape();
                    shape.setAsBox(layer.getTileWidth() / 2f / PPM, layer.getTileHeight() / 2f / PPM);

                    FixtureDef fixtureDef = new FixtureDef();
                    fixtureDef.shape = shape;
                    fixtureDef.friction = 0.5f;
                    fixtureDef.restitution = 0f;

                    Fixture fixture = body.createFixture(fixtureDef);
                    fixture.setUserData("collision"); // УБЕДИТЕСЬ ЧТО ЭТО ЕСТЬ!
                    shape.dispose();

                    collisionCount++;

                    // Отладка для первых нескольких тайлов
                    if (collisionCount < 10) {
                        Gdx.app.log("COLLISION", "Создана коллизия на позиции: " + bodyDef.position.x + ", " + bodyDef.position.y);
                    }
                }
            }
        }
        Gdx.app.log("INFO", "Создано коллизий: " + collisionCount);
    }

    private void createCollisionBody(MapObject object) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        if (object instanceof RectangleMapObject) {
            RectangleMapObject rectObject = (RectangleMapObject) object;
            Rectangle rect = rectObject.getRectangle();

            bodyDef.position.set(
                (rect.x + rect.width / 2) / PPM,
                (rect.y + rect.height / 2) / PPM
            );

            Body body = world.createBody(bodyDef);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(rect.width / 2 / PPM, rect.height / 2 / PPM);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.friction = 0.5f;
            fixtureDef.restitution = 0.1f;

            body.createFixture(fixtureDef);
            body.setUserData("collision");
            shape.dispose();

        } else if (object instanceof PolygonMapObject) {
            PolygonMapObject polygonObject = (PolygonMapObject) object;
            float[] vertices = polygonObject.getPolygon().getTransformedVertices();

            Vector2[] worldVertices = new Vector2[vertices.length / 2];
            for (int i = 0; i < worldVertices.length; i++) {
                worldVertices[i] = new Vector2(vertices[i*2]/PPM, vertices[i*2+1]/PPM);
            }

            bodyDef.position.set(0,0);
            Body body = world.createBody(bodyDef);

            PolygonShape shape = new PolygonShape();
            shape.set(worldVertices);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.friction = 0.5f;

            body.createFixture(fixtureDef);
            body.setUserData("collision");
            shape.dispose();
        }
    }

    private void createDebugGround() {
        BodyDef groundBodyDef = new BodyDef();
        groundBodyDef.position.set(mapWidth/2, 1f);

        Body groundBody = world.createBody(groundBodyDef);

        PolygonShape groundShape = new PolygonShape();
        groundShape.setAsBox(mapWidth/2, 0.5f);

        groundBody.createFixture(groundShape, 0.0f);
        groundShape.dispose();

        Gdx.app.log("WARNING", "Создан тестовый пол внизу карты!");
    }

    private void setupContactListener() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                Object userDataA = a.getUserData();
                Object userDataB = b.getUserData();

                // Проверяем любой контакт с ногами
                if (userDataA != null && userDataA.equals("foot") ||
                    userDataB != null && userDataB.equals("foot")) {
                    playerGrounded = true;
                    Gdx.app.log("GROUND", "*** НА ЗЕМЛЕ! *** playerGrounded=" + playerGrounded);
                }
            }

            @Override
            public void endContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                Object userDataA = a.getUserData();
                Object userDataB = b.getUserData();

                if (userDataA != null && userDataA.equals("foot") ||
                    userDataB != null && userDataB.equals("foot")) {
                    playerGrounded = false;
                    Gdx.app.log("GROUND", "*** В ВОЗДУХЕ! *** playerGrounded=" + playerGrounded);
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void createEnemiesFromTiled() {
        // Основной слой врагов (Toaster / Wheel и др.)
        MapLayer layer = map.getLayers().get("enemy");
        if (layer != null) {
            for (MapObject obj : layer.getObjects()) {
                Float objX = obj.getProperties().get("x", Float.class);
                Float objY = obj.getProperties().get("y", Float.class);
                if (objX == null || objY == null) continue;

                float x = objX / PPM;
                float y = objY / PPM;

                String enemyType = obj.getProperties().get("type", "zapper", String.class);
                enemies.add(new Enemy(world, new Vector2(x, y), enemyType));
            }
        } else {
            Gdx.app.log("ERROR", "Слой 'enemy' не найден!");
        }

        // Дополнительный слой enemy2 — можно ставить, например, колесных ботов
        MapLayer layer2 = map.getLayers().get("enemy2");
        if (layer2 != null) {
            for (MapObject obj : layer2.getObjects()) {
                Float objX = obj.getProperties().get("x", Float.class);
                Float objY = obj.getProperties().get("y", Float.class);
                if (objX == null || objY == null) continue;

                float x = objX / PPM;
                float y = objY / PPM;

                // по умолчанию считаем, что enemy2 — это колесные боты
                String enemyType = obj.getProperties().get("type", "wheel", String.class);
                enemies.add(new Enemy(world, new Vector2(x, y), enemyType));
            }
        }
    }

    // ===== УДАЛЯЕМ isPlayerGrounded() - больше не нужен =====
    // private boolean isPlayerGrounded() { ... } - удаляем

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f,0.1f,0.15f,1);

        world.step(1/60f,6,2);

        playerAttackTimer += delta;

        // ===== ИСПРАВЛЕНО: передаем playerGrounded в update =====
        player.update(delta, leftPressed, rightPressed, jumpPressed, attackPressed, playerGrounded);

        // ===== ДОБАВЬТЕ ЭТУ ОТЛАДКУ =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Gdx.app.log("INPUT", "Пробел нажат! jumpPressed=" + jumpPressed + " playerGrounded=" + playerGrounded);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            Gdx.app.log("DEBUG", "playerGrounded = " + playerGrounded);
            Gdx.app.log("DEBUG", "jumpPressed = " + jumpPressed);
        }

        // Обновление врагов и их поведения
        for (Enemy enemy : enemies) {
            if (enemy.alive) enemy.update(delta, player);
        }

        // Атака игрока по врагам (ближний бой)
        if (attackPressed && playerAttackTimer >= playerAttackCooldown && player.alive) {
            boolean hit = false;
            Vector2 playerPos = player.body.getPosition();
            for (Enemy enemy : enemies) {
                if (!enemy.alive) continue;
                if (playerPos.dst(enemy.body.getPosition()) <= playerAttackRange) {
                    enemy.takeDamage(25);
                    hit = true;
                }
            }
            if (hit) {
                playerAttackTimer = 0f;
            }
        }

        // Удаляем мертвых врагов и даем очки
        for (int i=enemies.size-1;i>=0;i--) {
            Enemy e = enemies.get(i);
            if (!e.alive) {
                enemies.removeIndex(i);
                score++;
            }
        }

        // Проверка смерти игрока при падении вниз и респавн
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
        for (Enemy enemy : enemies) enemy.render(batch);

        // Отрисовка интерфейса
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

    @Override public void resize(int width, int height) { viewport.update(width,height,true); }

    @Override
    public void dispose() {
        map.dispose(); renderer.dispose(); world.dispose(); debugRenderer.dispose(); batch.dispose();
        player.dispose();
        for (Enemy enemy : enemies) enemy.dispose();
        if (font != null) font.dispose();
    }

    private void respawnPlayer() {
        if (playerSpawn == null) return;
        player.body.setTransform(playerSpawn, 0);
        player.body.setLinearVelocity(0, 0);
        player.health = 100;
        player.alive = true;
        playerGrounded = false;
    }

    // ===== InputProcessor =====
    @Override public boolean keyDown(int keycode) {
        if (keycode==Input.Keys.A) leftPressed=true;
        if (keycode==Input.Keys.D) rightPressed=true;
        if (keycode==Input.Keys.SPACE) jumpPressed=true;
        if (keycode==Input.Keys.SHIFT_LEFT || keycode==Input.Keys.SHIFT_RIGHT) attackPressed=true;
        return true;
    }

    @Override public boolean keyUp(int keycode) {
        if (keycode==Input.Keys.A) leftPressed=false;
        if (keycode==Input.Keys.D) rightPressed=false;
        if (keycode==Input.Keys.SPACE) jumpPressed=false;
        if (keycode==Input.Keys.SHIFT_LEFT || keycode==Input.Keys.SHIFT_RIGHT) attackPressed=false;
        return true;
    }

    @Override public boolean keyTyped(char character){ return false; }
    @Override public boolean touchDown(int screenX,int screenY,int pointer,int button){ return false; }
    @Override public boolean touchUp(int screenX,int screenY,int pointer,int button){ return false; }
    @Override public boolean touchDragged(int screenX,int screenY,int pointer){ return false; }
    @Override public boolean mouseMoved(int screenX,int screenY){ return false; }
    @Override public boolean scrolled(float amountX,float amountY){ return false; }
    @Override public boolean touchCancelled(int screenX,int screenY,int pointer,int button){ return false; }
}
