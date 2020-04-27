package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import javax.xml.soap.Text;

public class TutorialController extends LevelController {


    private TextureRegionDrawable tutorial_card;
    /**
     * Creates and initialize a new instance of the tutorial level
     *
     * The game has default gravity and other settings
     *
     * @param json
     */
    public TutorialController(String json) {
        super(json);

    }

    public void setCard(TextureRegionDrawable card){
        tutorial_card = card;
    }

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

        final Button pauseButton = new Button(pauseButtonResource);
        pauseButton.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!paused) {
                    pauseGame();
                }
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                prepause = true;
                pauseButton.setChecked(true);

            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);

                prepause = false;
                pauseButton.setChecked(false);
            }
        });

        pauseButtonContainer = new Container<>();
        pauseButtonContainer.setBackground(pauseBG);
        pauseButtonContainer.setPosition(0, 0);
        pauseButtonContainer.fillX();
        pauseButtonContainer.fillY();
        table.add(pauseButton).width(sw / 15f).height(sw / 15f).expand().right().top();

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

        tableStack.add(table);
        tableStack.add(pauseButtonContainer);
        edgeContainer.setActor(tableStack);
        /*
         * END PAUSE SCREEN SETUP---------------------
         */
        TextureRegion rippleBG = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50.png")));
        sprite = new Sprite(rippleBG);

        stage.addActor(edgeContainer);
        Gdx.input.setInputProcessor(stage);

    }

    @Override
    public void pauseGame() {
        super.pauseGame();
    }

    @Override
    public void unpauseGame() {
        super.unpauseGame();
    }

    @Override
    public void update(float dt) {
        super.update(dt);
    }
}
