package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class TiledPhysicsBuilder {

    private World world;
    private float PPM;

    public TiledPhysicsBuilder(World world, float PPM) {
        this.world = world;
        this.PPM = PPM;
    }

    public void buildCollisions(MapLayer collisionLayer) {
        if (collisionLayer == null) {
            System.out.println("Слой коллизий не найден!");
            return;
        }

        // Проходим по всем объектам в слое
        for (MapObject object : collisionLayer.getObjects()) {
            // Создаем физическое тело для каждого объекта коллизии
            createCollisionBody(object);
        }
    }

    private void createCollisionBody(MapObject object) {
        // Создаем тело для коллизии (статическое - недвижимое)
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        // Проверяем тип объекта и создаем соответствующую форму
        if (object instanceof RectangleMapObject) {
            RectangleMapObject rectObject = (RectangleMapObject) object;
            Rectangle rect = rectObject.getRectangle();

            // Устанавливаем позицию тела в центр прямоугольника
            bodyDef.position.set(
                (rect.x + rect.width / 2) / PPM,
                (rect.y + rect.height / 2) / PPM
            );

            Body body = world.createBody(bodyDef);

            // Создаем форму прямоугольника
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(
                rect.width / 2 / PPM,
                rect.height / 2 / PPM
            );

            // Создаем фикстуру
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.friction = 0.5f;
            fixtureDef.restitution = 0.1f;

            body.createFixture(fixtureDef);
            body.setUserData("collision"); // Помечаем, что это стена/пол

            shape.dispose();

        } else if (object instanceof PolygonMapObject) {
            PolygonMapObject polygonObject = (PolygonMapObject) object;
            float[] vertices = polygonObject.getPolygon().getTransformedVertices();

            // Конвертируем вершины в метры
            Vector2[] worldVertices = new Vector2[vertices.length / 2];
            for (int i = 0; i < worldVertices.length; i++) {
                worldVertices[i] = new Vector2(
                    vertices[i * 2] / PPM,
                    vertices[i * 2 + 1] / PPM
                );
            }

            // Устанавливаем позицию тела
            bodyDef.position.set(0, 0); // Позиция уже учтена в вершинах
            Body body = world.createBody(bodyDef);

            // Создаем форму полигона
            PolygonShape shape = new PolygonShape();
            shape.set(worldVertices);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.friction = 0.5f;

            body.createFixture(fixtureDef);
            body.setUserData("collision");

            shape.dispose();
        }
    }
}
