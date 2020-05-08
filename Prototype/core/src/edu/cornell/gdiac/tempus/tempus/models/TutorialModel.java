package edu.cornell.gdiac.tempus.tempus.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import edu.cornell.gdiac.tempus.tempus.LevelController;
import edu.cornell.gdiac.tempus.tempus.TutorialController;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.util.HashMap;

public class TutorialModel extends LevelModel {

    private HashMap<Integer, String[]> tutorialCards;


    public TutorialModel(int lv, boolean unlocked, boolean finished, int resume, LevelController[] rms, HashMap<Integer, String[]> cards) {
        super(lv, unlocked, finished, resume, rms);

        int num_rooms = rms.length;
        tutorialCards = cards;

    }

    @Override
    public void createLevel() {
        for(int i = 0; i<rooms.length; i++) {
            TutorialController rc = (TutorialController) rooms[i];
            rc.loadContent();
            rc.setFirst(i==0);
            rc.setCard(tutorialCards.get(i));
            rc.setScreenListener(listener);
            rc.setCanvas(canvas);
        }
    }


}
