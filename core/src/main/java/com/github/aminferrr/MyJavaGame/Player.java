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
    private Animation<TextureRegion> idleAnim;
    private Animation<TextureRegion> runRightAnim;
    private Animation<TextureRegion> runLeftAnim;
    private Animation<TextureRegion> attackRightAnim;
    private Animation<TextureRegion> attackLeftAnim;
    private float stateTime = 0f;

    // ТЕКСТУРЫ
    private Texture moveSheet;
    private Texture attackSheet;

    // РАЗМЕРЫ
    private static final float PPM = 16f;
    // Физический размер тела (16x32 пикселей)
    private static final float PLAYER_WIDTH = 16f / PPM;      // 1 м
    private static final float PLAYER_HEIGHT = 32f / PPM;     // 2 м

    // РАЗМЕРЫ СПРАЙТА ДЛЯ ХОДЬБЫ (16x32)
    private static final int MOVE_FRAME_WIDTH = 16;
    private static final int MOVE_FRAME_HEIGHT = 32;
    private static final float MOVE_SPRITE_WIDTH = 16f / PPM;     // 1 м
    private static final float MOVE_SPRITE_HEIGHT = 32f / PPM;    // 2 м

    // РАЗМЕРЫ СПРАЙТА ДЛЯ АТАКИ (32x32)
    private static final int ATTACK_FRAME_WIDTH = 32;
    private static final int ATTACK_FRAME_HEIGHT = 32;
    private static final float ATTACK_SPRITE_WIDTH = 32f / PPM;   // 2 м
    private static final float ATTACK_SPRITE_HEIGHT = 32f / PPM;  // 2 м

    // ВИЗУАЛЬНОЕ СМЕЩЕНИЕ ВВЕРХ
    private static final float VISUAL_OFFSET_Y = 0.1f;

    // ДЛЯ ПРЫЖКА
    public boolean isGrounded = false;
    private static final float JUMP_FORCE = 16f;

    private boolean facingRight = true;
    private final float speed = 8f;

    // Состояние атаки
    private boolean attacking = false;
    private float attackTimer = 0f;
    private final float attackAnimDuration = 0.4f;

    public Player(World world) {
        createBody(world);
        loadAnimations();
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

        // ===== СЕНСОР НОГ =====
        PolygonShape footShape = new PolygonShape();

        float footWidth = PLAYER_WIDTH * 1.2f;
        float footHeight = 0.2f;
        float footOffsetY = -PLAYER_HEIGHT/2 - 0.05f - 0.5f;

        footShape.setAsBox(footWidth/2, footHeight/2, new Vector2(0, footOffsetY), 0);

        FixtureDef footFixtureDef = new FixtureDef();
        footFixtureDef.shape = footShape;
        footFixtureDef.isSensor = true;
        footFixtureDef.density = 0f;

        Fixture footSensor = body.createFixture(footFixtureDef);
        footSensor.setUserData("foot");

        footShape.dispose();

        Gdx.app.log("PLAYER", "Создан сенсор ног");
    }

    private void loadAnimations() {
        // ===== ХОДЬБА =====
        moveSheet = new Texture("characters/player/walking.png");
        TextureRegion[][] moveGrid = TextureRegion.split(moveSheet, MOVE_FRAME_WIDTH, MOVE_FRAME_HEIGHT);

        // Стоим (первый кадр первой строки)
        idleAnim = new Animation<>(0.5f, new TextureRegion[]{ moveGrid[0][0] });

        // Бег вправо (2-я строка)
        runRightAnim = new Animation<>(0.1f, moveGrid[1]);

        // Бег влево (4-я строка)
        runLeftAnim = new Animation<>(0.1f, moveGrid[3]);

        // ===== АТАКА =====
        attackSheet = new Texture("characters/player/attack.png");
        TextureRegion[][] attackGrid = TextureRegion.split(attackSheet, ATTACK_FRAME_WIDTH, ATTACK_FRAME_HEIGHT);

        // Атака вправо (3-я строка, индекс 2)
        attackRightAnim = new Animation<>(0.08f, attackGrid[2]);

        // Атака влево (4-я строка, индекс 3)
        attackLeftAnim = new Animation<>(0.08f, attackGrid[3]);
    }

    public void update(float delta, boolean left, boolean right, boolean jump, boolean attack, boolean groundedFromContact) {
        if (!alive) return;

        stateTime += delta;

        // Обновляем состояние земли
        this.isGrounded = groundedFromContact;

        // === Обновление состояния атаки ===
        if (attack && !attacking) {
            attacking = true;
            attackTimer = 0f;
        }
        if (attacking) {
            attackTimer += delta;
            if (attackTimer >= attackAnimDuration) {
                attacking = false;
            }
        }

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
            body.setLinearVelocity(vel.x, 0);
            body.applyLinearImpulse(0, JUMP_FORCE, body.getWorldCenter().x, body.getWorldCenter().y, true);
            Gdx.app.log("JUMP", "ПРЫЖОК!");
        }
    }

    public void render(SpriteBatch batch) {
        if (!alive) return;

        Vector2 pos = body.getPosition();

        // ВЫБОР АНИМАЦИИ
        boolean moving = Math.abs(body.getLinearVelocity().x) > 0.1f;

        TextureRegion frame;
        float drawWidth, drawHeight;

        if (attacking) {
            // Во время атаки — анимация атаки с размером 32x32
            if (facingRight) {
                frame = attackRightAnim.getKeyFrame(attackTimer, false);
            } else {
                frame = attackLeftAnim.getKeyFrame(attackTimer, false);
            }
            drawWidth = ATTACK_SPRITE_WIDTH;
            drawHeight = ATTACK_SPRITE_HEIGHT;
        } else if (moving) {
            // Бег — анимация бега с размером 16x32
            if (facingRight) {
                frame = runRightAnim.getKeyFrame(stateTime, true);
            } else {
                frame = runLeftAnim.getKeyFrame(stateTime, true);
            }
            drawWidth = MOVE_SPRITE_WIDTH;
            drawHeight = MOVE_SPRITE_HEIGHT;
        } else {
            // Стоим на месте
            frame = idleAnim.getKeyFrame(stateTime, true);
            drawWidth = MOVE_SPRITE_WIDTH;
            drawHeight = MOVE_SPRITE_HEIGHT;
        }

        // Центрируем спрайт по физическому телу
        float drawX = pos.x - drawWidth / 2f;
        float drawY = pos.y - drawHeight / 2f + VISUAL_OFFSET_Y- 0.5f;

        batch.draw(frame, drawX, drawY, drawWidth, drawHeight);
    }

    public void takeDamage(int dmg) {
        health -= dmg;
        if (health <= 0) alive = false;
    }

    public void dispose() {
        if (moveSheet != null) moveSheet.dispose();
        if (attackSheet != null) attackSheet.dispose();
    }
}
