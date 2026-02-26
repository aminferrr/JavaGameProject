package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class Enemy {
    public Body body;
    public boolean alive = true;
    public int health = 50;

    private AnimationManager animManager;

    // Анимации
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> runAnimation;
    private Animation<TextureRegion> attackAnimation;
    private Animation<TextureRegion> deathAnimation;
    private Animation<TextureRegion> wakeAnimation; // Для Droid Zapper

    // Типы врагов
    public enum EnemyType {
        ZAPPER("zapper"),    // Атакующий дроид
        SHIELD("shield");    // Защитный дроид

        private final String type;
        EnemyType(String type) { this.type = type; }
        public String getType() { return type; }
    }

    private EnemyType type;

    private final float speed = 3f;
    private final float attackRange = 1.5f;
    private final float attackCooldown = 1.0f;
    private float attackTimer = 0;

    private enum State { IDLE, RUNNING, ATTACKING, DEAD, WAKING }
    private State currentState = State.IDLE;

    // Для патрулирования
    private Vector2 spawnPoint;
    private float patrolRadius = 3f;
    private float patrolTimer = 0;
    private boolean movingRight = true;

    public Enemy(World world, Vector2 spawnPos, String typeStr) {
        this.type = typeStr.equals("zapper") ? EnemyType.ZAPPER : EnemyType.SHIELD;
        this.spawnPoint = spawnPos.cpy();

        createBody(world, spawnPos);
        loadAnimations();
        animManager = new AnimationManager();
    }

    private void createBody(World world, Vector2 spawnPos) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnPos);
        bodyDef.fixedRotation = true;

        body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 1.0f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.2f;
        fixtureDef.friction = 0.4f;

        Fixture fixture = body.createFixture(fixtureDef);
        fixture.setUserData("enemy");
        shape.dispose();
    }

    private void loadAnimations() {
        String basePath = "characters/droids/";

        if (type == EnemyType.ZAPPER) {
            basePath += "Droid Zapper/";
            wakeAnimation = createAnimation(basePath + "wake.png", 4, 0.15f);
            runAnimation = createAnimation(basePath + "run.png", 6, 0.1f);
            attackAnimation = createAnimation(basePath + "attack.png", 4, 0.1f);
            deathAnimation = createAnimation(basePath + "damaged and death.png", 6, 0.1f);
            idleAnimation = wakeAnimation; // Временно используем wake как idle
        } else {
            basePath += "shield droid/";
            idleAnimation = createAnimation(basePath + "static idle.png", 4, 0.15f);
            runAnimation = createAnimation(basePath + "walk.png", 6, 0.1f);
            attackAnimation = createAnimation(basePath + "shock attack.png", 4, 0.1f);
            deathAnimation = createAnimation(basePath + "hit and death.png", 6, 0.1f);
        }
    }

    private Animation<TextureRegion> createAnimation(String filePath, int frameCount, float frameDuration) {
        Array<TextureRegion> frames = new Array<>();

        TextureRegion spriteSheet = new TextureRegion(new com.badlogic.gdx.graphics.Texture(filePath));

        int frameWidth = spriteSheet.getRegionWidth() / frameCount;
        int frameHeight = spriteSheet.getRegionHeight();

        for (int i = 0; i < frameCount; i++) {
            frames.add(new TextureRegion(spriteSheet, i * frameWidth, 0, frameWidth, frameHeight));
        }

        return new Animation<>(frameDuration, frames);
    }

    public void update(float delta, Vector2 playerPos) {
        if (!alive) {
            currentState = State.DEAD;
            animManager.update(delta);
            return;
        }

        Vector2 enemyPos = body.getPosition();
        float distanceToPlayer = enemyPos.dst(playerPos);
        float distanceToSpawn = enemyPos.dst(spawnPoint);

        // Обновляем таймеры
        if (currentState != State.ATTACKING) {
            attackTimer += delta;
        }

        // State machine для поведения врага
        State newState = currentState;

        // Если игрок далеко - патрулируем
        if (distanceToPlayer > 6f) {
            newState = State.IDLE;
            patrol(delta);
        }
        // Если игрок в зоне видимости - преследуем
        else if (distanceToPlayer > attackRange) {
            newState = State.RUNNING;
            chasePlayer(playerPos);
        }
        // Если игрок в зоне атаки - атакуем
        else if (attackTimer >= attackCooldown) {
            newState = State.ATTACKING;
            attackTimer = 0;
            body.setLinearVelocity(0, 0);
        }

        // Не уходить далеко от точки спавна
        if (distanceToSpawn > patrolRadius * 2) {
            // Возвращаемся к точке спавна
            Vector2 direction = spawnPoint.cpy().sub(enemyPos).nor();
            body.setLinearVelocity(direction.scl(speed));
        }

        if (newState != currentState) {
            currentState = newState;
        }

        animManager.update(delta);
    }

    private void patrol(float delta) {
        patrolTimer += delta;

        if (patrolTimer > 2f) {
            movingRight = !movingRight;
            patrolTimer = 0;
        }

        float vx = movingRight ? speed * 0.5f : -speed * 0.5f;
        body.setLinearVelocity(vx, body.getLinearVelocity().y);
    }

    private void chasePlayer(Vector2 playerPos) {
        Vector2 enemyPos = body.getPosition();
        Vector2 direction = playerPos.cpy().sub(enemyPos).nor();
        body.setLinearVelocity(direction.scl(speed));
    }

    public void render(SpriteBatch batch) {
        if (!alive && animManager.isAnimationFinished()) {
            return;
        }

        Vector2 pos = body.getPosition();

        Animation<TextureRegion> anim = getCurrentAnimation();
        if (anim != null) {
            boolean looping = (currentState != State.ATTACKING && currentState != State.DEAD);
            animManager.setAnimation(anim, looping);

            TextureRegion frame = animManager.getFrame();

            // Поворачиваем врага в сторону движения
            if (body.getLinearVelocity().x < 0 && !frame.isFlipX()) {
                frame.flip(true, false);
            } else if (body.getLinearVelocity().x > 0 && frame.isFlipX()) {
                frame.flip(true, false);
            }

            float width = 1f;
            float height = 2f;

            // Цветовая подсветка для разных типов
            if (type == EnemyType.ZAPPER) {
                batch.setColor(Color.RED);
            } else {
                batch.setColor(Color.CYAN);
            }

            batch.draw(frame,
                pos.x - width/2,
                pos.y - height/2,
                width, height);

            batch.setColor(Color.WHITE);
        }
    }

    private Animation<TextureRegion> getCurrentAnimation() {
        switch (currentState) {
            case IDLE: return idleAnimation;
            case WAKING: return wakeAnimation;
            case RUNNING: return runAnimation;
            case ATTACKING: return attackAnimation;
            case DEAD: return deathAnimation;
            default: return idleAnimation;
        }
    }

    public void takeDamage(int amount) {
        health -= amount;
        if (health <= 0) {
            alive = false;
            currentState = State.DEAD;
        }
    }

    public void dispose() {
        // Очистка текстур (если нужно)
    }
}
