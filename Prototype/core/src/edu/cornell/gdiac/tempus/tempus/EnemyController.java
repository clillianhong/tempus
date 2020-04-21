package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.tempus.GameCanvas;
import edu.cornell.gdiac.tempus.WorldController;
import edu.cornell.gdiac.tempus.obstacle.Obstacle;
import edu.cornell.gdiac.tempus.tempus.models.Avatar;
import edu.cornell.gdiac.tempus.tempus.models.Enemy;
import edu.cornell.gdiac.tempus.tempus.models.Platform;
import edu.cornell.gdiac.tempus.tempus.models.Projectile;
import edu.cornell.gdiac.util.JsonAssetManager;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.SoundController;

import java.util.Random;

import static edu.cornell.gdiac.tempus.tempus.models.EntityType.PRESENT;

public class EnemyController {

    /** Offset for bullet when firing */
    private static final float BULLET_OFFSET = 1.5f;
    /** The density for a bullet */
    private static final float HEAVY_DENSITY = 10.0f;

    /** All the objects in the world. */
    private PooledList<Obstacle> objects  = new PooledList<Obstacle>();
    /** The enemy being controlled */
    private PooledList<Enemy> enemies  = new PooledList<Enemy>();
    /** Target being aimed at */
    private Avatar target;
    /** World */
    private World world;
    /** Scaling of the world */
    private Vector2 scale;
    /** Present or past */
    private boolean shifted;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** The reader to process JSON files */
    private JsonReader jsonReader;
    /** Assets for firing bullets */
    private JsonValue assetDirectory;

    private WorldController worldController;
    private GameCanvas canvas;

    public EnemyController(PooledList<Enemy> enemies, PooledList<Obstacle> objects, Avatar target, World world,
                           Vector2 scale, WorldController worldController) {
        this.enemies = enemies;
        this.objects = objects;
        this.target = target;
        this.world = world;
        this.scale = scale;
        this.shifted = false;
        this.worldController = worldController;
        jsonReader = new JsonReader();
        assetDirectory = jsonReader.parse(Gdx.files.internal("jsons/assets.json"));
        canvas = worldController.getCanvas();
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        for (Enemy e: enemies) {
            e.deactivatePhysics(world);
        }
        enemies.clear();
    }

    /**
     * Sets the cooldown for enemies when slowed down
     * @param flag whether the world is slowed down
     */
    public void slowCoolDown(boolean flag) {
        for (Enemy e: enemies) {
            if (flag) {
                e.setLimiter(1);
            } else {
                e.setLimiter(4);
            }
        }
    }

    /**
     * Shifts the world
     */
    public void shift() {
        shifted = !shifted;
        for (Enemy e: enemies) {
            if (!e.isTurret()) {
                e.coolDown(false);
                e.setLeftFixture(null);
                e.setRightFixture(null);
            }
        }
    }

    /**
     * Moves the enemy left and right
     */
    public void applyForce(Enemy e) {
        if (!e.isActive()) {
            return;
        }

        // Don't want to be moving. Damp out player motion
        if (e.getMovement() == 0f) {
            forceCache.set(-e.getDamping()*e.getVX(),0);
            e.getBody().applyForce(forceCache,e.getPosition(),true);
        }

        // Velocity too high, clamp it
        if (Math.abs(e.getVX()) >= e.getMaxSpeed()) {
            e.setVX(Math.signum(e.getVX()) * e.getMaxSpeed());
        } else {
            forceCache.set(e.getMovement(),0);
            e.getBody().applyForce(forceCache,e.getPosition(),true);
        }
    }

    /**
     * Sets the velocity of the bullet to aim at the target
     *
     * @param offset float of the offset from the enemy's center that bullet shoots from
     */
    public void setBulletVelocity(float offset, Enemy enemy) {
        Vector2 projVel = target.getPosition().sub(enemy.getPosition());
        projVel.y -= offset;
        enemy.setProjVel(projVel);
    }

    public void setFlyingVelocity (Enemy enemy) {
        Vector2 vel = target.getPosition().sub(enemy.getPosition());
        if (vel != enemy.getFlyingVelocity()) {
            enemy.setLinearVelocity(new Vector2(0,0));
            enemy.setAngularVelocity(0);
            enemy.setFlyingVelocity(vel);
        }
    }

