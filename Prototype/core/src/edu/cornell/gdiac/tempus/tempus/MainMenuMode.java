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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.tempus.GameCanvas;
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

    public MainMenuMode(){
        bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        canvas = null;
        scale = new Vector2();
        active = false;
        batch = new SpriteBatch();
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


    public void createMode(){
//        atlas = new TextureAtlas("skin.atlas");
        skin = new Skin(Gdx.files.internal("skins/flat_earth_skin/flat-earth-ui.json"));

        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(bounds.getWidth(),bounds.getHeight(), camera);
        viewport.apply();

        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();


        stage = new Stage(new ScreenViewport());
    }

    float sw;
    float sh;

    private void exitGame(){
        listener.exitScreen(this, ScreenExitCodes.EXIT_QUIT.ordinal());
    }

    private void exitLevelSelector(){
        listener.exitScreen(this, ScreenExitCodes.MENU_START.ordinal());
    }

    @Override
    public void show() {
        active = true;

        sw = Gdx.graphics.getWidth();
        sh = Gdx.graphics.getHeight();

        System.out.println("SW: " + sw);
        System.out.println("SH: " + sh);

        float row_height = sw / 12;
        float col_width = sh / 12;

        float cw = sw * 0.7f;
        float ch = sh * 0.5f;

        backgroundTexture = new TextureRegion(JsonAssetManager.getInstance().get("textures/background/bg_past_lv_1.jpg", Texture.class));

        //table container to center main table
        Container<Table> tableContainer = new Container<Table>();
        tableContainer.setSize(cw, ch);
        tableContainer.setPosition((sw - cw) / 2.0f, (sh - ch)/4.0f);
        tableContainer.fillX();

        //Stage should controller input:
        Gdx.input.setInputProcessor(stage);

        Table mainTable = new Table(skin);

        mainTable.setWidth(stage.getWidth());

        anim = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, Gdx.files.internal("textures/gui/tempus_logo_stationary.gif").read());
        glow = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/glow_logo.png")));
        //Create header

        //Create buttons
        Button startButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button.png")))));
//        startButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button.png"))));
//        startButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/start_button.png"))));
        Button exitButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button.png")))));
//        exitButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button.png"))));
//        exitButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/quit_button.png"))));
        Button helpButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button.png")))));
//        helpButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button.png"))));
//        helpButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/help_button.png"))));
        Button aboutButton = new Button(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button.png")))));
//        aboutButton.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button.png"))));
//        aboutButton.getStyle().imageDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/about_button.png"))));

        //Add listeners to buttons
        startButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exitLevelSelector();
            }
        });
        exitButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exitGame();
            }
        });


        //add header
        mainTable.row();
        mainTable.row();
        mainTable.row().expandX().fillX();

        //add buttons
        mainTable.add(startButton).width(cw/5f).height(ch/5f).pad(cw/20f).expand().fillX();
        mainTable.add(exitButton).width(cw/5f).height(ch/5f).pad(cw/15f).expand().fillX();
        mainTable.add(helpButton).width(cw/5f).height(ch/5f).pad(cw/15f).expand().fillX();
        mainTable.add(aboutButton).width(cw/5f).height(ch/5f).pad(cw/15f).expand().fillX();


        tableContainer.setActor(mainTable);
        //Add table to stage
        stage.addActor(tableContainer);
    }


    @Override
    public void render(float delta) {
        if(active){
            Gdx.gl.glClearColor(.1f, .12f, .16f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            canvas.clear();
            canvas.begin();

            canvas.draw(backgroundTexture, Color.WHITE, 0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.end();

            stage.act();
            stage.draw();


        }
        frameCounter += 5 * Gdx.graphics.getDeltaTime();

        TextureRegion an = (TextureRegion) anim.getKeyFrame(frameCounter, true);
        batch.begin();
        batch.draw(glow, sw/4*0.94f, sh/2*0.98f, sw/2*1.08f, sh/3*1.04f);
        batch.draw(an,sw/4, sh/2, sw/2, sh/3);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {

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

    }
}
