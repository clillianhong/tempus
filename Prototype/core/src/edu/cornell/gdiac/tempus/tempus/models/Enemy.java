package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.CapsuleObstacle;

public class Enemy extends CapsuleObstacle {

    // This is to fit the image to a tighter hitbox
    /** The amount to shrink the body fixture (vertically) relative to the image */
    private static final float VSHRINK = 0.95f;
    /** The amount to shrink the body fixture (horizontally) relative to the image */
    private static final float HSHRINK = 0.7f;

    /** Center of enemy position. */
    private EntityType type;

    // (x, y) is the initial position of the enemy
    public Enemy(EntityType type, float x, float y, float width, float height) {
        super(x, y,width*HSHRINK,height*VSHRINK);
        this.type = type;
    }

    /**
     * Returns the type of enemy.
     *
     * @return type of enemy.
     */
    public EntityType getType() { return type; }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = 1.0F;
        canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),effect,1.0f);
    }
}
