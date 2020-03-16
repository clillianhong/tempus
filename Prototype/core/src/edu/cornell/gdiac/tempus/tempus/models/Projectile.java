package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.tempus.obstacle.WheelObstacle;

public class Projectile extends WheelObstacle {

    /** The type of this projectile. */
    private EntityType type;

    public Projectile(EntityType type, Turret origin, float x, float y, float radius) {
        super(x, y, radius);
        this.type = type;
    }

    /**
     * Returns the type of projectile.
     *
     * @return type of projectile.
     */
    private EntityType getType() { return type; }
}
