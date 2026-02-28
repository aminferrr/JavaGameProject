package com.github.aminferrr.MyJavaGame.plot;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import com.github.aminferrr.MyJavaGame.elements.NPC;
import com.github.aminferrr.MyJavaGame.elements.Player;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NPCHandler {

    private final NPC patrolNpc;
    private final NPC dialogNpc;
    private final NPC thirdDialogNpc;

    // 🔥 список всех NPC
    private final List<NPC> allNpcs = new ArrayList<>();

    private final TextButton talkButton;
    private final DialogueManager dialogueManager;

    private boolean isNearStoryNpc = false;
    private boolean isNearThirdNpc = false;

    private static final float INTERACT_DISTANCE = 50f;

    public NPCHandler(Stage stage, Skin skin,
                      TiledMapTileLayer collisionLayer,
                      Player player) {  // БЕЗ КАМЕРЫ!

        // ===== Создание NPC =====

        patrolNpc = new NPC(100f, 60f, 80f, 150f, "nps/mike_anim.png");
        patrolNpc.setCollisionLayer(collisionLayer);
        patrolNpc.setCanMove(true);

        dialogNpc = new NPC(300f, 60f, 300f, 300f, "nps/lisa_anim.png");
        dialogNpc.setCollisionLayer(collisionLayer);
        dialogNpc.setCanMove(false);

        thirdDialogNpc = new NPC(1050f, 60f, 1050f, 1050f, "nps/nps_1.png");
        thirdDialogNpc.setCollisionLayer(collisionLayer);
        thirdDialogNpc.setCanMove(false);

        // ===== Добавляем всех в список =====
        allNpcs.add(patrolNpc);
        allNpcs.add(dialogNpc);
        allNpcs.add(thirdDialogNpc);

        // 🔥 Передаём список игроку (для коллизий)
        player.setNpcs(allNpcs);

        // ===== UI =====

        talkButton = new TextButton("Talk", skin);
        talkButton.setSize(60, 20);
        talkButton.setVisible(false);
        stage.addActor(talkButton);

        dialogueManager = new DialogueManager(stage);

        talkButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                if (dialogueManager.isActive() &&
                    dialogueManager.isWaitingForAnswer())
                    return;

                dialogueManager.stopCurrentSound();

                if (isNearStoryNpc) {
                    dialogueManager.startDialogue(
                        0, 13,
                        "I said you all...",
                        "story1_completed"
                    );
                }
                else if (isNearThirdNpc) {
                    if (dialogueManager.canStartDialogue(
                        "story3_completed",
                        "story1_completed")) {

                        dialogueManager.startDialogue(
                            14, 36,
                            "...",
                            "story3_completed"
                        );
                    }
                }
            }
        });
    }

    public void update(float delta, Player player) {

        // ===== Обновляем всех NPC =====
        for (NPC npc : allNpcs) {
            npc.update(delta);
        }

        if (!dialogueManager.isActive()) {
            checkPlayerProximity(player);
        } else {
            talkButton.setVisible(false);
        }
    }

    private void checkPlayerProximity(Player player) {

        if (distance(player, dialogNpc) <= INTERACT_DISTANCE) {

            showButtonAbove(dialogNpc);
            isNearStoryNpc = true;
            isNearThirdNpc = false;

        }
        else if (distance(player, thirdDialogNpc) <= INTERACT_DISTANCE) {

            showButtonAbove(thirdDialogNpc);
            isNearStoryNpc = false;
            isNearThirdNpc = true;

        }
        else {

            talkButton.setVisible(false);
            isNearStoryNpc = false;
            isNearThirdNpc = false;
        }
    }

    private void showButtonAbove(NPC npc) {
        talkButton.setVisible(true);

        // ПРОСТАЯ ПОЗИЦИЯ - как в рабочей версии
        talkButton.setPosition(
            npc.getX(),
            npc.getY() + npc.getHeight() + 5
        );
    }

    private float distance(Player player, NPC npc) {
        float dx = npc.getX() - player.getX();
        float dy = npc.getY() - player.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void updateDialoguePosition(OrthographicCamera camera,
                                       float stageWidth,
                                       float stageHeight) {
        dialogueManager.updatePosition(camera, stageWidth, stageHeight);
    }

    public void render(SpriteBatch batch, Player player) {

        // 🔥 правильная сортировка по Y (только для патрульного)
        float playerBottom = player.getY();
        float patrolBottom = patrolNpc.getY();

        if (playerBottom > patrolBottom) {
            patrolNpc.render(batch);
            player.render(batch);
        } else {
            player.render(batch);
            patrolNpc.render(batch);
        }

        dialogNpc.render(batch);
        thirdDialogNpc.render(batch);
    }

    public boolean isDialogueActive() {
        return dialogueManager.isActive();
    }

    public boolean isWaitingForAnswer() {
        return dialogueManager.isWaitingForAnswer();
    }

    public void nextDialogueLine() {
        if (!isWaitingForAnswer()) {
            dialogueManager.nextLine();
        }
    }

    public void submitPlayerAnswer(String answer) {
        dialogueManager.submitAnswer(answer);
    }

    public void stopCurrentDialogueSound() {
        dialogueManager.stopCurrentSound();
    }

    public void dispose() {
        for (NPC npc : allNpcs) {
            npc.dispose();
        }
    }
}
