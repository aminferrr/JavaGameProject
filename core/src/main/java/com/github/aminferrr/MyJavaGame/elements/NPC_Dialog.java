package com.github.aminferrr.MyJavaGame.elements;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class NPC_Dialog extends NPC {

    private TextButton talkButton;
    private Label dialogLabel;

    public NPC_Dialog(float startX, float startY, float minX, float maxX, String texturePath,
                      Stage stage, Skin skin, TiledMapTileLayer collisionLayer) {
        super(startX, startY, minX, maxX, texturePath); // передаём путь к картинке
        setCollisionLayer(collisionLayer);
        setCanMove(false);

        // ===== Инициализация кнопки и лейбла =====
        talkButton = new TextButton("Talk", skin);
        talkButton.setSize(60, 20);
        talkButton.setVisible(false);
        stage.addActor(talkButton);

        dialogLabel = new Label("", skin);
        dialogLabel.setVisible(false);
        stage.addActor(dialogLabel);
    }

    public void talk(String message) {
        dialogLabel.setText(message);
        dialogLabel.setVisible(true);
    }

    public TextButton getTalkButton() {
        return talkButton;
    }

    public Label getDialogLabel() {
        return dialogLabel;
    }
}
