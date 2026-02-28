package com.github.aminferrr.MyJavaGame;


import com.badlogic.gdx.Game;
import com.github.aminferrr.MyJavaGame.screens.FirstScreen; // <- важный импорт

public class Main extends Game {

    @Override
    public void create() {
        setScreen(new FirstScreen(this));
    }
}