    public void processAction() {
        for (Enemy e: enemies) {
            if (e.isTurret()) {
                fire(e);
            } else if ((!shifted && e.getSpace() == 1) || (shifted && e.getSpace() == 2)) {
                if (e.getAi() == Enemy.EnemyType.WALK && e.getLeftFixture() != null && e.getRightFixture() != null) {
                    createLineOfSight(world, BULLET_OFFSET, e);
                    applyForce(e);
                    setBulletVelocity(BULLET_OFFSET, e);
                    fire(e);
                } else if (e.getAi() == Enemy.EnemyType.TELEPORT) {
                    e.coolDown(true);
                    setBulletVelocity(BULLET_OFFSET, e);
                    findPlatform(e);
                } else if (e.getAi() == Enemy.EnemyType.GUN) {
                    createLineOfSight(world, BULLET_OFFSET, e);
                    setBulletVelocity(BULLET_OFFSET, e);
                    fire(e);
                } else {
                    createLineOfSight(world, BULLET_OFFSET, e);
                    setFlyingVelocity(e);
                    setBulletVelocity(BULLET_OFFSET, e);
                    fly(e);
                    fire(e);
                }
            }
        }
    }

    public void fly(Enemy e) {
        // Velocity too high, clamp it
        if (Math.abs(e.getVX()) >= e.getMaxSpeed()) {
            e.setVX(Math.signum(e.getVX()) * e.getMaxSpeed());
        }
        if (Math.abs(e.getVY()) >= e.getMaxSpeed()) {
            e.setVY(Math.signum(e.getVY()) * e.getMaxSpeed());
        }
        forceCache.set(e.getFlyingVelocity());
        e.getBody().applyForce(forceCache, e.getPosition(), true);
    }

    public void findPlatform(Enemy e) {
        for (Obstacle ob: objects) {
            if (ob instanceof Platform &&
                    ((!shifted && ob.getSpace() == 1) || (shifted && ob.getSpace() == 2))) {
                Platform p = (Platform) ob;
                teleportLineOfSight(p, e);
                if (e.getTeleportTo() != null && e.canFire()
                        && e.getTeleportTo().getBody().getUserData() != target.getCurrentPlatform()
                        && e.getCurrPlatform() != e.getTeleportTo()) {
                    teleport(e, p);
                    return;
                }
            }
        }
    }

    public void teleport(Enemy e, Platform p) {
        Vector2 newPos = p.getPosition();
        newPos.y += p.getHeight() + (e.getHeight() / 2);
        newPos.x += p.getWidth() / 2;
        e.setPosition(newPos);
        e.setCurrPlatform(p);
        setBulletVelocity(BULLET_OFFSET, e);
        createBullet(e);
    }

    public void fire(Enemy e) {
        if (e.canFire()) {
            createBullet(e);
        } else {
            e.coolDown(true);
        }
    }

//    public void fire() {
//        for (Enemy e: enemies) {
//            if (e.isTurret()) {
//                if (e.canFire()) {
//                    createBullet(e);
//                } else
//                    e.coolDown(true);
//            } else if ((!shifted && e.getSpace() == 1) || (shifted && e.getSpace() == 2)) {
//                if (e.getLeftFixture() != null && e.getRightFixture() != null) {
//                    createLineOfSight(world, BULLET_OFFSET, e);
//                    applyForce(e);
//                    if (e.canFire()) {
//                        if (e.getName() == "enemy") {
//                            setBulletVelocity(BULLET_OFFSET, e);
//                        }
//                        createBullet(e);
//                    } else
//                        e.coolDown(true);
//                }
//            }
//        }
//    }

