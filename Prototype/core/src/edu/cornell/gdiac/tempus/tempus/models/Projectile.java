package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.obstacle.WheelObstacle;

public class Projectile extends WheelObstacle {

    /** The type of this projectile */
    private EntityType type;


    public Projectile(EntityType type, float x, float y, float radius) {
        super(x, y, radius);
        this.type = type;
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
        if (texture != null) {
            canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1.0f,1.0f);
        }
    }
}
