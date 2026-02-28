package com.github.aminferrr.MyJavaGame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.aminferrr.MyJavaGame.Main;
import com.github.aminferrr.MyJavaGame.Database;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

import java.io.File;

public class FirstScreen implements Screen {

    private Main game;
    private Stage stage;
    private Database db;
    private Music bgMusic;
    private SpriteBatch batch;

    // ВИДЕОПЛЕЕР
    private VideoPlayer videoPlayer;
    private Texture videoFrame;

    // Окна
    private Window levelsWindow;
    private Window settingsWindow;
    private boolean soundEnabled = true;

    // Кнопки меню
    private TextButton continueButton;
    private TextButton newGameButton;
    private TextButton settingsButton;
    private TextButton exitButton;

    public FirstScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        db = new Database();
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Загружаем музыку
        bgMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/soundtrack.mp3"));
        bgMusic.setLooping(true);
        bgMusic.setVolume(0.5f);
        bgMusic.play();

        // Загрузка видео
        try {
            videoPlayer = VideoPlayerCreator.createVideoPlayer();
            FileHandle videoFile = Gdx.files.internal("back.webm");
            videoPlayer.play(videoFile);
            videoPlayer.setVolume(0f);
            videoPlayer.setLooping(true);
            Gdx.app.log("VIDEO", "Видео успешно загружено");
        } catch (Exception e) {
            Gdx.app.error("VIDEO", "Ошибка загрузки видео", e);
            videoPlayer = null;
        }

        // Создаем окна
        createLevelsWindow(skin);
        createSettingsWindow(skin);

