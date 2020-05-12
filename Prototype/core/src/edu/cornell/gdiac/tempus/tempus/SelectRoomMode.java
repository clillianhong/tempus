package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
<<<<<<< HEAD
import com.badlogic.gdx.utils.Json;
=======
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.tempus.models.LevelModel;
import edu.cornell.gdiac.tempus.tempus.models.ScreenExitCodes;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.GameStateManager;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.ScreenListener;

public class SelectRoomMode implements Screen {

    /** LISTENERS EVENTS TO CHANGE SCREEN **/
    private void exitBack(){
        listener.exitScreen(this, ScreenExitCodes.EXIT_PREV.ordinal());
    }
    private void exitToRoom(){ listener.exitScreen(this, ScreenExitCodes.ROOM_SELECT.ordinal());}

    protected Stage stage;
    private Viewport viewport;
    private OrthographicCamera camera;
    /** Fade in stage */
    protected Stage fadeinStage;
    /** count for fading into the screen **/
    protected int fadeincount;

    protected Rectangle bounds;
    /** Listener that will update the player mode when we are done */
    protected ScreenListener listener;
    private boolean active;

    //TEXTURES
    /** background texture region */
    private TextureRegion backgroundTexture;

    int sw = 1920/2;
    int sh = 1080/2;

    /** Reference to the game canvas */
    protected GameCanvas canvas;
    protected Vector2 scale;
    /** Width of the game world in Box2d units */
    protected static final float DEFAULT_WIDTH  = 32.0f;
    /** Height of the game world in Box2d units */
    protected static final float DEFAULT_HEIGHT = 18.0f;

    //SCENE2D COMPONENTS
    private Table wholescreenTable;
    private Table[]  roomTables;
    private Container roomContainer;
    private Button backButton;
    private Button prevRoomButton;
    private Button nextRoomButton;
    TextureRegionDrawable prevTexOff;
    TextureRegionDrawable prevTexOn;
    TextureRegionDrawable nextTexOff;
    TextureRegionDrawable nextTexOn;

    //level info
    int levelNum;
    int numRooms;
    int numPages;
    int curPage;

    /** Font */
    private BitmapFont font;
    /** Glyph layout for on the fly generation */
    private static GlyphLayout glyphLayout;



    public SelectRoomMode(int level){
        bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        canvas = null;
        scale = new Vector2();
        active = true;

        camera = new OrthographicCamera(sw,sh);
        viewport = new FitViewport(sw, sh, camera);
        viewport.apply();

        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();

        stage = new Stage(viewport);

        levelNum = level;
        numRooms = GameStateManager.getInstance().getLevel(level).getRoomCount();

        font = new BitmapFont(Gdx.files.internal("fonts/carterone.fnt"));
        glyphLayout  = new GlyphLayout();

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
        camera.update();
        stage = new Stage(viewport);
        fadeinStage = new Stage(viewport);
        fadeincount = 20;



    }


    @Override
    public void show() {
        fadeincount = 20;
        Gdx.input.setInputProcessor(stage);

        final GameStateManager gameManager = GameStateManager.getInstance();
<<<<<<< HEAD
        JsonAssetManager assetManager = JsonAssetManager.getInstance();
=======
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13

        wholescreenTable = new Table();
        wholescreenTable.setWidth(sw);
        wholescreenTable.setHeight(sh);

        numPages = numRooms % 4 == 0 ? numRooms/4 : (numRooms/4) + 1;
        roomTables = new Table[numPages];
        curPage = 0;

        //fade in hack
        Container fadeCont = new Container();
        fadeCont.setSize(sw, sh);
        fadeCont.setPosition(0, 0);
        fadeCont.fillX();
        fadeCont.fillY();
        TextureRegionDrawable fadeInBG = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("tutorial/black_bg.jpg"))));
        fadeCont.setBackground(fadeInBG);
        fadeinStage.addActor(fadeCont);


        int rowNum;
        int colNum;

        if(numRooms > 20){
            rowNum = 6;
            colNum = 7;
        }else{
            rowNum = 4;
            colNum = 5;
        }

        float cw = sw * 0.9f;
        float ch = sh * 0.5f;
<<<<<<< HEAD
        int pad = 20;
=======
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13

        font.getData().setScale(0.75f);
        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        FilmStrip bg = JsonAssetManager.getInstance().getEntry("level"+levelNum+"_bg", FilmStrip.class);
//        backgroundTexture = new TextureRegion(bg.getTexture());
<<<<<<< HEAD
        backgroundTexture = assetManager.getEntry("room_select_bg", TextureRegion.class);

