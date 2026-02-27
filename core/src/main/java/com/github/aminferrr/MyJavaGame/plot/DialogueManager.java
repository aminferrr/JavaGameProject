package com.github.aminferrr.MyJavaGame.plot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.audio.Sound;

import java.util.HashMap;
import java.util.Map;

public class DialogueManager {

    public static class DialogueLine {
        public String type;     // "message" или "answer"
        public String speaker;
        public String text;
        public String image;
        public String audio;    // путь к аудио для реплики
    }

    private Array<DialogueLine> story;
    private int currentIndex = 0;
    private int startIndex = 0;
    private int endIndex = 0;

    private boolean active = false;
    private boolean waitingForAnswer = false; // ждём ввода игрока
    private String expectedAnswer = "";

    private final Stage stage;
    private final Skin skin;
    private final Table dialogueTable;
    private final Label textLabel;
    private final Image characterImage;
    private final Image background;

    private Sound currentSound; // текущий проигрываемый звук

    private final String PROGRESS_FILE = "progress.json";
    private String currentProgressKey;
    private Map<String, Boolean> progressMap = new HashMap<>();

    public DialogueManager(Stage stage) {
        this.stage = stage;

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        // ===== Фон =====
        background = new Image(skin.newDrawable("white", new Color(0, 0, 0, 0.7f)));
        background.setSize(600, 150);
        background.setPosition(20, 20);
        background.setVisible(false);
        stage.addActor(background);

        // ===== Таблица =====
        dialogueTable = new Table();
        dialogueTable.setSize(background.getWidth(), background.getHeight());
        dialogueTable.setPosition(background.getX(), background.getY());
        dialogueTable.left().bottom().pad(10);
        dialogueTable.setVisible(false);
        stage.addActor(dialogueTable);

        characterImage = new Image();
        dialogueTable.add(characterImage).size(100, 100).padRight(10);
        dialogueTable.row();

        textLabel = new Label("", skin);
        textLabel.setWrap(true);
        textLabel.setWidth(480);
        dialogueTable.add(textLabel).expandX().fillX();
        dialogueTable.row();

        loadStory();
        loadProgressMap(); // загружаем прогресс
    }

    // ===== Загрузка JSON диалогов =====
    private void loadStory() {
        Json json = new Json();
        try {
            story = new Array<>(json.fromJson(
                DialogueLine[].class,
                Gdx.files.internal("story/story.json")
            ));
        } catch (Exception e) {
            System.out.println("Ошибка загрузки story.json: " + e.getMessage());
            story = new Array<>();
        }
    }

    // ===== Запуск диалога =====
    public void startDialogue(int from, int to, String repeatText, String progressKey) {
        if (story == null || story.size == 0) return;

        currentProgressKey = progressKey;

        // Если блок уже пройден — показываем короткую фразу
        if (progressMap.getOrDefault(progressKey, false)) {
            textLabel.setText(repeatText);
            characterImage.setDrawable(null);
            background.setVisible(true);
            dialogueTable.setVisible(true);

            active = true;
            currentIndex = -1; // режим повторной фразы
            return;
        }

        startIndex = Math.max(0, from);
        endIndex = Math.min(to, story.size - 1);

        currentIndex = startIndex;
        active = true;
        waitingForAnswer = false;

        showCurrentLine();
    }

    // ===== Показ строки =====
    private void showCurrentLine() {
        if (!active) return;

        stopCurrentSound(); // остановка предыдущего звука

        if (currentIndex <= endIndex) {
            DialogueLine line = story.get(currentIndex);

            // ===== Если тип "answer" =====
            if ("answer".equals(line.type)) {
                waitingForAnswer = true;
                expectedAnswer = line.text;
                textLabel.setText("");          // скрываем текст игрока
                characterImage.setDrawable(null);
                background.setVisible(true);
                dialogueTable.setVisible(true);
                return; // ждём ввода игрока
            }

            // ===== Если тип "message" =====
            waitingForAnswer = false;
            textLabel.setText(line.speaker + ": " + line.text);

            if (line.image != null && !line.image.isEmpty()) {
                Texture tex = new Texture(Gdx.files.internal(line.image));
                characterImage.setDrawable(new Image(tex).getDrawable());
            } else {
                characterImage.setDrawable(null);
            }

            // ===== Проигрываем озвучку =====
            if (line.audio != null && !line.audio.isEmpty()) {
                try {
                    currentSound = Gdx.audio.newSound(Gdx.files.internal(line.audio));
                    currentSound.play();
                } catch (Exception e) {
                    System.out.println("Ошибка воспроизведения аудио: " + e.getMessage());
                }
            }

            background.setVisible(true);
            dialogueTable.setVisible(true);

        } else {
            // завершение блока
            if (currentProgressKey != null) {
                progressMap.put(currentProgressKey, true);
                saveProgressMap();
            }
            endDialogue();
        }
    }

    // ===== Следующая строка =====
    public void nextLine() {
        if (!active || waitingForAnswer) return;

        if (currentIndex == -1) { // повторная фраза
            endDialogue();
            return;
        }

        currentIndex++;
        showCurrentLine();
    }

    // ===== Ввод ответа игрока =====
    public void submitAnswer(String answer) {
        if (waitingForAnswer && answer.equals(expectedAnswer)) {
            waitingForAnswer = false;
            currentIndex++;
            showCurrentLine();
        }
    }

    // ===== Обновление позиции диалога =====
    public void updatePosition(OrthographicCamera camera, float stageWidth, float stageHeight) {
        if (!active) return;

        float tableWidth = dialogueTable.getWidth();
        float tableHeight = dialogueTable.getHeight();

        float x = camera.position.x - tableWidth / 2f;
        float y = camera.position.y - tableHeight / 2f;

        dialogueTable.setPosition(x, y);
        background.setPosition(x, y);
    }

    // ===== Завершение диалога =====
    private void endDialogue() {
        active = false;
        waitingForAnswer = false;
        textLabel.setText("");
        characterImage.setDrawable(null);
        background.setVisible(false);

        stopCurrentSound();

        if (currentProgressKey != null) {
            progressMap.put(currentProgressKey, true);
            saveProgressMap();
        }
    }

    // ===== Новый метод: остановка текущего звука =====
    public void stopCurrentSound() {
        if (currentSound != null) {
            currentSound.stop();
            currentSound.dispose();
            currentSound = null;
        }
    }

    // ===== Геттеры =====
    public boolean isActive() {
        return active;
    }

    public boolean isWaitingForAnswer() {
        return waitingForAnswer;
    }

    public boolean canStartDialogue(String progressKey, String previousKey) {
        return previousKey == null || progressMap.getOrDefault(previousKey, false);
    }

    // ===== Прогресс: загрузка =====
    private void loadProgressMap() {
        try {
            FileHandle file = Gdx.files.local(PROGRESS_FILE);
            if (!file.exists()) return;

            Json json = new Json();
            progressMap = json.fromJson(HashMap.class, file.readString());

        } catch (Exception e) {
            System.out.println("Ошибка чтения прогресса: " + e.getMessage());
            progressMap = new HashMap<>();
        }
    }

    // ===== Прогресс: сохранение =====
    private void saveProgressMap() {
        try {
            FileHandle file = Gdx.files.local(PROGRESS_FILE);
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            file.writeString(json.toJson(progressMap), false);

        } catch (Exception e) {
            System.out.println("Ошибка сохранения прогресса: " + e.getMessage());
        }
    }
}
