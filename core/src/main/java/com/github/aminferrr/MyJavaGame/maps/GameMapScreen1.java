package com.github.aminferrr.MyJavaGame.maps;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class GameMapScreen1 {

    private TiledMap map;
    private TiledMapTileLayer builds;

    private static final String MAP_PATH = "maps/MainMap/map..tmx";

    // Конструктор должен совпадать с именем класса
    public GameMapScreen1() {
        loadMap();
    }

    private void loadMap() {
        map = new TmxMapLoader().load(MAP_PATH);
        builds = getTileLayer(map, "builds");
    }

    private TiledMapTileLayer getTileLayer(TiledMap map, String name) {
        MapLayer layer = map.getLayers().get(name);
        if (layer instanceof TiledMapTileLayer) return (TiledMapTileLayer) layer;
        return null;
    }

    public TiledMap getMap() {
        return map;
    }

    public TiledMapTileLayer getBuildsLayer() {
        return builds;
    }

    public void dispose() {
        if (map != null) map.dispose();
    }
}
