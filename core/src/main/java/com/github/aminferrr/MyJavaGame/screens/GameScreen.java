package com.github.aminferrr.MyJavaGame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.Actor;

import com.github.aminferrr.MyJavaGame.Main;
import com.github.aminferrr.MyJavaGame.Database;
import com.github.aminferrr.MyJavaGame.elements.Player;
import com.github.aminferrr.MyJavaGame.elements.PlayerStats;
import com.github.aminferrr.MyJavaGame.maps.GameMapScreen1;
import com.github.aminferrr.MyJavaGame.plot.NPCHandler;
import com.github.aminferrr.MyJavaGame.Level2Screen;
import com.github.aminferrr.MyJavaGame.PlayingScreen;

public class GameScreen implements Screen {

    private final Main game;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer buildsLayer;

    private final SpriteBatch batch;
    private final BitmapFont font;

    private final Database db;
    private final PlayerStats playerStats;
    private final Player player;

    private final GameMapScreen1 gameMap;

    private final Stage stage;
    private final Skin skin;

    private final TextButton hpButton, strengthButton, speedButton, defenseButton;

    // Кнопки в правом верхнем углу
    private TextButton levelsBtn, settingsBtn;
    private Window levelsWindow;
    private Window settingsWindow;

    private NPCHandler npcHandler;

    // ===== Поле ввода ответа =====
    private final TextField answerField;
    private final TextButton submitButton;

    private static final float TILE = 16f;