//        Container whiteBorderContainer = new Container();
//        whiteBorderContainer.setActor(whiteBorder);
        TextureRegionDrawable lockedRoom = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/locked_room.png"))));

=======
        backgroundTexture = new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/room_select.png")));
        TextureRegionDrawable overlayBG = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_filter_50_black.png"))));

        TextureRegionDrawable lockedRoom = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/locked_room.png"))));

//        wholescreenTable.setBackground(overlayBG);
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
        int wid = bg.getTexture().getWidth();
        int ht = bg.getTexture().getHeight();
        final int highestRoom = gameManager.getCurrentLevel().getHighestUnlockedRoom();

        //load all room buttons
        int row = 0;
<<<<<<< HEAD
=======
        System.out.println("width: " + wid);
        System.out.println("height: " + ht);
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
        boolean finishPage = false;
        for(int page = 0; page < numPages; page++){
            //create four buttons and four labels
            roomTables[page] = new Table();
            for(int f = 0; f<4; f++){
                final int roomNum = page*4 + f;
                if(roomNum >= numRooms){
                    finishPage = true;
                    break;
                }
<<<<<<< HEAD
=======
                System.out.println("x,y: (" + (wid/colNum)*f + "," + (ht/rowNum)*row + ")");
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
                TextureRegionDrawable roomPreview;
                TextureRegionDrawable roomPreviewDown;
                if(roomNum <= highestRoom){
                    roomPreview = new TextureRegionDrawable(new TextureRegion(bg, (wid/colNum)*f, (ht/rowNum)*row, wid/colNum, ht/rowNum));
                    roomPreviewDown = roomPreview;
                    roomPreviewDown.tint(Color.BLACK);
                }else{
                    roomPreview = lockedRoom;
                    roomPreviewDown = lockedRoom;
                }

<<<<<<< HEAD

                Button tempButton = new Button(roomPreview, roomPreviewDown);
//                tempButton.addListener(new ClickListener(){
//                    @Override
//                    public void clicked(InputEvent event, float x, float y) {
//                        if(roomNum <= highestRoom){
//                            gameManager.setCurrentLevel(levelNum, roomNum);
//                            gameManager.getCurrentLevel().setCurrentRoom(roomNum);
//                            gameManager.printGameState();
//                            exitToRoom();
////                            stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
////                                @Override
////                                public void run() {
////
////                                }
////                            })));
//                        }
//                    }
//                });
//                Image whiteBorder = new Image(assetManager.getEntry("white_border", TextureRegion.class));
                TextureRegionDrawable whiteOverlayBorder = new TextureRegionDrawable(assetManager.getEntry("white_border_light", TextureRegion.class));

                final Button whiteBorder = new Button(new TextureRegionDrawable(assetManager.getEntry("white_border", TextureRegion.class)),
                        whiteOverlayBorder,whiteOverlayBorder);
//                final Button whiteBorder = new Button(whiteOverlayBorder,
//                        whiteOverlayBorder,whiteOverlayBorder);
                whiteBorder.addListener(new ClickListener(){
=======
                Container butCon = new Container();
                Button tempButton = new Button(roomPreview, roomPreviewDown);
                tempButton.addListener(new ClickListener(){
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if(roomNum <= highestRoom){
                            gameManager.setCurrentLevel(levelNum, roomNum);
                            gameManager.getCurrentLevel().setCurrentRoom(roomNum);
                            gameManager.printGameState();
                            exitToRoom();
<<<<<<< HEAD
                        }
                    }

                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                        super.enter(event, x, y, pointer, fromActor);
                        whiteBorder.setChecked(true);
                    }
                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                    if(!(scrolling)){
                        super.enter(event, x, y, pointer, fromActor);
                        whiteBorder.setChecked(false);
                    }

                });
                Container whiteBorderContainer = new Container();
                whiteBorderContainer.setActor(whiteBorder);
                whiteBorderContainer.size(cw/4-pad*2,ch*0.8f-pad*2);

                Table buttonstackTable = new Table();
                buttonstackTable.add(tempButton).width(cw/4-pad*2-15).height(ch*0.8f-pad*2-15);
                Stack buttonStack = new Stack();
                buttonStack.add(buttonstackTable);
                buttonStack.add(whiteBorderContainer);

                roomTables[page].add(buttonStack).width(cw/4-pad*2).height(ch*0.8f-pad*2).padLeft(pad).padRight(pad).padTop(pad).expand().center();
=======
//                            stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                }
//                            })));
                        }
                    }
                });
                butCon.setActor(tempButton);

                int pad = 20;
                roomTables[page].add(tempButton).width(cw/4-pad*2).height(ch*0.8f-pad*2).padLeft(pad).padRight(pad).padTop(pad).expand().center();
