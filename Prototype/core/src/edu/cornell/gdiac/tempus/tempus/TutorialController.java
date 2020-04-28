package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.tempus.models.Avatar;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.SoundController;

//import javax.xml.soap.Text;

public class TutorialController extends LevelController {


    private TextureRegionDrawable tutorial_card;
    private boolean first;
    private TextureRegionDrawable press_h_card;
    Table helpCard;
    private int beginDisplay;
    private Table tutorialCard;
    private boolean isHelp;
    /**
     * Creates and initialize a new instance of the tutorial level
     *
     * The game has default gravity and other settings
     *
     * @param json
     */
    public TutorialController(String json) {
        super(json);
        isHelp = false;
        beginDisplay = 150;
        isTutorial = true;
    }

    public void setCard(TextureRegionDrawable card){
        tutorial_card = card;
    }
    public void setFirst(boolean b) {first = b;};
    @Override
    public void preLoadContent() {
        super.preLoadContent();
    }

    @Override
    public void loadContent() {
        super.loadContent();
    }

    @Override
    public void createUI() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // table container to center main table
        Container<Stack> edgeContainer = new Container<Stack>();
        edgeContainer.setSize(sw, sh);
        edgeContainer.setPosition(0, 0);
        edgeContainer.fillX();
        edgeContainer.fillY();

        Stack tableStack = new Stack();

        /*
         * START PAUSE SCREEN SETUP ---------------------
         */
        TextureRegionDrawable pauseButtonResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pausebutton.png"))));
        TextureRegionDrawable pauseBG = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50_black.png"))));
        TextureRegionDrawable pauseBox = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/frame_pause.png"))));
        TextureRegionDrawable resumeResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_resume_button.png"))));
        TextureRegionDrawable restartResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_restart_button.png"))));
        TextureRegionDrawable exitResource = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_exit_button.png"))));


        press_h_card = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("tutorial/helpcard.png"))));

        pauseButtonContainer = new Container<>();
        pauseButtonContainer.setBackground(pauseBG);
        pauseButtonContainer.setPosition(0, 0);
        pauseButtonContainer.fillX();
        pauseButtonContainer.fillY();

        pauseTable = new Table();
        //pauseTable.background(pauseBox);
        pauseButtonContainer.setActor(pauseTable);
        pauseButtonContainer.setVisible(false);

        Button resumeButton = new Button(resumeResource);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                unpauseGame();
            }
        });

        Button restartButton = new Button(restartResource);
        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                reset();
                unpauseGame();
            }
        });

        Button exitButton = new Button(exitResource);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                exitLevelSelect();
            }
        });
        pauseTable.add(resumeButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).center().expandX().padBottom(sh / 20);
        pauseTable.row();
        pauseTable.add(restartButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).center().expandX().padBottom(sh / 20);
        ;
        pauseTable.row();
        pauseTable.add(exitButton).width(sw / 4 / 1.5f).height(sh / 5.1f / 1.5f).expandX();

        tutorialCard = new Table();
        tutorialCard.setBackground(pauseBG);
        tutorialCard.setPosition(0, 0);
        tutorialCard.setBackground(tutorial_card);
        tutorialCard.setVisible(false);

        helpCard = new Table();
        if(first){
            helpCard.setBackground(press_h_card);
        }
        tableStack.add(table);
        tableStack.add(pauseButtonContainer);
        tableStack.add(tutorialCard);
        helpCard.setVisible(true);
        tableStack.add(helpCard);
        edgeContainer.setActor(tableStack);
        /*
         * END PAUSE SCREEN SETUP---------------------
         */
        TextureRegion rippleBG = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50.png")));
        sprite = new Sprite(rippleBG);

        stage.addActor(edgeContainer);
        Gdx.input.setInputProcessor(stage);

        if(first){
            showTutorial();
        }

    }

    public void showTutorial(){
        tutorialCard.setVisible(true);
    }

    public void unshowTutorial(){
        tutorialCard.setVisible(false);
    }

    @Override
    public boolean preUpdate(float dt) {
        failed = false;

        if(first){
            if(beginDisplay >0){
                beginDisplay--;
                return false;
            }else {
                helpCard.setVisible(false);
            }
        }

        if(InputController.getInstance().didHelp()){
            isHelp = !isHelp;
        }
        return super.preUpdate(dt);
    }

    @Override
    public void update(float dt) {

        if (isHelp) {
            showTutorial();
        } else {
            unshowTutorial();
        }

        avatar.setLives(5);

        super.update(dt);

    }



}
