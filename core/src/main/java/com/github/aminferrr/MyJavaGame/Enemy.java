package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.Gdx;
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
    private Animation<TextureRegion> wakeAnimation;

    // Типы врагов
    public enum EnemyType {
        ZAPPER("zapper"),
        SHIELD("shield"),
        WHEEL("wheel"),
        MUD_GUARD("mud"),
        STORMHEAD("stormhead");

        private final String type;
        EnemyType(String type) { this.type = type; }
        public String getType() { return type; }
    }

    private EnemyType type;

    private static final float PPM = 16f;

    private final Vector2 tmp = new Vector2();

    private final float speed = 3f;
    private final float attackRange = 1.0f; // Уменьшили дистанцию атаки
    private final float visionRange = 5f;   // Немного уменьшили зону видимости
    private final float attackCooldown = 1.0f;
    private float attackTimer = 0;

    private enum State { IDLE, RUNNING, ATTACKING, DEAD, WAKING }
    private State currentState = State.IDLE;

    private Vector2 spawnPoint;
    private float patrolRadius = 3f;
    private float patrolTimer = 0;
    private boolean movingRight = true;

    public Enemy(World world, Vector2 spawnPos, String typeStr) {
        if ("zapper".equals(typeStr))          this.type = EnemyType.ZAPPER;
        else if ("wheel".equals(typeStr))      this.type = EnemyType.WHEEL;
        else if ("mud".equals(typeStr))        this.type = EnemyType.MUD_GUARD;
        else if ("stormhead".equals(typeStr))  this.type = EnemyType.STORMHEAD;
        else                                    this.type = EnemyType.SHIELD;

        this.spawnPoint = spawnPos.cpy();

        createBody(world, spawnPos);
        loadAnimations();
        animManager = new AnimationManager();

        Gdx.app.log("ENEMY", "Создан враг типа " + typeStr + " на позиции " + spawnPos);
    }

    private void createBody(World world, Vector2 spawnPos) {
        BodyDef bodyDef = new BodyDef();
        boolean flying = (type == EnemyType.ZAPPER || type == EnemyType.WHEEL || type == EnemyType.STORMHEAD);
        bodyDef.type = flying ? BodyDef.BodyType.KinematicBody : BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnPos);
        bodyDef.fixedRotation = true;

        body = world.createBody(bodyDef);

        // СОЗДАЕМ ОСНОВНОЙ ХИТБОКС С КОЛЛИЗИЕЙ
        PolygonShape mainShape = new PolygonShape();

        // ЕЩЁ БОЛЬШЕ УМЕНЬШАЕМ размеры хитбоксов
        if (type == EnemyType.MUD_GUARD) {
            // Mud Guard
            mainShape.setAsBox(0.3f, 0.8f); // Было 0.5f, 1.2f
        } else if (type == EnemyType.ZAPPER) {
            // Toaster Bot - делаем совсем маленьким
            mainShape.setAsBox(-0.2f, -0.2f); // Было 1.0f, 0.4f
        } else if (type == EnemyType.WHEEL) {
            // Wheel Bot - делаем совсем маленьким
            mainShape.setAsBox(-0.2f, -0.2f); // Было 1.1f, 0.5f
        } else if (type == EnemyType.STORMHEAD) {
            // Stormhead - уменьшаем значительно
            mainShape.setAsBox(0.5f, 0.6f); // Было 1.2f, 1.3f
        } else if (flying) {
            mainShape.setAsBox(0.4f, 0.2f);
        } else {
            mainShape.setAsBox(0.3f, 0.4f);
        }

        FixtureDef mainFixtureDef = new FixtureDef();
        mainFixtureDef.shape = mainShape;
        mainFixtureDef.density = 1.2f;
        mainFixtureDef.friction = 0.4f;
        mainFixtureDef.restitution = 0f;

        Fixture mainFixture = body.createFixture(mainFixtureDef);
        mainFixture.setUserData("enemy");

        body.setUserData(this);
        mainShape.dispose();

        body.setGravityScale(flying ? 0f : 1f);

        Gdx.app.log("ENEMY", "Создан враг с коллизией типа " + type);
    }

    private void loadAnimations() {
        String basePath = "characters/droids/";

        try {
            if (type == EnemyType.ZAPPER) {
                basePath += "Toaster Bot/";
                idleAnimation = createHorizontalAnimation(basePath + "idle.png", 106, 22, 0.15f);
                runAnimation = createHorizontalAnimation(basePath + "run.png", 106, 22, 0.10f);
                attackAnimation = createHorizontalAnimation(basePath + "attack.png", 106, 22, 0.10f);
                deathAnimation = createHorizontalAnimation(basePath + "death.png", 106, 22, 0.10f);
            } else if (type == EnemyType.WHEEL) {
                basePath += "Wheel Bot/";
                idleAnimation = createHorizontalAnimation(basePath + "move 112x26.png", 112, 26, 0.12f);
                runAnimation = createHorizontalAnimation(basePath + "move 112x26.png", 112, 26, 0.08f);
                attackAnimation = createHorizontalAnimation(basePath + "shoot 112x26.png", 112, 26, 0.08f);
                deathAnimation = createHorizontalAnimation(basePath + "death 112x26.png", 112, 26, 0.10f);
            } else if (type == EnemyType.MUD_GUARD) {
                // Mud Guard - ВЕРТИКАЛЬНЫЙ спрайт-лист!
                basePath += "Mud Guard/";

                // 7 кадров сверху вниз, размер кадра 82x? (нужно определить)
                // По вашим данным: ширина 82, высота кадра примерно 23-24
                int frameWidth = 82;
                int frameHeight = 23; // Примерно, уточните по файлу

                idleAnimation = createVerticalAnimation(basePath + "idle.png", frameWidth, frameHeight, 7, 0.16f);
                runAnimation = createVerticalAnimation(basePath + "Run.png", frameWidth, frameHeight, 7, 0.10f);
                attackAnimation = createVerticalAnimation(basePath + "attack 1.png", frameWidth, frameHeight, 7, 0.10f);
                deathAnimation = createVerticalAnimation(basePath + "damaged and death.png", frameWidth, frameHeight, 7, 0.12f);
            } else if (type == EnemyType.STORMHEAD) {
                basePath += "stormhead/";
                idleAnimation = createHorizontalAnimation(basePath + "idle.png", 119, 124, 0.16f);
                runAnimation = createHorizontalAnimation(basePath + "run.png", 119, 124, 0.10f);
                attackAnimation = createHorizontalAnimation(basePath + "attack.png", 119, 124, 0.10f);
                deathAnimation = createHorizontalAnimation(basePath + "death.png", 119, 124, 0.12f);
            } else {
                basePath += "shield droid/";
                idleAnimation = createHorizontalAnimation(basePath + "static idle.png", 32, 32, 0.15f);
                runAnimation = createHorizontalAnimation(basePath + "walk.png", 32, 32, 0.1f);
                attackAnimation = createHorizontalAnimation(basePath + "shock attack.png", 32, 32, 0.1f);
                deathAnimation = createHorizontalAnimation(basePath + "hit and death.png", 32, 32, 0.1f);
            }

            Gdx.app.log("ENEMY", "Анимации загружены для " + type);
        } catch (Exception e) {
            Gdx.app.error("ENEMY", "Ошибка загрузки анимаций для " + type + ": " + e.getMessage());
        }
    }

    // Для горизонтальных спрайт-листов (кадры идут слева направо)
    private Animation<TextureRegion> createHorizontalAnimation(String filePath, int frameWidth, int frameHeight, float frameDuration) {
        try {
            Array<TextureRegion> frames = new Array<>();
            com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(filePath);
            int frameCount = texture.getWidth() / frameWidth;

            for (int i = 0; i < frameCount; i++) {
                frames.add(new TextureRegion(texture, i * frameWidth, 0, frameWidth, frameHeight));
            }

            return new Animation<>(frameDuration, frames);
        } catch (Exception e) {
            Gdx.app.error("ENEMY", "Не удалось загрузить " + filePath);
            return null;
        }
    }

    // ДЛЯ ВЕРТИКАЛЬНЫХ СПРАЙТ-ЛИСТОВ (кадры идут сверху вниз)
    private Animation<TextureRegion> createVerticalAnimation(String filePath, int frameWidth, int frameHeight, int frameCount, float frameDuration) {
        try {
            Array<TextureRegion> frames = new Array<>();
            com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(filePath);

            for (int i = 0; i < frameCount; i++) {
                frames.add(new TextureRegion(texture, 0, i * frameHeight, frameWidth, frameHeight));
            }

            return new Animation<>(frameDuration, frames);
        } catch (Exception e) {
            Gdx.app.error("ENEMY", "Не удалось загрузить " + filePath);
            return null;
        }
    }

    public void update(float delta, Player player) {
        if (!alive) {
            currentState = State.DEAD;
            body.setLinearVelocity(0, 0);
            animManager.update(delta);
            return;
        }

        Vector2 playerPos = player.body.getPosition();
        Vector2 enemyPos = body.getPosition();

        float distanceToPlayer = enemyPos.dst(playerPos);
        float distanceToSpawn = enemyPos.dst(spawnPoint);

        attackTimer += delta;

        // Летающие боты
        if (type == EnemyType.ZAPPER || type == EnemyType.WHEEL || type == EnemyType.STORMHEAD) {
            float dx = playerPos.x - enemyPos.x;
            float absDx = Math.abs(dx);

            if (absDx > visionRange) {
                currentState = State.RUNNING;
                patrolFlying();
            } else if (absDx > attackRange) {
                currentState = State.RUNNING;
                float vx = (dx < 0) ? -speed : speed;
                body.setLinearVelocity(vx, 0f);
            } else {
                body.setLinearVelocity(0f, 0f);
                if (attackTimer >= attackCooldown) {
                    currentState = State.ATTACKING;
                    attackTimer = 0;
                    player.takeDamage(10);
                } else {
                    currentState = State.IDLE;
                }
            }
        } else {
            // Наземные враги (включая Mud Guard)
            if (distanceToSpawn > patrolRadius) {
                moveTo(spawnPoint);
                currentState = State.RUNNING;
            } else if (distanceToPlayer > visionRange) {
                patrol(delta);
                currentState = State.IDLE;
            } else if (distanceToPlayer > attackRange) {
                chasePlayer(playerPos);
                currentState = State.RUNNING;
            } else {
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (attackTimer >= attackCooldown) {
                    currentState = State.ATTACKING;
                    attackTimer = 0;
                    player.takeDamage(10);
                } else {
                    currentState = State.IDLE;
                }
            }
        }

        animManager.update(delta);
    }

    private void moveTo(Vector2 target) {
        tmp.set(target).sub(body.getPosition());
        if (tmp.isZero(0.0001f)) return;
        tmp.nor();
        body.setLinearVelocity(tmp.x * speed, body.getLinearVelocity().y);
    }

    private void patrol(float delta) {
        patrolTimer += delta;

        if (patrolTimer >= 2f) {
            movingRight = !movingRight;
            patrolTimer = 0;
        }

        float vx = movingRight ? speed * 0.5f : -speed * 0.5f;
        body.setLinearVelocity(vx, body.getLinearVelocity().y);
    }

    private void chasePlayer(Vector2 playerPos) {
        float dir = playerPos.x > body.getPosition().x ? 1 : -1;
        body.setLinearVelocity(dir * speed, body.getLinearVelocity().y);
    }

    private void patrolFlying() {
        Vector2 enemyPos = body.getPosition();

        if (enemyPos.x > spawnPoint.x + patrolRadius) movingRight = false;
        if (enemyPos.x < spawnPoint.x - patrolRadius) movingRight = true;

        float vx = movingRight ? speed * 0.7f : -speed * 0.7f;
        body.setLinearVelocity(vx, 0f);
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

            // Поворот в сторону движения
            if (body.getLinearVelocity().x < 0 && !frame.isFlipX()) {
                frame.flip(true, false);
            } else if (body.getLinearVelocity().x > 0 && frame.isFlipX()) {
                frame.flip(true, false);
            }

            float width, height;

            // Размеры для каждого типа врага (в метрах)
            if (type == EnemyType.ZAPPER) {
                width = 106f / PPM;  // 6.625 метров
                height = 22f / PPM;   // 1.375 метров
            } else if (type == EnemyType.WHEEL) {
                width = 112f / PPM;  // 7 метров
                height = 26f / PPM;   // 1.625 метров
            } else if (type == EnemyType.MUD_GUARD) {
                // Mud Guard: меняем местами для правильной ориентации
                width = 48f / PPM;   // 3 метра
                height = 24f / PPM;  // 1.5 метра
            } else if (type == EnemyType.STORMHEAD) {
                width = 119f / PPM;  // 7.44 метров
                height = 124f / PPM;  // 7.75 метров
            } else {
                width = 1f;
                height = 2f;
            }

            // Центрируем спрайт
            batch.draw(frame,
                pos.x - width/2,
                pos.y - height/2,
                width, height);
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
        // Очистка текстур
    }
}
