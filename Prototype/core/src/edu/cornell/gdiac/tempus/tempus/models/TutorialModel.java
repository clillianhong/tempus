package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.tempus.tempus.TutorialController;

public class TutorialModel extends LevelModel {

    private TextureRegionDrawable [] tutorials;


    public TutorialModel(int lv, boolean unlocked, boolean finished, int resume, LevelController[] rms) {
        super(lv, unlocked, finished, resume, rms);

        int num_rooms = rms.length;
        tutorials = new TextureRegionDrawable[num_rooms];

        for(int i = 0; i<num_rooms; i++){
            String filename = "tutorial" + (i+1) + ".png";
            tutorials[i] = new TextureRegionDrawable(
                    new TextureRegion(new Texture(Gdx.files.internal("tutorial/" + filename))));
        }
    }

    @Override
    public void createLevel() {
        for(int i = 0; i<rooms.length; i++) {
            TutorialController rc = (TutorialController) rooms[i];
            rc.loadContent();
            rc.setCard(tutorials[i]);
            rc.setScreenListener(listener);
            rc.setCanvas(canvas);
        }
    }


}
