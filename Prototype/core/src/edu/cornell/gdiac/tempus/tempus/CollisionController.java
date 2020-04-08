package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.tempus.InputController;
import edu.cornell.gdiac.tempus.obstacle.Obstacle;
import edu.cornell.gdiac.tempus.tempus.models.*;
import edu.cornell.gdiac.util.PooledList;

import java.util.Iterator;

public class CollisionController implements ContactListener {
    private PrototypeController controller;
    private ObjectMap<Short, ObjectMap<Short, ContactListener>> listeners;
    private Avatar avatar;
    private PooledList<Obstacle> obstacles;
    private float cur_normal;
    private CollisionPair prevCollisionPair;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * CURRENTLY UNUSED
     * helper class that uses comparable to determine if collision events are happening between the same two bodies
     */
    class CollisionPair implements Comparable {

        private Obstacle A;
        private Obstacle B;

        public CollisionPair(Obstacle a, Obstacle b){
            A = a;
            B = b;
        }
        public CollisionPair(){
            //do nothing
        }

        public Obstacle getA() {
            return A;
        }

        public Obstacle getB() {
            return B;
        }

        @Override
        public int compareTo(Object o) {
            CollisionPair comp = (CollisionPair) o;
            if(comp.getA() == null){
                return -1;
            }
            if((comp.getA() == this.getA() && comp.getB() == this.getB()) ||
                    (comp.getB() == this.getA() && comp.getA() == this.getB())){
                return 0;
            }
            return -1;
        }
    }

    /**
     * Creates instance of the collision handler
     * @param w
     */
    public CollisionController(PrototypeController w)
    {
        controller = w;
        avatar = w.getAvatar();
        obstacles = w.getObjects();
        cur_normal = 0;
        listeners = new ObjectMap<Short, ObjectMap<Short, ContactListener>>();
        sensorFixtures = new ObjectSet<Fixture>();
        prevCollisionPair = new CollisionPair();
    }

    /**
     * Process all contacts in the game
     * @param contact is a detected contact with a sensor
     */
    private void processContact(Contact contact)
    {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        Object objA = fixA.getBody().getUserData();
        Object objB = fixB.getBody().getUserData();

        //avatar-projectile
        if((objA instanceof Avatar) && (objB instanceof Projectile)){
            processAvatarProjectileContact(fixA, fixB);
        }else if((objB instanceof Avatar) && (objA instanceof Projectile)){
            processAvatarProjectileContact(fixB, fixA);
        }
        //avatar-platform
        else if((objA instanceof Avatar) && (objB instanceof Platform)){
            processAvatarPlatformContact(fixA, fixB);
        }else if((objB instanceof Avatar) && (objA instanceof Platform)){
            processAvatarPlatformContact(fixB, fixA);
        }
        //avatar-turret
        else if((objA instanceof Avatar) && (objB instanceof Enemy)){
            processAvatarEnemyContact(fixA, fixB);
        }
        else if((objB instanceof Avatar) && (objA instanceof Enemy)){
            processAvatarEnemyContact(fixB, fixA);
        }
        //avatar-door
        else if((objA instanceof Avatar) && (objB instanceof Door)){
            processAvatarDoorContact(fixA, fixB);
        }
        else if((objB instanceof Avatar) && (objA instanceof Door)){
            processAvatarDoorContact(fixB, fixA);
        }
        //avatar-projectile
        else if((objA instanceof Projectile) && (objB instanceof Enemy)){
            processProjEnemyContact(fixA, fixB);
        }
        else if((objB instanceof Projectile) && (objA instanceof Enemy)){
            processProjEnemyContact(fixB, fixA);
        }
        //TODO: model classes for platforms, projectiles
        //TODO: create delegation of all contacts
    }