    public GameScreen(Main game) {
        this.game = game;

        batch = new SpriteBatch();
        font = new BitmapFont();

        db = new Database();
        playerStats = new PlayerStats(db);

        gameMap = new GameMapScreen1();
        mapRenderer = new OrthogonalTiledMapRenderer(gameMap.getMap(), 1f);
        buildsLayer = gameMap.getBuildsLayer();

        player = new Player(playerStats);
        player.setCollisionLayer(buildsLayer);

        camera = new OrthographicCamera();
        viewport = new FitViewport(640, 360, camera);
        viewport.apply(true);

        stage = new Stage(viewport);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // ===== Кнопки прокачки =====
        hpButton = new TextButton("HP +", skin);
        strengthButton = new TextButton("STR +", skin);
        speedButton = new TextButton("SPD +", skin);
        defenseButton = new TextButton("DEF +", skin);

        hpButton.setBounds(10, 100, 50, 20);
        strengthButton.setBounds(10, 80, 50, 20);
        speedButton.setBounds(10, 60, 50, 20);
        defenseButton.setBounds(10, 40, 50, 20);

        hpButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                playerStats.upgradeHp();
            }
        });
        strengthButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                playerStats.upgradeStrength();
            }
        });
        speedButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                playerStats.upgradeSpeed();
            }
        });
        defenseButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                playerStats.upgradeDefense();
            }
        });

        stage.addActor(hpButton);
        stage.addActor(strengthButton);
        stage.addActor(speedButton);
        stage.addActor(defenseButton);

        // ===== Кнопки Levels и Settings в правом верхнем углу =====
        createTopButtons();
        createLevelsWindow();
        createSettingsWindow();

        // ===== NPC Handler =====
        npcHandler = new NPCHandler(stage, skin, buildsLayer, player);

        // ===== Поле ввода ответа =====
        answerField = new TextField("", skin);
        answerField.setSize(200, 30);
        answerField.setPosition(200, 50);
        answerField.setVisible(false);
        stage.addActor(answerField);

        submitButton = new TextButton("Submit", skin);
        submitButton.setSize(80, 30);
        submitButton.setPosition(410, 50);
        submitButton.setVisible(false);
        stage.addActor(submitButton);

        submitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!answerField.getText().isEmpty() && npcHandler.isWaitingForAnswer()) {
                    npcHandler.submitPlayerAnswer(answerField.getText());
                    answerField.setText("");
                    answerField.setVisible(false);
                    submitButton.setVisible(false);
                }
            }
        });

        // ===== Input =====
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (npcHandler.isDialogueActive() && !npcHandler.isWaitingForAnswer()) {
                    npcHandler.nextDialogueLine();
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void createTopButtons() {
        levelsBtn = new TextButton("Levels", skin);
        settingsBtn = new TextButton("Settings", skin);

        // Позиционируем относительно viewport (не экрана!)
        levelsBtn.setPosition(viewport.getWorldWidth() - 120, viewport.getWorldHeight() - 40);
        settingsBtn.setPosition(viewport.getWorldWidth() - 220, viewport.getWorldHeight() - 40);

        levelsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                levelsWindow.setVisible(true);
            }
        });

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsWindow.setVisible(true);
            }
        });

        stage.addActor(levelsBtn);
        stage.addActor(settingsBtn);
    }

    private void createLevelsWindow() {
        levelsWindow = new Window("Select Level", skin);
        levelsWindow.setSize(400, 300);
        levelsWindow.setPosition(
            viewport.getWorldWidth()/2f - 200,
            viewport.getWorldHeight()/2f - 150
        );

        Table table = new Table();
        table.defaults().pad(10);

        int totalLevels = 10;
        for (int i = 1; i <= totalLevels; i++) {
            final int level = i;
            TextButton levelBtn = new TextButton("Level " + i, skin);
            levelBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // Закрываем окно уровней
                    levelsWindow.setVisible(false);

                    // Логируем выбор уровня
                    Gdx.app.log("LEVEL", "Loading Level " + level);

                    // Переход на соответствующий уровень
                    switch (level) {
                        case 1:
                            game.setScreen(new PlayingScreen(game));
                            break;
                        case 2:
                            game.setScreen(new Level2Screen(game));
                            break;
                        case 3:
                            // TODO: Создать Level3Screen
                            Gdx.app.log("LEVEL", "Level 3 not implemented yet");
                            break;
                        case 4:
                            // TODO: Создать Level4Screen
                            Gdx.app.log("LEVEL", "Level 4 not implemented yet");
                            break;
                        case 5:
                            // TODO: Создать Level5Screen
                            Gdx.app.log("LEVEL", "Level 5 not implemented yet");
                            break;
                        case 6:
                            // TODO: Создать Level6Screen
                            Gdx.app.log("LEVEL", "Level 6 not implemented yet");
                            break;
                        case 7:
                            // TODO: Создать Level7Screen
                            Gdx.app.log("LEVEL", "Level 7 not implemented yet");
                            break;
                        case 8:
                            // TODO: Создать Level8Screen
                            Gdx.app.log("LEVEL", "Level 8 not implemented yet");
                            break;
                        case 9:
                            // TODO: Создать Level9Screen
                            Gdx.app.log("LEVEL", "Level 9 not implemented yet");
                            break;
                        case 10:
                            // TODO: Создать Level10Screen
                            Gdx.app.log("LEVEL", "Level 10 not implemented yet");
                            break;
                        default:
                            Gdx.app.log("LEVEL", "Unknown level: " + level);
                            break;
                    }
                }
            });
            table.add(levelBtn).width(100).height(40);
            if (i % 4 == 0) table.row();
        }

        TextButton close = new TextButton("Close", skin);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                levelsWindow.setVisible(false);
            }
        });
        table.row();
        table.add(close).colspan(4).padTop(20);

        levelsWindow.add(table);
        levelsWindow.setVisible(false);
        stage.addActor(levelsWindow);
    }

    private void createSettingsWindow() {
        settingsWindow = new Window("Settings", skin);
        settingsWindow.setSize(350, 200);
        settingsWindow.setPosition(
            viewport.getWorldWidth()/2f - 175,
            viewport.getWorldHeight()/2f - 100
        );

        Table table = new Table();
        table.defaults().pad(10);

        CheckBox soundCheck = new CheckBox(" Sound", skin);
        soundCheck.setChecked(true);
        table.add(soundCheck);
        table.row();

        TextButton close = new TextButton("Close", skin);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsWindow.setVisible(false);
            }
        });
        table.add(close);

        settingsWindow.add(table);
        settingsWindow.setVisible(false);
        stage.addActor(settingsWindow);
    }

    @Override
    public void render(float delta) {

        if (!npcHandler.isDialogueActive()) {
            player.update(delta);
        }

        npcHandler.update(delta, player);

        if (npcHandler.isDialogueActive()) {
            npcHandler.updateDialoguePosition(camera, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        if (npcHandler.isDialogueActive() && npcHandler.isWaitingForAnswer()) {
            float centerX = camera.position.x - answerField.getWidth() / 2f;
            float centerY = camera.position.y - answerField.getHeight() / 2f;
            answerField.setPosition(centerX, centerY);
            answerField.setVisible(true);
            submitButton.setPosition(centerX + answerField.getWidth() + 10, centerY);
            submitButton.setVisible(true);
        } else {
            answerField.setVisible(false);
            submitButton.setVisible(false);
        }

        updateCameraClamped();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        npcHandler.render(batch, player);
        font.draw(batch, "HP: " + playerStats.getHp(), camera.position.x - 300, camera.position.y + 150);
        font.draw(batch, "STR: " + playerStats.getStrength(), camera.position.x - 300, camera.position.y + 130);
        font.draw(batch, "DEF: " + playerStats.getDefense(), camera.position.x - 300, camera.position.y + 110);
        font.draw(batch, "SPD: " + playerStats.getSpeed(), camera.position.x - 300, camera.position.y + 90);
        font.draw(batch, "EXP: " + playerStats.getExperience(), camera.position.x - 300, camera.position.y + 70);
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void updateCameraClamped() {
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;

        float worldW = buildsLayer.getWidth() * TILE;
        float worldH = buildsLayer.getHeight() * TILE;

        float targetX = player.getX() + player.getWidth() / 2f;
        float targetY = player.getY() + player.getHeight() / 2f;

        float camX = MathUtils.clamp(targetX, halfW, worldW - halfW);
        float camY = MathUtils.clamp(targetY, halfH, worldH - halfH);

        camera.position.set(camX, camY, 0);
        camera.update();

        // Обновляем позиции кнопок при движении камеры
        levelsBtn.setPosition(camera.position.x + viewport.getWorldWidth()/2f - 120,
            camera.position.y + viewport.getWorldHeight()/2f - 40);
        settingsBtn.setPosition(camera.position.x + viewport.getWorldWidth()/2f - 220,
            camera.position.y + viewport.getWorldHeight()/2f - 40);
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        player.dispose();
        npcHandler.dispose();
        stage.dispose();
        skin.dispose();
        db.close();
        mapRenderer.dispose();
        gameMap.dispose();
    }
}
