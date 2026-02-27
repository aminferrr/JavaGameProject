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

// Импорты для видео
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;

public class FirstScreen implements Screen {

    private Main game;
    private Stage stage;
    private Database db;
    private Music bgMusic;
    private SpriteBatch batch;

    // ВИДЕОПЛЕЕР (вместо Texture)
    private VideoPlayer videoPlayer;
    private Texture videoFrame; // текущий кадр видео

    // Окна для уровней и настроек
    private Window levelsWindow;
    private Window settingsWindow;
    private boolean soundEnabled = true;

    public FirstScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        db = new Database();
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Загружаем музыку
        bgMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/music/1bgmu.mp3"));
        bgMusic.setLooping(true);
        bgMusic.setVolume(0.5f);
        bgMusic.play();

        // === ЗАГРУЗКА ВИДЕО (ПРАВИЛЬНО!) ===
        try {
            videoPlayer = VideoPlayerCreator.createVideoPlayer();
            FileHandle videoFile = Gdx.files.internal("back.webm");
            videoPlayer.play(videoFile);  // сразу начинаем воспроизведение
            videoPlayer.setVolume(0f);     // звук отключаем (чтоб не мешал музыке)
            videoPlayer.setLooping(true);  // зацикливаем
            Gdx.app.log("VIDEO", "Видео успешно загружено и запущено");
        } catch (Exception e) {
            Gdx.app.error("VIDEO", "Ошибка загрузки видео", e);
            videoPlayer = null;
        }

        // Создаем окна
        createLevelsWindow(skin);
        createSettingsWindow(skin);

        // Создаем центральное меню (Continue, Reset, Exit)
        createCenterMenu(skin);
    }

    private void createCenterMenu(Skin skin) {
        boolean playerExists = db.checkPlayerExists();

        TextButton playButton = new TextButton(playerExists ? "Continue" : "Start", skin);
        TextButton resetButton = new TextButton("Reset Player", skin);
        TextButton exitButton = new TextButton("Exit", skin);

        playButton.addListener(new ClickListener() {
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

        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    db.resetPlayerTable();
                    System.out.println("Player table reset!");
                    // Обновляем текст кнопки на "Start" после сброса
                    playButton.setText("Start");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table centerTable = new Table();
        centerTable.setFillParent(true);
        centerTable.center();
        centerTable.add(playButton).width(250).height(70).pad(15);
        centerTable.row();
        centerTable.add(resetButton).width(250).height(70).pad(15);
        centerTable.row();
        centerTable.add(exitButton).width(250).height(70).pad(15);

        stage.addActor(centerTable);
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

        // Обновляем видео (получаем новый кадр)
        if (videoPlayer != null) {
            videoPlayer.update();
            videoFrame = videoPlayer.getTexture();
        }

        // Рисуем фон через stage (чтобы сохранялись пропорции)
        if (videoFrame != null) {
            stage.getBatch().begin();

            // Получаем размеры stage
            float stageWidth = stage.getWidth();
            float stageHeight = stage.getHeight();

            // Получаем размеры видео
            float videoWidth = videoFrame.getWidth();
            float videoHeight = videoFrame.getHeight();

            // Вычисляем пропорции
            float videoAspect = videoWidth / videoHeight;
            float stageAspect = stageWidth / stageHeight;

            float drawWidth, drawHeight;
            float x = 0, y = 0;

            if (videoAspect > stageAspect) {
                // Видео шире, чем stage - подгоняем по высоте
                drawHeight = stageHeight;
                drawWidth = drawHeight * videoAspect;
                x = (stageWidth - drawWidth) / 2;
            } else {
                // Видео выше, чем stage - подгоняем по ширине
                drawWidth = stageWidth;
                drawHeight = drawWidth / videoAspect;
                y = (stageHeight - drawHeight) / 2;
            }

            // Рисуем видео с правильными пропорциями
            stage.getBatch().draw(videoFrame, x, y, drawWidth, drawHeight);
            stage.getBatch().end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Обновляем позиции окон
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
