package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.tempus.obstacle.CapsuleObstacle;

public class Turret extends CapsuleObstacle {

    // This is to fit the image to a tighter hitbox
    /** The amount to shrink the body fixture (vertically) relative to the image */
    private static final float VSHRINK = 0.95f;
    /** The amount to shrink the body fixture (horizontally) relative to the image */
    private static final float HSHRINK = 0.7f;

    /** The number of frames until we can fire again */
    private int framesTillFire;
    /** How long the turret must wait until it can fire again */
    private int cooldown; // in ticks
    /** The number of frames until the turret can fire again */
    private boolean isActive;
    /** The velocity of the projectile that this turret fires */
    private Vector2 velocity;

    public Turret(float x, float y,float width, float height, int cooldown, Vector2 vel) {
        super(x,y,width*HSHRINK,height*VSHRINK);

        this.cooldown = cooldown;
        this.velocity = vel;
        isActive = true;
        framesTillFire = 0;
    }

    /**
     * Returns the velocity of the projectiles that this turret fires.
     *
     * @return velocity of the projectiles that this turret fires.
     */
    public Vector2 getVelocity() { return velocity; }

    /**
     * Returns whether or not turret can fire.
     *
     * @return whether or not turret can fire.
     */
    public boolean canFire() {
        return framesTillFire <= 0 && isActive;
    }

    /**
     * Reset or cool down the turret weapon.
     *
     * If flag is true, the turret will cool down by one animation frame.  Otherwise
     * it will reset to its maximum cooldown.
     *
     * @param flag whether to cooldown or reset
     */
    public void coolDown(boolean flag) {
        if (flag && framesTillFire > 0) {
            framesTillFire--;
        } else if (!flag) {
            framesTillFire = cooldown;
        }
    }
}
