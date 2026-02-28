package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.aminferrr.MyJavaGame.screens.FirstScreen;

public class SettingsScreen implements Screen {

    private Main game;
    private Stage stage;

    // Значения громкости
    private float musicVolume;
    private float soundVolume;
    private float dialogVolume;

    // Preferences для сохранения настроек
    private Preferences prefs;

    // Ссылки на музыку и звуки (можно передать из Main)
    private Music backgroundMusic;
    private Sound attackSound;
    private Sound jumpSound;

    public SettingsScreen(Main game) {
        this(game, null, null, null);
    }

    public SettingsScreen(Main game, Music music, Sound attack, Sound jump) {
        this.game = game;
        this.backgroundMusic = music;
        this.attackSound = attack;
        this.jumpSound = jump;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Инициализируем Preferences
        prefs = Gdx.app.getPreferences("MyGameSettings");
        musicVolume = prefs.getFloat("musicVolume", 0.5f);
        soundVolume = prefs.getFloat("soundVolume", 0.5f);
        dialogVolume = prefs.getFloat("dialogVolume", 0.5f);

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Создаём слайдеры
        Slider musicSlider = new Slider(0f, 1f, 0.01f, false, skin);
        musicSlider.setValue(musicVolume);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                musicVolume = musicSlider.getValue();
                prefs.putFloat("musicVolume", musicVolume);
                prefs.flush();

                // Применяем громкость к музыке
                if (backgroundMusic != null) {
                    backgroundMusic.setVolume(musicVolume);
                }

                System.out.println("Music Volume: " + musicVolume);
            }
        });

        Slider soundSlider = new Slider(0f, 1f, 0.01f, false, skin);
        soundSlider.setValue(soundVolume);
        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                soundVolume = soundSlider.getValue();
                prefs.putFloat("soundVolume", soundVolume);
                prefs.flush();

                // Здесь можно сохранить громкость для использования в игре
                System.out.println("Sound Volume: " + soundVolume);
            }
        });

        Slider dialogSlider = new Slider(0f, 1f, 0.01f, false, skin);
        dialogSlider.setValue(dialogVolume);
        dialogSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                dialogVolume = dialogSlider.getValue();
                prefs.putFloat("dialogVolume", dialogVolume);
                prefs.flush();
                System.out.println("Dialog Volume: " + dialogVolume);
            }
        });

        // Кнопка "Назад"
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.setScreen(new FirstScreen(game));
            }
        });

        // Создаём метки (Label) для слайдеров
        Label musicLabel = new Label("Music", skin);
        Label soundLabel = new Label("Sound", skin);
        Label dialogLabel = new Label("Dialog", skin);

        // Добавляем элементы на таблицу
        table.add(musicLabel).pad(10);
        table.add(musicSlider).width(300).pad(10);
        table.row();

        table.add(soundLabel).pad(10);
        table.add(soundSlider).width(300).pad(10);
        table.row();

        table.add(dialogLabel).pad(10);
        table.add(dialogSlider).width(300).pad(10);
        table.row();

        table.add(backButton).colspan(2).pad(20).center();

        stage.addActor(table);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }

    // Геттеры для получения настроек из других экранов
    public float getMusicVolume() { return musicVolume; }
    public float getSoundVolume() { return soundVolume; }
    public float getDialogVolume() { return dialogVolume; }
}
