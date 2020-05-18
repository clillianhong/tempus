package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.WheelObstacle;

public class Projectile extends WheelObstacle {

    /** The type of this projectile */
    private EntityType type;

    private float rotation;

    private Object sourceData;

    private boolean slowed;


    public Projectile(EntityType type, float x, float y, float radius, Object sourceData) {
        super(x, y, radius);
        this.type = type;
        rotation = 0;
        this.sourceData = sourceData;
        this.setSensor(true);
        slowed = false;
    }

    public Object getSourceData() {
        return sourceData;
    }

    public void slow(boolean slow){
        slowed = slow;
    }

    /**
     * Returns the type of projectile.
     *
     * @return type of projectile.
     */
    public EntityType getType() { return type; }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        if (rotation == 361){
            rotation = 0;
        }
        if (texture != null) {
            if (getLinearVelocity().x >= 0) {
                canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, -1 * rotation, 0.005f * drawScale.x, 0.005f * drawScale.y);
            } else {
                canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, rotation, 0.005f * drawScale.x, 0.005f * drawScale.y);
            }
        }
        if (!slowed) {
            if (type.equals(EntityType.PRESENT)) {
                rotation += 0.04f;
            } else {
                rotation += 1.0f;
            }
        } else {
            if (type.equals(EntityType.PRESENT)) {
                rotation += 0.01f;
            } else {
                rotation += 0.25f;
            }
        }
    }
}
