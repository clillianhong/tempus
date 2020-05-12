package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.MusicController;
import edu.cornell.gdiac.tempus.WorldController;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.util.GifDecoder;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.ScreenListener;

import static edu.cornell.gdiac.tempus.WorldController.EXIT_NEXT;
import static edu.cornell.gdiac.tempus.WorldController.EXIT_QUIT;

public class MainMenuMode implements Screen {
    private SpriteBatch batch;
    protected Stage stage;
    private Viewport viewport;
    private OrthographicCamera camera;
    private TextureAtlas atlas;
    protected Skin skin;
    protected Rectangle bounds;
    /** Listener that will update the player mode when we are done */
    protected ScreenListener listener;
    private boolean active;
    /** background texture region */
    TextureRegion backgroundTexture;
    TextureRegion glow;
    Animation anim;
    float frameCounter;



    /** Reference to the game canvas */
    protected GameCanvas canvas;
    protected Vector2 scale;
    /** Width of the game world in Box2d units */
    protected static final float DEFAULT_WIDTH  = 32.0f;
    /** Height of the game world in Box2d units */
    protected static final float DEFAULT_HEIGHT = 18.0f;

    int sw = 1920/2;
    int sh = 1080/2;

    public MainMenuMode(){
        bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        canvas = null;
        scale = new Vector2();
        active = false;
        frameCounter = 0;
    }

    /* set the canvas for this mode */
    public void setCanvas(GameCanvas canvas) {
        this.canvas = canvas;
        this.scale.x = canvas.getWidth()/bounds.getWidth();
        this.scale.y = canvas.getHeight()/bounds.getHeight();
    }
    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }


//    float sw = WorldController;
//    float sh = Gdx.graphics.getHeight();

    public void createMode(){
//        atlas = new TextureAtlas("skin.atlas");
        skin = new Skin(Gdx.files.internal("skins/flat_earth_skin/flat-earth-ui.json"));
        camera = new OrthographicCamera(sw, sh);
        stage = new Stage(new FitViewport(sw, sh, camera));
        stage.getViewport().apply();
//        stage.getViewport().getCamera().position.setZero();

    }



    private void exitGame(){
        listener.exitScreen(this, ScreenExitCodes.EXIT_QUIT.ordinal());
    }

    private void exitLevelSelector(){
        listener.exitScreen(this, ScreenExitCodes.MENU_START.ordinal());
    }

    private void exitToHelpMenu(){ listener.exitScreen(this, ScreenExitCodes.MENU_HELP.ordinal()); }

    private void exitToAboutMenu(){ listener.exitScreen(this, ScreenExitCodes.MENU_ABOUT.ordinal()); }


    @Override
    public void show() {

        //Stage should controller input:
        Gdx.input.setInputProcessor(stage);
        active = true;

        float cw = sw * 0.7f;
        float ch = sh * 0.5f;

        backgroundTexture = new TextureRegion(new Texture(Gdx.files.internal("textures/background/mainmenubackground.png")));
        //table container to center main table
        Container<Table> tableContainer = new Container<Table>();
        tableContainer.setSize(cw, ch);
        tableContainer.setPosition((sw - cw) / 2.0f, (sh - ch)/4.0f);
        tableContainer.fillX();

        Table mainTable = new Table(skin);

        mainTable.setWidth(stage.getViewport().getScreenWidth());

        anim = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, Gdx.files.internal("textures/gui/tempus_logo_stationary.gif").read());
        glow = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/glow_logo.png")));
        //Create header

        //Create buttons
        TextureRegionDrawable startUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button_v3_up.png"))));
        TextureRegionDrawable helpUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button_v3_up.png"))));
        TextureRegionDrawable exitUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button_v3_up.png"))));
        TextureRegionDrawable aboutUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button_v3_up.png"))));

        final Button startButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button_v3.png")))),
                startUp, startUp);
        final Button helpButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button_v3.png")))),
                helpUp, helpUp);
        final Button exitButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button_v3.png")))),
                exitUp, exitUp);
        final Button aboutButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button_v3.png")))),
                aboutUp, aboutUp);
                
        //Add listeners to buttons
        startButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(Actions.fadeOut(1f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        exitLevelSelector();
                    }
                })));
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                startButton.setChecked(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                startButton.setChecked(false);
            }


        });
        helpButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(Actions.fadeOut(1f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        exitToHelpMenu();
                    }
                })));
            }


            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                helpButton.setChecked(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                helpButton.setChecked(false);
            }
        });
        aboutButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(Actions.fadeOut(1f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        exitToAboutMenu();
                    }
                })));
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                aboutButton.setChecked(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                aboutButton.setChecked(false);
            }

        });
        exitButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exitGame();
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                exitButton.setChecked(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                super.enter(event, x, y, pointer, fromActor);
                exitButton.setChecked(false);
            }

        });


        //add header
        mainTable.row();
        mainTable.row();
        mainTable.row().expandX().fillX();

        //add buttons

        mainTable.add(exitButton).width(cw/5f).height(ch/4f).pad(cw/15f).expand().fillX();
        mainTable.add(helpButton).width(cw/5f).height(ch/4f).pad(cw/15f).expand().fillX();
        mainTable.add(startButton).width(cw/5f).height(ch/4f).pad(cw/25f).expand().fillX();
        mainTable.add(aboutButton).width(cw/5f).height(ch/4f).pad(cw/15f).expand().fillX();
//        mainTable.add(aboutButton).width(cw/5f).height(ch/5f).pad(cw/15f).expand().fillX();

        tableContainer.setActor(mainTable);
        //Add table to stage
        //
        stage.addActor(tableContainer);
        stage.getViewport().apply();

        MusicController.getInstance().playMenuMusic();

    }


    @Override
    public void render(float delta) {

        if(active){
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            frameCounter += 6 * Gdx.graphics.getDeltaTime();
            float mult = 1.1f;
            float anti_mult = 0.9f;

            TextureRegion an = (TextureRegion) anim.getKeyFrame(frameCounter, true);

            stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
            stage.getCamera().update();

            stage.getBatch().begin();
            stage.getBatch().draw(backgroundTexture, 0, 0, sw, sh);
            stage.getBatch().draw(glow, sw/4*0.91f*anti_mult, sh/2*0.96f*anti_mult, sw/2*1.08f*mult, sh/3*1.04f*mult);
            stage.getBatch().draw(an,sw/4*anti_mult, sh/2*anti_mult, sw/2*mult, sh/3*mult);

            stage.getBatch().end();

            stage.act();
            stage.draw();
        }

    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
        stage.getCamera().viewportWidth = sw;
        stage.getCamera().viewportHeight = sh;
        stage.getCamera().position.set(stage.getCamera().viewportWidth / 2, stage.getCamera().viewportHeight / 2, 0);
        stage.getCamera().update();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        active = false;
        if (stage != null) {
            stage.clear();
        }
    }
}
