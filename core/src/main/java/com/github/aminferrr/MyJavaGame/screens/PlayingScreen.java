package com.github.aminferrr.MyJavaGame.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.Preferences;

import com.github.aminferrr.MyJavaGame.Main;
import com.github.aminferrr.MyJavaGame.Database;
import com.github.aminferrr.MyJavaGame.elements.PlayerStats;
import com.github.aminferrr.MyJavaGame.Enemy;
import com.github.aminferrr.MyJavaGame.Player;
import com.github.aminferrr.MyJavaGame.screens.GameScreen;

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

    private boolean playerGrounded = false;

    private int score = 0;
    private Vector2 playerSpawn;
    private float playerAttackCooldown = 0.4f;
    private float playerAttackTimer = 0f;
    private float playerAttackRange = 2.5f;

    private Database database;
    private PlayerStats playerStats;
    private float statsUpdateTimer = 0f;
    private static final float STATS_UPDATE_INTERVAL = 1.0f;

    // ===== UI Stage =====
    private Stage uiStage;
    private Skin skin;
    private Window pauseWindow;
    private Window settingsWindow;
    private TextButton pauseButton;
    private boolean isPaused = false;

    // ===== Звуки и музыка =====
    private Music backgroundMusic;
    private Sound attackSound;
    private Sound jumpSound;
    private Sound enemyDeathSound;
    private Sound playerHurtSound;
    private Preferences soundPrefs;
    private float musicVolume = 0.5f;
    private float soundVolume = 0.5f;
    // ===== UI элементы для статистики =====
    private Table statsTable;
    private Label scoreLabel, hpLabel, expLabel;

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

        setupContactListener();

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(0.06f);

        // ===== UI Stage =====
        uiStage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // ===== Загружаем настройки звука =====
        soundPrefs = Gdx.app.getPreferences("MyGameSettings");
        musicVolume = soundPrefs.getFloat("musicVolume", 0.5f);
        soundVolume = soundPrefs.getFloat("soundVolume", 0.5f);

        // ===== Загружаем звуки и музыку =====
        loadSounds();

        int tilesW = map.getProperties().get("width", Integer.class);
        int tilesH = map.getProperties().get("height", Integer.class);
        int tileW = map.getProperties().get("tilewidth", Integer.class);
        int tileH = map.getProperties().get("tileheight", Integer.class);

        mapWidth = (tilesW * tileW) / PPM;
        mapHeight = (tilesH * tileH) / PPM;

        createCollisionsFromTileLayer();

        player = new Player(world);
        playerSpawn = player.body.getPosition().cpy();

        database = new Database();
        database.insertInitialPlayer("Hero");
        playerStats = new PlayerStats(database);
        player.health = playerStats.getHp();

        // Передаем звуки игроку
        player.setSounds(attackSound, jumpSound, playerHurtSound);

        enemies = new Array<>();
        createEnemiesFromTiled();

        // ===== Создаем UI элементы =====
        createUI();
        // ===== Создаем UI элементы =====
        createUI();


        createStatsTable();  // ДОБАВЬТЕ ЭТУ СТРОКУ

        Gdx.input.setInputProcessor(new InputMultiplexer(uiStage, this));

        Gdx.input.setInputProcessor(new InputMultiplexer(uiStage, this));

        updateCamera();

        Gdx.app.log("INFO", "Игра загружена. Размер карты: " + mapWidth + " x " + mapHeight);
    }

    private void loadSounds() {
        try {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/soundtrack.mp3"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(musicVolume);
            backgroundMusic.play();

            attackSound = Gdx.audio.newSound(Gdx.files.internal("audio/sounds/attack.mp3"));
            jumpSound = Gdx.audio.newSound(Gdx.files.internal("audio/sounds/jump.mp3"));
            enemyDeathSound = Gdx.audio.newSound(Gdx.files.internal("audio/sounds/enemy_death.mp3"));
            playerHurtSound = Gdx.audio.newSound(Gdx.files.internal("audio/sounds/player_hurt.mp3"));

            Gdx.app.log("SOUND", "Звуки загружены");
        } catch (Exception e) {
            Gdx.app.error("SOUND", "Ошибка загрузки звуков", e);
        }
    }

    private void createUI() {
        // Кнопка паузы
        pauseButton = new TextButton("Pause", skin);
        pauseButton.setPosition(Gdx.graphics.getWidth() - 100, Gdx.graphics.getHeight() - 50);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isPaused = !isPaused;
                if (isPaused) {
                    pauseWindow.setVisible(true);
                } else {
                    pauseWindow.setVisible(false);
                    settingsWindow.setVisible(false);
                }
            }
        });
        uiStage.addActor(pauseButton);

        // Окно паузы
        createPauseWindow();

        // Окно настроек
        createSettingsWindow();
    }

    private void createStatsTable() {
        statsTable = new Table();
        statsTable.setFillParent(true);
        statsTable.top().left();
        statsTable.pad(10);

        scoreLabel = new Label("Score: 0", skin);
        hpLabel = new Label("HP: 100/100", skin);
        expLabel = new Label("EXP: 0", skin);

        statsTable.add(scoreLabel).left().padBottom(5);
        statsTable.row();
        statsTable.add(hpLabel).left().padBottom(5);
        statsTable.row();
        statsTable.add(expLabel).left();

        uiStage.addActor(statsTable);
    }



    private void createPauseWindow() {
        pauseWindow = new Window("Paused", skin);
        pauseWindow.setSize(300, 250);
        pauseWindow.setPosition(
            Gdx.graphics.getWidth()/2f - 150,
            Gdx.graphics.getHeight()/2f - 125
        );
        pauseWindow.setVisible(false);

        Table table = new Table();
        table.defaults().pad(10);

        TextButton resumeButton = new TextButton("Resume", skin);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isPaused = false;
                pauseWindow.setVisible(false);
                settingsWindow.setVisible(false);
            }
        });

        TextButton settingsButton = new TextButton("Settings", skin);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsWindow.setVisible(true);
            }
        });

        TextButton exitButton = new TextButton("Exit to Menu", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (backgroundMusic != null) backgroundMusic.stop();
                game.setScreen(new FirstScreen(game));
            }
        });

        table.add(resumeButton).width(200).height(40);
        table.row();
        table.add(settingsButton).width(200).height(40);
        table.row();
        table.add(exitButton).width(200).height(40);

        pauseWindow.add(table);
        uiStage.addActor(pauseWindow);
    }

    private void createSettingsWindow() {
        settingsWindow = new Window("Settings", skin);
        settingsWindow.setSize(350, 300);
        settingsWindow.setPosition(
            Gdx.graphics.getWidth()/2f - 175,
            Gdx.graphics.getHeight()/2f - 150
        );
        settingsWindow.setVisible(false);

        Table table = new Table();
        table.defaults().pad(10);

        // Музыка
        Label musicLabel = new Label("Music Volume", skin);
        Slider musicSlider = new Slider(0f, 1f, 0.1f, false, skin);
        musicSlider.setValue(musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                musicVolume = musicSlider.getValue();
                if (backgroundMusic != null) {
                    backgroundMusic.setVolume(musicVolume);
                }
                soundPrefs.putFloat("musicVolume", musicVolume);
                soundPrefs.flush();
            }
        });

        // Звуки
        Label soundLabel = new Label("Sound Volume", skin);
        Slider soundSlider = new Slider(0f, 1f, 0.1f, false, skin);
        soundSlider.setValue(soundVolume);
        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                soundVolume = soundSlider.getValue();
                soundPrefs.putFloat("soundVolume", soundVolume);
                soundPrefs.flush();
            }
        });

        // Чекбокс для музыки
        CheckBox musicCheck = new CheckBox(" Enable Music", skin);
        musicCheck.setChecked(true);
        musicCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (backgroundMusic != null) {
                    if (musicCheck.isChecked()) {
                        backgroundMusic.setVolume(musicVolume);
                    } else {
                        backgroundMusic.setVolume(0);
                    }
                }
            }
        });

        // Чекбокс для отладки
        CheckBox debugCheck = new CheckBox(" Debug Mode", skin);
        debugCheck.setChecked(debugMode);
        debugCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                debugMode = debugCheck.isChecked();
            }
        });

        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsWindow.setVisible(false);
            }
        });

        table.add(musicLabel).left();
        table.add(musicSlider).width(200);
        table.row();
        table.add(soundLabel).left();
        table.add(soundSlider).width(200);
        table.row();
        table.add(musicCheck).colspan(2);
        table.row();
        table.add(debugCheck).colspan(2);
        table.row();
        table.add(backButton).colspan(2).padTop(20);

        settingsWindow.add(table);
        uiStage.addActor(settingsWindow);
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
                    fixture.setUserData("collision");
                    shape.dispose();

                    collisionCount++;
                }
            }
        }
        Gdx.app.log("INFO", "Создано коллизий: " + collisionCount);
    }

    private void setupContactListener() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                Object userDataA = a.getUserData();
                Object userDataB = b.getUserData();

                if (userDataA != null && userDataA.equals("foot") ||
                    userDataB != null && userDataB.equals("foot")) {
                    playerGrounded = true;
                }

                if (("player".equals(userDataA) && "enemy".equals(userDataB)) ||
                    ("enemy".equals(userDataA) && "player".equals(userDataB))) {
                    if (player.alive) {
                        player.takeDamage(10);
                        Vector2 pushDir = player.body.getPosition().sub(
                            "player".equals(userDataA) ?
                                b.getBody().getPosition() :
                                a.getBody().getPosition()
                        ).nor();
                        player.body.applyLinearImpulse(pushDir.scl(5f), player.body.getWorldCenter(), true);
                    }
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
                }
            }

            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private void createEnemiesFromTiled() {
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
        }

        MapLayer layer2 = map.getLayers().get("enemy2");
        if (layer2 != null) {
            for (MapObject obj : layer2.getObjects()) {
                Float objX = obj.getProperties().get("x", Float.class);
                Float objY = obj.getProperties().get("y", Float.class);
                if (objX == null || objY == null) continue;

                float x = objX / PPM;
                float y = objY / PPM;

                String enemyType = obj.getProperties().get("type", "wheel", String.class);
                enemies.add(new Enemy(world, new Vector2(x, y), enemyType));
            }
        }
    }

    @Override
    public void render(float delta) {
        if (isPaused) {
            ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1);
            renderer.render();
            uiStage.act(delta);
            uiStage.draw();
            return;
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1);

        world.step(1/60f, 6, 2);

        playerAttackTimer += delta;

        statsUpdateTimer += delta;
        if (statsUpdateTimer >= STATS_UPDATE_INTERVAL) {
            playerStats = new PlayerStats(database);
            statsUpdateTimer = 0f;
        }

        player.update(delta, leftPressed, rightPressed, jumpPressed, attackPressed, playerGrounded);

        for (Enemy enemy : enemies) {
            if (enemy.alive) enemy.update(delta, player);
        }

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

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.alive) {
                enemies.removeIndex(i);
                score++;

                if (enemyDeathSound != null) {
                    enemyDeathSound.play(soundVolume);
                }

                try (java.sql.Statement stmt = database.getConnection().createStatement()) {
                    stmt.executeUpdate("UPDATE player SET experience = experience + 10 WHERE id=1;");
                    playerStats = new PlayerStats(database);
                } catch (java.sql.SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }

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
        batch.end();  // Закрываем batch

// Обновляем статистику для таблицы
        scoreLabel.setText("Score: " + score);
        hpLabel.setText("HP: " + player.health + "/" + playerStats.getHp());
        expLabel.setText("EXP: " + playerStats.getExperience());

        uiStage.act(delta);
        uiStage.draw();

        if (debugMode) debugRenderer.render(world, camera.combined);

// ===== ВАЖНО: Проверка на завершение уровня =====
        if (enemies.size == 0) {
            Gdx.app.log("LEVEL", "All enemies defeated! Returning to GameScreen");
            game.setScreen(new GameScreen(game));
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

        playerStats = new PlayerStats(database);
        player.health = playerStats.getHp();
        player.alive = true;
        playerGrounded = false;

        if (playerHurtSound != null) {
            playerHurtSound.play(soundVolume);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiStage.getViewport().update(width, height, true);

        pauseButton.setPosition(width - 100, height - 50);
        if (pauseWindow != null && pauseWindow.isVisible()) {
            pauseWindow.setPosition(width/2f - 150, height/2f - 125);
        }
        if (settingsWindow != null && settingsWindow.isVisible()) {
            settingsWindow.setPosition(width/2f - 175, height/2f - 150);
        }
    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
        world.dispose();
        debugRenderer.dispose();
        batch.dispose();
        player.dispose();
        for (Enemy enemy : enemies) enemy.dispose();
        if (font != null) font.dispose();
        if (database != null) database.close();

        if (backgroundMusic != null) backgroundMusic.dispose();
        if (attackSound != null) attackSound.dispose();
        if (jumpSound != null) jumpSound.dispose();
        if (enemyDeathSound != null) enemyDeathSound.dispose();
        if (playerHurtSound != null) playerHurtSound.dispose();

        uiStage.dispose();
        skin.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.A) leftPressed = true;
        if (keycode == Input.Keys.D) rightPressed = true;
        if (keycode == Input.Keys.SPACE) jumpPressed = true;
        if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) attackPressed = true;

        if (keycode == Input.Keys.NUM_1) {
            if (playerStats.upgradeHp()) {
                player.health = playerStats.getHp();
            }
        }
        if (keycode == Input.Keys.NUM_2) {
            if (playerStats.upgradeStrength()) {
            }
        }
        if (keycode == Input.Keys.NUM_3) {
            if (playerStats.upgradeSpeed()) {
            }
        }
        if (keycode == Input.Keys.NUM_4) {
            if (playerStats.upgradeDefense()) {
            }
        }

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