    /**
     * Add a new bullet to the world and send it in the right direction.
     *
     * @param enemy enemy
     */
    private void createBullet(Enemy enemy) {
        float offset = BULLET_OFFSET;

//		//TODO: quick fix for enemy projectile offsets
//		if (!enemy.isTurret() && enemy.getType() == PAST) {
//			offset = 2.5f;
//		}
//		if (!enemy.isTurret() && enemy.getType() == PRESENT) {
//			offset = 1.5f;
//		}

        TextureRegion bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig", TextureRegion.class);
        TextureRegion presentBullet = JsonAssetManager.getInstance().getEntry("projpresent", TextureRegion.class);
        TextureRegion pastbullet = JsonAssetManager.getInstance().getEntry("projpast", TextureRegion.class);
        float radius = bulletBigTexture.getRegionWidth() / (30.0f);
        Projectile bullet = new Projectile(enemy.getType(), enemy.getX(), enemy.getY() + offset, radius,
                enemy.getBody().getUserData());

        Filter f = new Filter();
        Random random = new Random();
        f.groupIndex = (short) -random.nextInt(Short.MAX_VALUE + 1);
        enemy.setFilterData(f);
        bullet.setFilterData(f);

        bullet.setName("bullet");
        bullet.setDensity(HEAVY_DENSITY);
        bullet.setDrawScale(scale);
        //bullet.setTexture(bulletBigTexture);
        bullet.setBullet(true);
        bullet.setGravityScale(0);
        bullet.setLinearVelocity(enemy.getProjVel());
        bullet.setSpace(enemy.getSpace());
        worldController.addQueuedObject(bullet);
        if (bullet.getType().equals(PRESENT)){
            bullet.setTexture(presentBullet);
        } else {
            bullet.setTexture(pastbullet);
        }

        if (shifted && enemy.getSpace() == 2) { // past world
            JsonValue data = assetDirectory.get("sounds").get("pew");
//			System.out.println("sound volume: " +  data.get("volume").asFloat());
            SoundController.getInstance().play("pew", data.get("file").asString(), false, data.get("volume").asFloat());
        } else if (!shifted && enemy.getSpace() == 1) { // present world
            JsonValue data = assetDirectory.get("sounds").get("pew");
//			System.out.println("sound volume: " +    data.get("volume").asFloat());
            SoundController.getInstance().play("pew", data.get("file").asString(), false, data.get("volume").asFloat());
        }

        // Reset the firing cooldown.
        enemy.coolDown(false);
    }

    /**
     * Updates the enemies line of sight to check if it can see the target
     * @param world
     * @param offset offset from the enemy center to where the bullet shoots from
     */
    public void createLineOfSight(World world, float offset, Enemy e) {
        Vector2 shootPos = e.getPosition().add(0f, offset);
        TextureRegion bulletBigTexture = JsonAssetManager.getInstance().getEntry("bulletbig", TextureRegion.class);
        float radius = bulletBigTexture.getRegionWidth() / (30.0f);
        shootPos.y -= radius * 2;
        world.rayCast(e.getSight(), shootPos, target.getPosition());
    }

    public void teleportLineOfSight(Platform p, Enemy e) {
        e.setTeleportTo(p);
        Vector2 aim = p.getPosition();
        aim.y += e.getHeight() / 2 + p.getHeight() / 2 + BULLET_OFFSET;
        aim.x += p.getWidth() / 2;
        world.rayCast(e.getSight(), aim, target.getPosition());
    }

    public void sleepIfNotInWorld() {
        for (Enemy e: enemies) {
            if (!e.isTurret()) {
                e.setActive(true);
                if (e.getSpace() == 3) {
                    e.setIsFiring(true);
                    e.setActive(true);
                } else if (!shifted && (e.getSpace()==2)) {
                    e.setIsFiring(false);
                    e.setActive(false);
                } else if (shifted && (e.getSpace()==2)) {
                    e.setIsFiring(e.getShiftedFiring());
                } else if (shifted && (e.getSpace()==1)) {
                    e.setIsFiring(false);
                    e.setActive(false);
                } else if (!shifted && (e.getSpace()==1)) {
                    e.setIsFiring(e.getShiftedFiring());
                }
            } else {
                e.setSensor(false);
                if (e.getSpace() == 3) {
                    e.setSensor(false);
                } else if (!shifted && (e.getSpace() == 2)) {
                    e.setSensor(true);
                } else if (shifted && (e.getSpace() == 1)) {
                    e.setSensor(true);
                }
            }
        }
    }

    public void drawEnemiesInWorld() {
        for (Enemy e: enemies) {
            if (e.getSpace() == 3) {
                e.draw(canvas);
            } else if (shifted && (e.getSpace() == 2)) { // past world
                e.draw(canvas);
            } else if (!shifted && (e.getSpace() == 1)) { // present world
                e.draw(canvas);
            }
        }
    }

    public void drawEnemiesDebugInWorld() {
        for (Enemy e: enemies) {
            if (e.getSpace() == 3) {
                e.drawDebug(canvas);
            } else if (shifted && (e.getSpace() == 2)) {
                e.drawDebug(canvas);
            } else if (!shifted && (e.getSpace() == 1)) {
                e.drawDebug(canvas);
            }
        }
    }
}