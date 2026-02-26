package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;

public class HUD {
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Viewport viewport;

    public HUD(Viewport viewport) {
        this.viewport = viewport;
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
    }

    public void render(SpriteBatch batch, Player player) {
        // Сохраняем проекционную матрицу
        batch.end();

        // Рисуем полоску здоровья
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Фон полоски здоровья
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(10, viewport.getWorldHeight() - 30, 200, 20);

        // Здоровье
        if (player.health > 60) {
            shapeRenderer.setColor(Color.GREEN);
        } else if (player.health > 30) {
            shapeRenderer.setColor(Color.ORANGE);
        } else {
            shapeRenderer.setColor(Color.RED);
        }

        float healthWidth = 200 * (player.health / 100f);
        shapeRenderer.rect(10, viewport.getWorldHeight() - 30, healthWidth, 20);

        shapeRenderer.end();

        // Рисуем текст
        batch.begin();
        font.draw(batch, "HP: " + player.health, 15, viewport.getWorldHeight() - 15);

        // Подсказки по управлению
        font.draw(batch, "A/D - движение", 10, 80);
        font.draw(batch, "Пробел - прыжок", 10, 60);
        font.draw(batch, "Shift - рывок", 10, 40);
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