    private void processAvatarPlatformContact(Fixture av, Fixture platform){
        //TODO: avatar platform contact
    }
    private void processAvatarEnemyContact(Fixture av, Fixture turret){
        //TODO: avatar turret contact (die)
    }
    private void processAvatarProjectileContact(Fixture av, Fixture projectile){
        if (!avatar.isHolding() && InputController.getInstance().pressedRightMouseButton()) {
            avatar.setHolding(true);
            Obstacle bullet = (Obstacle) projectile.getBody().getUserData();
            avatar.setHeldBullet((Projectile) bullet);
            removeBullet(bullet);
            // //bullet.markRemoved(true);
           //bullet.setPosition(avatar.getPosition());
            // avatar.setHeldBullet(bullet);
//            projectile.getBody().setType(BodyDef.BodyType.StaticBody);
        } else {
            Obstacle bullet = (Obstacle) projectile.getBody().getUserData();
            removeBullet(bullet);
            /*else if (avatar.isHolding() && InputController.getInstance().releasedLeftMouseButton()){
            Vector2 mousePos = InputController.getInstance().getMousePosition();
            avatar.setBodyType(BodyDef.BodyType.DynamicBody);
            avatar.setSticking(false);
            avatar.setWasSticking(false);
            avatar.setDashing(true);
            avatar.setDashStartPos(avatar.getPosition().cpy());
            avatar.setDashDistance(avatar.getDashRange());
            avatar.setDashForceDirection(mousePos.cpy().sub(avatar.getPosition()));
            Obstacle obj = avatar.getHeldBullet();
            obj.setBodyType(BodyDef.BodyType.DynamicBody);
            Vector2 bulletRedirection = avatar.getPosition().cpy().sub(mousePos).nor();
            obj.setPosition(obj.getPosition().add(bulletRedirection.cpy()));
            obj.setLinearVelocity(bulletRedirection.cpy().scl(12.0f));
            avatar.setHolding(false);
            avatar.setHeldBullet(null);
        }*/
        }
    }
    private void processAvatarDoorContact(Fixture av, Fixture door){
        //TODO: avatar door contact
    }
    private void processProjPlatformContact(Fixture projectile, Fixture platform){
        //TODO: platform projectile contact
    }
    private void processProjEnemyContact(Fixture projectile, Fixture enemy){
        Body projBody = projectile.getBody();
        Projectile proj = (Projectile) projBody.getUserData();
        Body enemyBody = enemy.getBody();
        Enemy e = (Enemy) enemyBody.getUserData();
//        System.out.println("proj type: " + proj.getType());
//        System.out.println("enemy type: " + e.getType());

        // if projectile and enemy are of different worlds
        // then remove enemy
        if (proj.getType() != e.getType() && !e.getName().equals("turret")) {
            Obstacle obs = (Obstacle) enemyBody.getUserData();
            obs.markRemoved(true);
        }
        removeBullet(proj);
    }
    private void processProjProjContact(Fixture projectile1, Fixture projectile2){
        //TODO: projectile projectile contact
    }

