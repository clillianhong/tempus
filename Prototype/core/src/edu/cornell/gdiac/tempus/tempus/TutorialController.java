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

import javax.xml.soap.Text;

public class TutorialController extends LevelController {


    private TextureRegionDrawable tutorial_card;
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
        beginDisplay = 0;
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


        press_h_card = new TextureRegionDrawable(
                new TextureRegion(new Texture(Gdx.files.internal("textures/gui/pause_exit_button.png"))));


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
        helpCard.setBackground(press_h_card);
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

        showTutorial();

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
        if(beginDisplay >0){
            beginDisplay--;
            return false;
        }else{
            helpCard.setVisible(false);
        }
        if(InputController.getInstance().didHelp()){
            isHelp = !isHelp;
        }
        return super.preUpdate(dt);
    }

    @Override
    public void update(float dt) {


        if(isHelp){
            showTutorial();
        }
        else{
            unshowTutorial();
        }

        if(InputController.getInstance().didPause() && !paused){
            pauseGame();
        }

        if(rippleOn){
            updateShader();
        }

        // test slow down time
        if (timeFreeze) {
            world.step(WORLD_STEP / 8, WORLD_VELOC, WORLD_POSIT);

            enemyController.slowCoolDown(true);

        } else {
            world.step(WORLD_STEP, WORLD_VELOC, WORLD_POSIT);

            enemyController.slowCoolDown(false);
        }
        int t = avatar.getStartedDashing();
        if (t > 0) {
            t = t - 1;
            avatar.setStartedDashing(t);
        }
        // System.out.println(numEnemies);
        if (numEnemies == 0) {
            goalDoor.setOpen(true);
        } else {
            goalDoor.setOpen(false);
        }

        float presVol = present_music.getVolume();
        float pastVol = past_music.getVolume();
        if (shifted) {
            if (presVol > 0){
                present_music.setVolume(presVol - 0.1f);
            }
            if (pastVol < 1){
                past_music.setVolume(pastVol + 0.1f);
            }
        } else {
            if (presVol < 1){
                present_music.setVolume(presVol + 0.1f);
            }
            if (pastVol > 0){
                past_music.setVolume(pastVol - 0.1f);
            }
        }

        if (avatar.isHolding()) {
            timeFreeze = true;
            avatar.resetDashNum(1);
            if (avatar.getBodyType() != BodyDef.BodyType.StaticBody) {
                avatar.setBodyType(BodyDef.BodyType.StaticBody);
            } else if (InputController.getInstance().releasedRightMouseButton()) {
                timeFreeze = false;
                Vector2 mousePos = InputController.getInstance().getMousePosition();
                avatar.setBodyType(BodyDef.BodyType.DynamicBody);
                avatar.setSticking(false);
                avatar.setWasSticking(false);
                avatar.setDashing(true);
                avatar.setDashStartPos(avatar.getPosition().cpy());
                avatar.setDashDistance(avatar.getDashRange());
                // avatar.setDashDistance(Math.min(avatar.getDashRange(),
                // avatar.getPosition().dst(mousePos)));
                avatar.setDashForceDirection(mousePos.cpy().sub(avatar.getPosition()));
                avatar.setHolding(false);
                avatar.setCurrentPlatform(null);
                createRedirectedProj();
                avatar.setHeldBullet(null); // NOTE: gives error if called before createRedirectedProj()

            }
        }
        if (InputController.getInstance().pressedShiftKey()) {
            //update ripple shader params

            rippleOn = true;
            ticks=0;
            m_rippleDistance = 0;
            m_rippleRange = 0;

            if (avatar.isSticking()) {
                avatar.resetDashNum(-1);
            }
            shifted = !shifted;
            // avatar.resetDashNum();
            /*
             * if (!avatar.isHolding()) { avatar.setPosition(avatar.getPosition().x,
             * avatar.getPosition().y + 0.0001f * Gdx.graphics.getWidth()); }
             */
            if (avatar.getCurrentPlatform() != null) {
                if (avatar.isSticking()) {
                    if (!shifted && (avatar.getCurrentPlatform().getSpace() == 2)) { // past world
                        avatar.setSticking(false);
                        avatar.setWasSticking(false);
                        avatar.setBodyType(BodyDef.BodyType.DynamicBody);
                    } else if (shifted && (avatar.getCurrentPlatform().getSpace() == 1)) { // present world
                        avatar.setSticking(false);
                        avatar.setWasSticking(false);
                        avatar.setBodyType(BodyDef.BodyType.DynamicBody);
                    }
                }
            }

            enemyController.shift();
        }
        // Check if the platform is in this world or other world. If in the other world,
        // make the platform sleep.
        sleepIfNotInWorld();
        if (InputController.getInstance().didDebug()) {
            debug = !debug;
        }

        // prototype: Dash
        boolean dashAttempt = InputController.getInstance().releasedLeftMouseButton();
        if (dashAttempt) {
            avatar.dash(); // handles checking if dashing is possible
        }

        if(rippleOn){
            float rippleSpeed = 0.25f;
            float maxRippleDistance = 8f;
            ticks += time_incr;
            if(ticks > ripple_reset){
                rippleOn = false;
                ticks=0;
                m_rippleDistance = 0;
                m_rippleRange = 0;
            }
            m_rippleDistance += rippleSpeed * ticks;
            m_rippleRange = (1 - m_rippleDistance / maxRippleDistance) * 0.02f;
            updateShader();

        }

        // Process actions in object model
        avatar.setJumping(InputController.getInstance().didPrimary());
        avatar.setShooting(InputController.getInstance().didSecondary());

        // Sets which direction the avatar is facing (left or right)
        if (InputController.getInstance().pressedLeftMouseButton()) {
            Vector2 mousePos = InputController.getInstance().getMousePosition();
            Vector2 avatarPos = avatar.getPosition().cpy();
            avatar.setMovement(mousePos.x - avatarPos.x);
        }

        enemyController.processAction();

        avatar.applyForce();
        // enemy.applyForce();

        // Update animation state
        if (InputController.getInstance().pressedLeftMouseButton()
                || InputController.getInstance().pressedRightMouseButton()) {
            // If either mouse button is held, set animation to be crouching
            avatar.animate(Avatar.AvatarState.CROUCHING, false);
        } else if (avatar.isSticking()) {
            // Default animation if player is stationary
            avatar.animate(Avatar.AvatarState.STANDING, false);
        } else if (avatar.getLinearVelocity().y > 0) {
            avatar.animate(Avatar.AvatarState.DASHING, false);
        } else {
            avatar.animate(Avatar.AvatarState.FALLING, false);
        }

        if (avatar.isJumping()) {
            JsonValue data = assetDirectory.get("sounds").get("jump");
            SoundController.getInstance().play("jump", data.get("file").asString(), false, data.get("volume").asFloat());
        }

        // If we use sound, we must remember this.
        SoundController.getInstance().update();

    }

    /**
     * Draw the physics object360s to the canvas
     *
     * For simple worlds, this method is enough by itself. It will need to be
     * overriden if the world needs fancy backgrounds or the like.
     *
     * The method draws all objects in the order that they were added.
     *
     * @param delta The drawing context
     */
    public void draw(float delta) {

        canvas.clear();

        if(rippleOn){
            updateShader();
        }

        //render batch with shader
        batch.begin();
        batch.setShader(shaderprog);

        if (shifted) {
            // System.out.println(backgroundTexture.getRegionWidth());
            backgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("past_background").asString(),
                    TextureRegion.class);
        } else {
            backgroundTexture = JsonAssetManager.getInstance().getEntry(levelFormat.get("present_background").asString(),
                    TextureRegion.class);
        }

        sprite = new Sprite(backgroundTexture);
        batch.draw(sprite,0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        batch.end();

        canvas.begin();

        drawObjectInWorld();
        canvas.end();

        drawIndicator(canvas);

        if (debug) {
            canvas.beginDebug();
            drawDebugInWorld();
            canvas.endDebug();
        }

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();


    }


}
