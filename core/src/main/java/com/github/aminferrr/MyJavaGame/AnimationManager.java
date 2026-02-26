package com.github.aminferrr.MyJavaGame;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AnimationManager {
    private Animation<TextureRegion> currentAnimation;
    private float stateTime;
    private boolean looping = true;

    public void update(float delta) {
        stateTime += delta;
    }

    public TextureRegion getFrame() {
        return currentAnimation.getKeyFrame(stateTime, looping);
    }

    public void setAnimation(Animation<TextureRegion> animation, boolean looping) {
        if (currentAnimation != animation) {
            this.currentAnimation = animation;
            this.looping = looping;
            this.stateTime = 0;
        }
    }

    public boolean isAnimationFinished() {
        return currentAnimation.isAnimationFinished(stateTime);
    }
}