    /**
     * Remove a new bullet from the world.
     *
     * @param  bullet   the bullet to remove
     */
    public void removeBullet(Obstacle bullet) {
        bullet.markRemoved(true);
        /*
        TODO: implement AssetManager
         */
    }
    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.  In particular, we
     * use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    @Override
    public void beginContact(Contact contact) {
        
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object objA = fix1.getBody().getUserData();
        Object objB = fix2.getBody().getUserData();

        try {
            Obstacle bd1 = (Obstacle)objA;
            Obstacle bd2 = (Obstacle)objB;

            // Test bullet collision with world
            if (bd1.getSpace() != 3 && bd2.getSpace() != 3 && bd1.getSpace() != bd2.getSpace()) {
                return;
            }
                if (bd1.getName().equals("bullet") && bd2 != avatar) {
                    removeBullet(bd1);
                }
                if (bd2.getName().equals("bullet") && bd1 != avatar) {
                    removeBullet(bd2);
                }


            //handle platform-avatar collisions first (outside of processcontact
            if(((objA instanceof Avatar) && (objB instanceof Platform)) ||((objB instanceof Avatar) && (objA instanceof Platform))){

                if(avatar.isSticking()){
                    System.out.println("after: " + bd1.getName());
                    System.out.println("after 1: " + bd2.getName());

                } else{
                    Float norm_angle = contact.getWorldManifold().getNormal().angle();

                    if(!norm_angle.isNaN()){
                        cur_normal = ((norm_angle.intValue()) == 0) ? 0 : (float) Math.toRadians(norm_angle-90);
                        System.out.println("MANIFOLD ANGLE: " + norm_angle);
                    }
                }

                if (!avatar.isSticking() && avatar.getStartedDashing() == 0) {
                    /*System.out.println("previous: " + avatar.getCurrentPlatform());
                    if (objB instanceof Platform) {
                        System.out.println("OBject: " + objB);
                    } else {
                        System.out.println("OBject: " + objA);
                    }*/
                    if (avatar.getCurrentPlatform() != null && (bd1.getName().equals("wall") || bd2.getName().equals("wall"))) {
                        //System.out.println("Attempted to stick to wall");
                        if (!avatar.getCurrentPlatform().equals(objA) && !avatar.getCurrentPlatform().equals(objB)) {
                            //System.out.println("STUCK");
                            avatar.setGrounded(true);
                            avatar.setSticking(true);
                            avatar.setNewAngle(cur_normal);
                            if (objB instanceof Platform) {
                                avatar.setCurrentPlatform((Platform) objB);
                                //System.out.println("set current platform to: " + objB);
                            } else {
                                avatar.setCurrentPlatform((Platform) objA);
                                //System.out.println("set current platform to: " + objA);
                            }
                        }
                    } else {
                        avatar.setGrounded(true);
                        avatar.setSticking(true);
                        avatar.setNewAngle(cur_normal);
                        if (objB instanceof Platform) {
                            avatar.setCurrentPlatform((Platform) objB);
                            //System.out.println("set current platform to: " + objB);
                        } else {
                            avatar.setCurrentPlatform((Platform) objA);
                            //System.out.println("set current platform to: " + objA);
                        }
                    }
                }
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
//                beginContactHelper(avatar.getSensorName(), AvatarOrientation.OR_UP, fd1, fd2, bd1, bd2, fix1, fix2);
//                beginContactHelper(avatar.getLeftSensorName(), AvatarOrientation.OR_RIGHT, fd1, fd2, bd1, bd2, fix1, fix2);
//                beginContactHelper(avatar.getRightSensorName(), AvatarOrientation.OR_LEFT, fd1, fd2, bd1, bd2, fix1, fix2);
//                beginContactHelper(avatar.getTopSensorName(), AvatarOrientation.OR_DOWN, fd1, fd2, bd1, bd2, fix1, fix2);

            }

            for (Obstacle obj: obstacles) {
                if (obj instanceof Enemy) {
                    Enemy enemy = (Enemy) obj;
                    if ((enemy.getLeftSensorName().equals(fd2) && enemy != bd1 && !bd1.getName().equals("bullet")) ||
                            (enemy.getLeftSensorName().equals(fd1) && enemy != bd2 && !bd2.getName().equals("bullet"))) {
                        enemy.getLeftFixtures().add(enemy == bd1 ? fix2 : fix1);
                    }
                    if ((enemy.getRightSensorName().equals(fd2) && enemy != bd1 && !bd1.getName().equals("bullet")) ||
                            (enemy.getRightSensorName().equals(fd1) && enemy != bd2 && !bd2.getName().equals("bullet"))) {
                        enemy.getRightFixtures().add(enemy == bd1 ? fix2 : fix1);
                    }
                }
            }

            // Check for win condition
            if ((bd1 == avatar   && bd2 == controller.getGoalDoor()) ||
                    (bd1 == controller.getGoalDoor() && bd2 == avatar)) {
                    controller.setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the characer is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    @Override
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
                avatar.setSticking(false);
            }
        }

        for (Obstacle obj: obstacles) {
            if (obj instanceof Enemy) {
                Enemy enemy = (Enemy) obj;
                if ((enemy.getLeftSensorName().equals(fd2) && enemy != bd1) ||
                        (enemy.getLeftSensorName().equals(fd1) && enemy != bd2)) {
                    enemy.getLeftFixtures().remove(enemy == bd1 ? fix2 : fix1);
                    if (enemy.getLeftFixtures().size == 0) {
                        enemy.setMovement(0);
                        enemy.setNextDirection(1);
                    }
                }

                if ((enemy.getRightSensorName().equals(fd2) && enemy != bd1) ||
                        (enemy.getRightSensorName().equals(fd1) && enemy != bd2)) {
                    enemy.getRightFixtures().remove(enemy == bd1 ? fix2 : fix1);
                    if (enemy.getRightFixtures().size == 0) {
                        enemy.setMovement(0);
                        enemy.setNextDirection(-1);
                    }
                }
            }
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        this.processContact(contact);
    }

    /**
     * Returns the collection of listeners
     * @param categoryA
     * @param categoryB
     * @return
     */
    private ContactListener getListener(short categoryA, short categoryB)
    {
        ObjectMap<Short, ContactListener> listenerCollection = listeners.get(categoryA);
        if (listenerCollection == null)
        {
            return null;
        }
        return listenerCollection.get(categoryB);
    }

}
