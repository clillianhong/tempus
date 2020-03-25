package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.tempus.obstacle.BoxObstacle;

public class Door extends BoxObstacle {

    private int NEXT_LEVEL;
    private TextureRegion goalTile;
    private Vector2 scale;
    /** The goal door position */
    private static Vector2 GOAL_POS = new Vector2(29.5f,15.5f);


    public Door(float x, float y, float width, float height, int next_level) {
        super(x, y, width, height);
        NEXT_LEVEL = next_level;
    }
    public Door() {
        super(0,0,1,1);
    }
    public void initialize(){

    }
//    public void initialize(){
//        setName
//    }
}
