package com.github.aminferrr.MyJavaGame.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.Preferences;

import com.github.aminferrr.MyJavaGame.Main;
import com.github.aminferrr.MyJavaGame.Database;
import com.github.aminferrr.MyJavaGame.elements.Player;
import com.github.aminferrr.MyJavaGame.elements.PlayerStats;
import com.github.aminferrr.MyJavaGame.screens.PlayingScreen;
import com.github.aminferrr.MyJavaGame.maps.GameMapScreen1;
import com.github.aminferrr.MyJavaGame.plot.NPCHandler;
import com.github.aminferrr.MyJavaGame.screens.Level2Screen;

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
    private Stage uiStage;

    // ===== UI элементы для статистики =====
    private Table statsTable;
    private Label hpLabel, strengthLabel, speedLabel, defenseLabel, expLabel;
    private Label hpCostLabel, strengthCostLabel, speedCostLabel, defenseCostLabel;
    private TextButton hpButton, strengthButton, speedButton, defenseButton;

    // Кнопки в правом верхнем углу
    private TextButton levelsBtn, settingsBtn;
    private Window levelsWindow;
    private Window settingsWindow;

    private NPCHandler npcHandler;

    // ===== Поле ввода ответа =====
    private final TextField answerField;
    private final TextButton submitButton;

    // ===== Звуки и музыка =====
    private Music backgroundMusic;
    private Sound attackSound;
    private Sound jumpSound;
    private Sound enemyDeathSound;
    private Sound playerHurtSound;
    private Preferences soundPrefs;
    private float musicVolume = 0.5f;
    private float soundVolume = 0.5f;

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

        // Создаем два Stage: один для мира, один для UI
        stage = new Stage(viewport);
        uiStage = new Stage(new ScreenViewport()); // Используем ScreenViewport для UI

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // ===== Загружаем настройки звука =====
        soundPrefs = Gdx.app.getPreferences("MyGameSettings");
        musicVolume = soundPrefs.getFloat("musicVolume", 0.5f);
        soundVolume = soundPrefs.getFloat("soundVolume", 0.5f);

        // ===== Загружаем звуки и музыку =====
        loadSounds();

        // ===== Создаем таблицу со статистикой =====
        createStatsTable();

        // ===== Кнопки Levels и Settings в правом верхнем углу =====
        createTopButtons();
        createLevelsWindow();
        createSettingsWindow();


        npcHandler = new NPCHandler(stage, skin, buildsLayer, player);

        // ===== Поле ввода ответа =====
        answerField = new TextField("", skin);
        answerField.setSize(200, 30);
        answerField.setVisible(false);
        uiStage.addActor(answerField);

        submitButton = new TextButton("Submit", skin);
        submitButton.setSize(80, 30);
        submitButton.setVisible(false);
        uiStage.addActor(submitButton);

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
        // ===== Input =====
        InputMultiplexer multiplexer = new InputMultiplexer();

