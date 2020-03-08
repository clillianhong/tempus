package edu.cornell.gdiac.tempus.tempus.models;

import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;

public class Door extends BoxObstacle {

    private int NEXT_LEVEL;

    public Door(float x, float y, float width, float height, int next_level) {
        super(x, y, width, height);
        NEXT_LEVEL = next_level;
    }
}
