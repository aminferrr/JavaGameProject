package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Player {
    public Body body;
    public boolean alive = true;
    public int health = 100;

    // АНИМАЦИИ
    private Texture idleSheet;
    private Texture walkSheet;
    private Texture jumpSheet;
    private Animation<TextureRegion> idleAnim;
    private Animation<TextureRegion> walkAnim;
    private Animation<TextureRegion> jumpAnim;
    private float stateTime = 0f;

    // РАЗМЕРЫ
    private static final float PPM = 16f;
    private static final float PLAYER_WIDTH = 1.0f;      // 16px / 16 = 1м
    private static final float PLAYER_HEIGHT = 1.5f;     // 24px / 16 = 1.5м

    // РАЗМЕРЫ СПРАЙТА
    private static final int FRAME_WIDTH = 48;
    private static final int FRAME_HEIGHT = 64;
    private static final float SPRITE_WIDTH = 3f;        // 48/16 = 3м
    private static final float SPRITE_HEIGHT = 4f;       // 64/16 = 4м

    // Смещение спрайта
    private float spriteOffsetX;
    private float spriteOffsetY;

    // ВИЗУАЛЬНОЕ СМЕЩЕНИЕ ВВЕРХ
    private static final float VISUAL_OFFSET_Y = 0.3f;

    // ДЛЯ ПРЫЖКА
    public boolean isGrounded = false;
    private static final float JUMP_FORCE = 25f;  // ЕЩЕ УВЕЛИЧИЛ СИЛУ ПРЫЖКА

    private boolean facingRight = true;
    private final float speed = 8f;

    public Player(World world) {
        createBody(world);
        loadAnimations();

        spriteOffsetX = (SPRITE_WIDTH - PLAYER_WIDTH) / 2f;
        spriteOffsetY = (SPRITE_HEIGHT - PLAYER_HEIGHT);
    }

    private void createBody(World world) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(10f, 15f);
        bodyDef.fixedRotation = true;

        body = world.createBody(bodyDef);

        // ОСНОВНОЙ ХИТБОКС
        PolygonShape mainShape = new PolygonShape();
        mainShape.setAsBox(PLAYER_WIDTH / 2, PLAYER_HEIGHT / 2);

        FixtureDef mainFixtureDef = new FixtureDef();
        mainFixtureDef.shape = mainShape;
        mainFixtureDef.density = 1f;
        mainFixtureDef.friction = 0.5f;
        mainFixtureDef.restitution = 0f;

        Fixture mainFixture = body.createFixture(mainFixtureDef);
        mainFixture.setUserData("player");
        mainShape.dispose();

        // ===== БОЛЬШОЙ СЕНСОР НОГ =====
        PolygonShape footShape = new PolygonShape();

        // Делаем сенсор очень большим и низким
        float footWidth = PLAYER_WIDTH * 2.0f;  // 200% ширины игрока
        float footHeight = 0.8f;                 // 80 см высота
        float footOffsetY = -PLAYER_HEIGHT/2 - 0.2f; // 20 см ниже ног

        footShape.setAsBox(footWidth/2, footHeight/2, new Vector2(0, footOffsetY), 0);

        FixtureDef footFixtureDef = new FixtureDef();
        footFixtureDef.shape = footShape;
        footFixtureDef.isSensor = true;
        footFixtureDef.density = 0f;

        Fixture footSensor = body.createFixture(footFixtureDef);
        footSensor.setUserData("foot");

        footShape.dispose();

        Gdx.app.log("PLAYER", "Создан сенсор ног: позиция Y=" + footOffsetY +
            " ширина=" + footWidth + " высота=" + footHeight);
    }

    private void loadAnimations() {
        idleSheet = new Texture("characters/player/Idle/Idle_Down.png");
        walkSheet = new Texture("characters/player/Walk/walk_Right_Down.png");
        jumpSheet = new Texture("characters/player/Idle/Idle_Right_Up.png");

        idleAnim = createAnimation(idleSheet, 4, 0.15f);
        walkAnim = createAnimation(walkSheet, 6, 0.1f);
        jumpAnim = createAnimation(jumpSheet, 4, 0.15f);
    }

    private Animation<TextureRegion> createAnimation(Texture sheet, int frameCount, float frameDuration) {
        TextureRegion[][] grid = TextureRegion.split(sheet, FRAME_WIDTH, FRAME_HEIGHT);
        TextureRegion[] frames = new TextureRegion[frameCount];

        for (int i = 0; i < frameCount && i < grid[0].length; i++) {
            frames[i] = grid[0][i];
        }

        return new Animation<>(frameDuration, frames);
    }

    public void update(float delta, boolean left, boolean right, boolean jump, boolean attack, boolean groundedFromContact) {
        if (!alive) return;

        stateTime += delta;

        // Обновляем состояние земли
        this.isGrounded = groundedFromContact;

        Vector2 vel = body.getLinearVelocity();

        // ДВИЖЕНИЕ
        float vx = 0;
        if (left) {
            vx = -speed;
            facingRight = false;
        }
        if (right) {
            vx = speed;
            facingRight = true;
        }

        body.setLinearVelocity(vx, vel.y);

        // ПРЫЖОК
        if (jump && isGrounded) {
            // Сбрасываем вертикальную скорость
            body.setLinearVelocity(vel.x, 0);
            // Применяем импульс вверх
            body.applyLinearImpulse(0, JUMP_FORCE, body.getWorldCenter().x, body.getWorldCenter().y, true);
            Gdx.app.log("JUMP", "ПРЫЖОК! Сила=" + JUMP_FORCE + " isGrounded=" + isGrounded);
        }
    }

    public void render(SpriteBatch batch) {
        if (!alive) return;

        Vector2 pos = body.getPosition();

        // ВЫБОР АНИМАЦИИ
        boolean moving = Math.abs(body.getLinearVelocity().x) > 0.1f;
        boolean inAir = !isGrounded;

        TextureRegion frame;
        if (inAir) {
            frame = jumpAnim.getKeyFrame(stateTime, true);
        } else if (moving) {
            frame = walkAnim.getKeyFrame(stateTime, true);
        } else {
            frame = idleAnim.getKeyFrame(stateTime, true);
        }

        // ПОВОРОТ
        if (!facingRight && !frame.isFlipX()) {
            frame.flip(true, false);
        } else if (facingRight && frame.isFlipX()) {
            frame.flip(true, false);
        }

        // РИСУЕМ
        float drawX = pos.x - spriteOffsetX;
        float drawY = pos.y - spriteOffsetY + VISUAL_OFFSET_Y;

        batch.draw(frame, drawX, drawY, SPRITE_WIDTH, SPRITE_HEIGHT);
    }

    public void takeDamage(int dmg) {
        health -= dmg;
        if (health <= 0) alive = false;
    }

    public void dispose() {
        if (idleSheet != null) idleSheet.dispose();
        if (walkSheet != null) walkSheet.dispose();
        if (jumpSheet != null) jumpSheet.dispose();
    }
}