// ВАЖНО: stage должен быть ПЕРВЫМ, чтобы кнопка Talk получала события
        multiplexer.addProcessor(stage);      // Сначала stage (кнопка Talk, диалоги)
        multiplexer.addProcessor(uiStage);    // Потом uiStage (статистика, кнопки Levels/Settings)
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Этот обработчик срабатывает только если ни stage, ни uiStage не обработали событие
                if (npcHandler.isDialogueActive() && !npcHandler.isWaitingForAnswer()) {
                    npcHandler.nextDialogueLine();
                    return true;
                }
                return false;
            }
        });

        Gdx.input.setInputProcessor(multiplexer);
    }

    private void createStatsTable() {
        statsTable = new Table();
        statsTable.setFillParent(true);
        statsTable.top().left(); // Прижимаем к левому верхнему углу
        statsTable.pad(10);

        // Создаем метки для значений
        hpLabel = new Label("HP: " + player.getHealth() + "/" + playerStats.getHp(), skin);
        strengthLabel = new Label("STR: " + playerStats.getStrength(), skin);
        speedLabel = new Label("SPD: " + playerStats.getSpeed(), skin);
        defenseLabel = new Label("DEF: " + playerStats.getDefense(), skin);
        expLabel = new Label("EXP: " + playerStats.getExperience(), skin);

        // Создаем метки для стоимости
        hpCostLabel = new Label("Cost: " + playerStats.getHpCost(), skin);
        strengthCostLabel = new Label("Cost: " + playerStats.getStrengthCost(), skin);
        speedCostLabel = new Label("Cost: " + playerStats.getSpeedCost(), skin);
        defenseCostLabel = new Label("Cost: " + playerStats.getDefenseCost(), skin);

        // Создаем кнопки
        hpButton = new TextButton("UP", skin);
        strengthButton = new TextButton("UP", skin);
        speedButton = new TextButton("UP", skin);
        defenseButton = new TextButton("UP", skin);

        // Добавляем слушатели
        hpButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (playerStats.upgradeHp()) {
                    player.setMaxHealth(playerStats.getHp());
                    updateStatsLabels();
                }
            }
        });

        strengthButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (playerStats.upgradeStrength()) {
                    updateStatsLabels();
                }
            }
        });

        speedButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (playerStats.upgradeSpeed()) {
                    player.setSpeed(playerStats.getSpeed());
                    updateStatsLabels();
                }
            }
        });

        defenseButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (playerStats.upgradeDefense()) {
                    updateStatsLabels();
                }
            }
        });

        // Добавляем все в таблицу
        statsTable.add(hpLabel).left().padRight(20);
        statsTable.add(hpCostLabel).padRight(10);
        statsTable.add(hpButton).width(40).height(25).padRight(20);
        statsTable.row().padTop(5);

        statsTable.add(strengthLabel).left().padRight(20);
        statsTable.add(strengthCostLabel).padRight(10);
        statsTable.add(strengthButton).width(40).height(25).padRight(20);
        statsTable.row().padTop(5);

        statsTable.add(speedLabel).left().padRight(20);
        statsTable.add(speedCostLabel).padRight(10);
        statsTable.add(speedButton).width(40).height(25).padRight(20);
        statsTable.row().padTop(5);

        statsTable.add(defenseLabel).left().padRight(20);
        statsTable.add(defenseCostLabel).padRight(10);
        statsTable.add(defenseButton).width(40).height(25).padRight(20);
        statsTable.row().padTop(10);

        statsTable.add(expLabel).left().colspan(3);

        uiStage.addActor(statsTable);
    }

    private void updateStatsLabels() {
        hpLabel.setText("HP: " + player.getHealth() + "/" + playerStats.getHp());
        strengthLabel.setText("STR: " + playerStats.getStrength());
        speedLabel.setText("SPD: " + playerStats.getSpeed());
        defenseLabel.setText("DEF: " + playerStats.getDefense());
        expLabel.setText("EXP: " + playerStats.getExperience());

        hpCostLabel.setText("Cost: " + playerStats.getHpCost());
        strengthCostLabel.setText("Cost: " + playerStats.getStrengthCost());
        speedCostLabel.setText("Cost: " + playerStats.getSpeedCost());
        defenseCostLabel.setText("Cost: " + playerStats.getDefenseCost());
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

            player.setSounds(attackSound, jumpSound, playerHurtSound);

            Gdx.app.log("SOUND", "Звуки загружены");
        } catch (Exception e) {
            Gdx.app.error("SOUND", "Ошибка загрузки звуков", e);
        }
    }

    private void createTopButtons() {
        levelsBtn = new TextButton("Levels", skin);
        settingsBtn = new TextButton("Settings", skin);

        // Позиционируем на UI Stage
        levelsBtn.setPosition(Gdx.graphics.getWidth() - 120, Gdx.graphics.getHeight() - 50);
        settingsBtn.setPosition(Gdx.graphics.getWidth() - 220, Gdx.graphics.getHeight() - 50);

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

        uiStage.addActor(levelsBtn);
        uiStage.addActor(settingsBtn);
    }

    private void createLevelsWindow() {
        // Окно оставляем большим - 800x500
        levelsWindow = new Window("Select Level", skin);
        levelsWindow.setSize(800, 500);
        levelsWindow.setPosition(
            Gdx.graphics.getWidth()/2f - 400,
            Gdx.graphics.getHeight()/2f - 250
        );

        Table table = new Table();
        table.defaults().pad(15); // Отступы между кнопками

        // Создаем кнопки для 10 уровней в сетке 5x2
        for (int i = 1; i <= 10; i++) {
            final int level = i;
            String buttonText = "Level " + i;
            if (i == 1) buttonText += "\n(First)";
            else if (i == 2) buttonText += "\n(Second)";
            else buttonText += "\n(Coming Soon)";

            TextButton levelBtn = new TextButton(buttonText, skin);

            if (i > 2) {
                levelBtn.setDisabled(true);
            }

            levelBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (levelBtn.isDisabled()) {
                        Gdx.app.log("LEVEL", "Level " + level + " is locked");
                        return;
                    }

                    levelsWindow.setVisible(false);
                    Gdx.app.log("LEVEL", "Loading Level " + level);

                    if (backgroundMusic != null) {
                        backgroundMusic.stop();
                    }

                    switch (level) {
                        case 1:
                            game.setScreen(new PlayingScreen(game));
                            break;
                        case 2:
                            game.setScreen(new Level2Screen(game));
                            break;
                        default:
                            Gdx.app.log("LEVEL", "Level " + level + " not implemented");
                            break;
                    }
                }
            });

            // УМЕНЬШАЕМ размер кнопок (было 180x100)
            table.add(levelBtn).width(120).height(70); // Меньше размер
            if (i % 5 == 0) table.row();
        }

        TextButton close = new TextButton("Close", skin);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                levelsWindow.setVisible(false);
            }
        });
        table.row();
        // Кнопка Close тоже поменьше
        table.add(close).colspan(5).padTop(20).width(150).height(40);

        levelsWindow.add(table);
        levelsWindow.setVisible(false);
        uiStage.addActor(levelsWindow);
    }

    private void createSettingsWindow() {
        settingsWindow = new Window("Settings", skin);
        settingsWindow.setSize(400, 300);
        settingsWindow.setPosition(
            Gdx.graphics.getWidth()/2f - 200,
            Gdx.graphics.getHeight()/2f - 150
        );

        Table table = new Table();
        table.defaults().pad(10);

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

        TextButton close = new TextButton("Close", skin);
        close.addListener(new ClickListener() {
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
        table.add(close).colspan(2).padTop(20);

        settingsWindow.add(table);
        settingsWindow.setVisible(false);
        uiStage.addActor(settingsWindow);
    }

    @Override
    public void render(float delta) {
        // Обновляем статистику каждый кадр
        updateStatsLabels();

        if (!npcHandler.isDialogueActive()) {
            player.update(delta);

            if (player.isDead()) {
                player.respawn();
                if (playerHurtSound != null) {
                    playerHurtSound.play(soundVolume);
                }
            }
        }

        npcHandler.update(delta, player);

        if (npcHandler.isDialogueActive()) {
            npcHandler.updateDialoguePosition(camera, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        if (npcHandler.isDialogueActive() && npcHandler.isWaitingForAnswer()) {
            float centerX = Gdx.graphics.getWidth()/2f - answerField.getWidth()/2f;
            float centerY = Gdx.graphics.getHeight()/2f;
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

        batch.end();

        stage.act(delta);
        stage.draw();
        // Рисуем UI stage (кнопки статистики, уровни, настройки)
        uiStage.act(delta);
        uiStage.draw();

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
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true);

        // Обновляем позиции кнопок
        levelsBtn.setPosition(width - 120, height - 50);
        settingsBtn.setPosition(width - 220, height - 50);

        // Обновляем позиции окон с новыми размерами
        if (levelsWindow != null && levelsWindow.isVisible()) {
            levelsWindow.setPosition(width/2f - 400, height/2f - 250);  // 800x500
        }
        if (settingsWindow != null && settingsWindow.isVisible()) {
            settingsWindow.setPosition(width/2f - 200, height/2f - 150); // 400x300
        }
    }

    @Override
    public void show() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.setVolume(musicVolume);
            backgroundMusic.play();
        }
    }

    @Override
    public void pause() {
        if (backgroundMusic != null) backgroundMusic.pause();
    }

    @Override
    public void resume() {
        if (backgroundMusic != null) backgroundMusic.play();
    }

    @Override
    public void hide() {
        if (backgroundMusic != null) backgroundMusic.pause();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        player.dispose();
        npcHandler.dispose();
        stage.dispose();
        uiStage.dispose();
        skin.dispose();
        db.close();
        mapRenderer.dispose();
        gameMap.dispose();

        if (backgroundMusic != null) backgroundMusic.dispose();
        if (attackSound != null) attackSound.dispose();
        if (jumpSound != null) jumpSound.dispose();
        if (enemyDeathSound != null) enemyDeathSound.dispose();
        if (playerHurtSound != null) playerHurtSound.dispose();
    }
}