>>>>>>> e9fc9033905eda29e1f3f9999c2216575bd7fc13
            }
            roomTables[page].row();

            row+=1;

            for(int f = 0; f<4; f++){
                final int roomNum = page*4 + f;
                if(roomNum >= numRooms){
                    finishPage = true;
                    break;
                }
                String roomLabel = "Room " + (roomNum+1);
                Label tempLabel = new Label(roomLabel, style);
                tempLabel.setAlignment(Align.center);
                roomTables[page].add(tempLabel).width(cw/4).height(ch*0.2f).center();
            }

            if(finishPage){
                break;
            }

        }

        roomContainer = new Container();
        roomContainer.setActor(roomTables[curPage]);

        prevTexOn = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/prev_arrow.png"))));
        prevTexOff = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/prev_arrow_off.png"))));

        nextTexOn = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/next_arrow.png"))));
        nextTexOff = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/roomselect/next_arrow_off.png"))));

        Table overlayBackButton = new Table();
        //back button
        TextureRegionDrawable bup = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/selectmode/backbutton.png"))));
        Button backButton = new Button(bup);
        backButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        exitBack();
                    }
                })));
            }
        });
        overlayBackButton.add(backButton).width(cw/12f).height(cw/15f).expand().bottom().left();

        nextRoomButton = new Button(nextTexOn,nextTexOn,nextTexOff);
        nextRoomButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {

                roomTables[curPage].act(Gdx.graphics.getDeltaTime());
                curPage = Math.min(curPage+1, numPages-1);
                roomContainer.setActor(roomTables[curPage]);
                updateArrows();
            }
        });


        prevRoomButton = new Button(prevTexOn,prevTexOn,prevTexOff);
        prevRoomButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
              //go previous
                curPage = Math.max(0,(curPage - 1));
                roomContainer.setActor(roomTables[curPage]);
                updateArrows();
            }
        });
        prevRoomButton.setChecked(true);

////        Table backCont = new Table();
//        Container backCont = new Container();
//        backCont.setActor(backButton);
//        backCont.setWidth(cw/20);
        overlayBackButton.align(Align.left);
//        backCont.align(Align.left);
        Container prevCont = new Container();
        prevCont.setActor(prevRoomButton);
        prevCont.setWidth(cw/15);
        prevCont.align(Align.right);
        Container nextCont = new Container();
        nextCont.setActor(nextRoomButton);
        nextCont.setWidth(cw/15);
        nextCont.align(Align.left);
//        backCont.add(backButton).width(cw/13f).height(cw/15f).expand().bottom().left();
        roomContainer.align(Align.center);
        wholescreenTable.setPosition(0,0);
        wholescreenTable.add(roomContainer).padLeft((sw-cw)/2).padRight((sw-cw)/2).padTop((sh-ch)/2).padBottom((sh-ch)/4).center().colspan(3);
        wholescreenTable.row().height((sh-ch)/4);
        wholescreenTable.add(overlayBackButton).width(sw/4);
        wholescreenTable.add(prevCont).width(sw/4).right();
        wholescreenTable.add(nextCont).width(sw/2).left();

        stage.addActor(wholescreenTable);
//        stage.addActor();

        fadeinStage.addAction(Actions.alpha(1f));
        fadeinStage.addAction(Actions.fadeIn(1f));
    }

    public void updateArrows(){
        if(curPage==numPages-1){
            nextRoomButton.setChecked(true);
        }else{
            nextRoomButton.setChecked(false);
        }
        if(curPage==0){
            prevRoomButton.setChecked(true);
        }else{
            prevRoomButton.setChecked(false);
        }

    }

    @Override
    public void render(float delta) {
        if(fadeincount > 0){
            fadeincount--;
        }

        if(active){
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);;

            stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
            stage.getCamera().update();

            stage.getBatch().begin();
            stage.getBatch().draw(backgroundTexture, 0, 0, sw, sh);
            stage.getBatch().end();

            stage.act();
            stage.draw();

            fadeinStage.getBatch().setProjectionMatrix(stage.getCamera().combined);
            fadeinStage.getCamera().update();
            fadeinStage.getViewport().apply();

            if(fadeincount>0){
                fadeinStage.draw();
            }

        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
        stage.getCamera().viewportWidth = sw;
        stage.getCamera().viewportHeight = sh;
        stage.getCamera().position.set(stage.getCamera().viewportWidth / 2, stage.getCamera().viewportHeight / 2, 0);
        stage.getCamera().update();
        fadeinStage.getCamera().update();
        fadeinStage.getViewport().apply();
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
        stage.clear();
    }

}