        // Создаем центральное меню
        createCenterMenu(skin);
    }

    private void createCenterMenu(Skin skin) {
        boolean playerExists = db.checkPlayerExists();

        // Создаем кнопки
        continueButton = new TextButton(playerExists ? "Continue" : "", skin);
        newGameButton = new TextButton("New Game", skin);
        settingsButton = new TextButton("Settings", skin);
        exitButton = new TextButton("Exit", skin);

        // Делаем Continue невидимой, если нет сохранения
        if (!playerExists) {
            continueButton.setVisible(false);
        }

        // ===== Continue Button =====
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!db.checkPlayerExists()) {
                    db.insertInitialPlayer("Hero");
                }
                if (videoPlayer != null) videoPlayer.stop();
                bgMusic.stop();
                game.setScreen(new GameScreen(game));
            }
        });

        // ===== New Game Button (сбрасывает прогресс и создает новую игру) =====
        newGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Удаляем старые данные
                db.resetPlayerTable();
                deleteProgressFile();

                // Создаем нового игрока
                db.insertInitialPlayer("Hero");

                // Скрываем Continue (так как теперь новый игрок, но Continue всё равно будет)
                continueButton.setVisible(false);

                if (videoPlayer != null) videoPlayer.stop();
                bgMusic.stop();
                game.setScreen(new GameScreen(game));
            }
        });

        // ===== Settings Button (открывает окно настроек) =====
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsWindow.setVisible(true);
            }
        });

        // ===== Exit Button =====
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table centerTable = new Table();
        centerTable.setFillParent(true);
        centerTable.center();

        // Добавляем кнопки в таблицу (Continue только если есть)
        if (continueButton.isVisible()) {
            centerTable.add(continueButton).width(250).height(70).pad(15);
            centerTable.row();
        }

        centerTable.add(newGameButton).width(250).height(70).pad(15);
        centerTable.row();
        centerTable.add(settingsButton).width(250).height(70).pad(15);
        centerTable.row();
        centerTable.add(exitButton).width(250).height(70).pad(15);

        stage.addActor(centerTable);
    }

    // Метод для удаления progress.json
    private void deleteProgressFile() {
        try {
            FileHandle progressFile = Gdx.files.local("progress.json");
            if (progressFile.exists()) {
                progressFile.delete();
                Gdx.app.log("PROGRESS", "progress.json deleted");
            }
        } catch (Exception e) {
            Gdx.app.error("PROGRESS", "Error deleting progress.json", e);
        }
    }

    private void createLevelsWindow(Skin skin) {
        levelsWindow = new Window("Select Level", skin);
        levelsWindow.setSize(500, 350);
        levelsWindow.setPosition(
            Gdx.graphics.getWidth()/2f - 250,
            Gdx.graphics.getHeight()/2f - 175
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
                    if (videoPlayer != null) videoPlayer.stop();
                    bgMusic.stop();
                    game.setScreen(new GameScreen(game));
                }
            });

            table.add(levelBtn).width(120).height(50);
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
        table.add(close).colspan(5).padTop(20);

        levelsWindow.add(table);
        levelsWindow.setVisible(false);
        stage.addActor(levelsWindow);
    }

    private void createSettingsWindow(Skin skin) {
        settingsWindow = new Window("Settings", skin);
        settingsWindow.setSize(400, 250);
        settingsWindow.setPosition(
            Gdx.graphics.getWidth()/2f - 200,
            Gdx.graphics.getHeight()/2f - 125
        );

        Table table = new Table();
        table.defaults().pad(10);

        // Чекбокс для звука
        CheckBox soundCheck = new CheckBox(" Sound Enabled", skin);
        soundCheck.setChecked(soundEnabled);

        soundCheck.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundEnabled = soundCheck.isChecked();
                if (soundEnabled) {
                    bgMusic.setVolume(0.5f);
                } else {
                    bgMusic.setVolume(0f);
                }
                System.out.println("Sound: " + soundEnabled);
            }
        });

        // Ползунок громкости
        Slider volumeSlider = new Slider(0f, 1f, 0.1f, false, skin);
        volumeSlider.setValue(0.5f);
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float volume = volumeSlider.getValue();
                bgMusic.setVolume(volume);
            }
        });

        Label volumeLabel = new Label("Volume:", skin);

        TextButton close = new TextButton("Close", skin);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsWindow.setVisible(false);
            }
        });

        table.add(soundCheck).colspan(2);
        table.row();
        table.add(volumeLabel);
        table.add(volumeSlider);
        table.row();
        table.add(close).colspan(2).padTop(20);

        settingsWindow.add(table);
        settingsWindow.setVisible(false);
        stage.addActor(settingsWindow);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Обновляем видео
        if (videoPlayer != null) {
            videoPlayer.update();
            videoFrame = videoPlayer.getTexture();
        }

        // Рисуем фон
        if (videoFrame != null) {
            stage.getBatch().begin();

            float stageWidth = stage.getWidth();
            float stageHeight = stage.getHeight();

            float videoWidth = videoFrame.getWidth();
            float videoHeight = videoFrame.getHeight();

            float videoAspect = videoWidth / videoHeight;
            float stageAspect = stageWidth / stageHeight;

            float drawWidth, drawHeight;
            float x = 0, y = 0;

            if (videoAspect > stageAspect) {
                drawHeight = stageHeight;
                drawWidth = drawHeight * videoAspect;
                x = (stageWidth - drawWidth) / 2;
            } else {
                drawWidth = stageWidth;
                drawHeight = drawWidth / videoAspect;
                y = (stageHeight - drawHeight) / 2;
            }

            stage.getBatch().draw(videoFrame, x, y, drawWidth, drawHeight);
            stage.getBatch().end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        if (levelsWindow != null && levelsWindow.isVisible()) {
            levelsWindow.setPosition(width/2f - 250, height/2f - 175);
        }
        if (settingsWindow != null && settingsWindow.isVisible()) {
            settingsWindow.setPosition(width/2f - 200, height/2f - 125);
        }
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (videoPlayer != null) videoPlayer.dispose();
        stage.dispose();
        db.close();
        bgMusic.dispose();
        batch.dispose();
    }
}
