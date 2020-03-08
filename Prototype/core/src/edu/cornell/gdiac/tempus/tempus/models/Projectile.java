package edu.cornell.gdiac.tempus.tempus.models;

import edu.cornell.gdiac.tempus.obstacle.WheelObstacle;

public class Projectile extends WheelObstacle {
    public Projectile(Turret origin, float x, float y, float radius) {
        super(x, y, radius);
    }
}
