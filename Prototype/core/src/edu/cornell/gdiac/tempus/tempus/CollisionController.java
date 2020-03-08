package edu.cornell.gdiac.tempus.tempus;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.tempus.obstacle.Obstacle;
import edu.cornell.gdiac.tempus.tempus.models.*;

public class CollisionController implements ContactListener {
    private PrototypeController controller;
    private ObjectMap<Short, ObjectMap<Short, ContactListener>> listeners;
    private Avatar avatar;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * Creates instance of the collision handler
     * @param w
     */
    public CollisionController(PrototypeController w)
    {
        controller = w;
        avatar = w.getAvatar();
        listeners = new ObjectMap<Short, ObjectMap<Short, ContactListener>>();
        sensorFixtures = new ObjectSet<Fixture>();

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
        else if((objA instanceof Avatar) && (objB instanceof Turret)){
            processAvatarTurretContact(fixA, fixB);
        }
        else if((objB instanceof Avatar) && (objA instanceof Turret)){
            processAvatarTurretContact(fixB, fixA);
        }
        //avatar-door
        else if((objA instanceof Avatar) && (objB instanceof Door)){
            processAvatarDoorContact(fixA, fixB);
        }
        else if((objB instanceof Avatar) && (objA instanceof Door)){
            processAvatarDoorContact(fixB, fixA);
        }
        //TODO: model classes for platforms, projectiles
        //TODO: create delegation of all contacts
    }

    private void processAvatarPlatformContact(Fixture avatar, Fixture platform){
        //TODO: avatar platform contact
    }
    private void processAvatarTurretContact(Fixture avatar, Fixture turret){
        //TODO: avatar turret contact
    }
    private void processAvatarProjectileContact(Fixture avatar, Fixture projectile){
        //TODO: avatar projectile contact
    }
    private void processAvatarDoorContact(Fixture avatar, Fixture door){
        //TODO: avatar door contact
    }
    private void processProjPlatformContact(Fixture projectile, Fixture platform){
        //TODO: platform projectile contact
    }
    private void processProjTurretContact(Fixture projectile, Fixture turret){
        //TODO: platform projectile contact
    }
    private void processProjProjContact(Fixture projectile1, Fixture projectile2){
        //TODO: projectile projectile contact
    }


    public void beginContactHelper(Object sensor, AvatarOrientation or, Object fd1, Object fd2,
                                   Obstacle bd1, Obstacle bd2, Fixture fix1, Fixture fix2){
        if ((sensor.equals(fd2) && avatar != bd1) ||
                (sensor.equals(fd1) && avatar != bd2)) {
            avatar.setAvatarOrientation(or);
            avatar.setGrounded(true);
            avatar.setSticking(true);
            sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
        }
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

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            Obstacle bd1 = (Obstacle)body1.getUserData();
            Obstacle bd2 = (Obstacle)body2.getUserData();

            // Test bullet collision with world
            if (bd1.getName().equals("bullet") && bd2 != avatar) {
                removeBullet(bd1);
            }
            if (bd2.getName().equals("bullet") && bd1 != avatar) {
                removeBullet(bd2);
            }

            beginContactHelper(avatar.getSensorName(), AvatarOrientation.OR_UP, fd1, fd2, bd1, bd2, fix1, fix2);
            beginContactHelper(avatar.getLeftSensorName(), AvatarOrientation.OR_RIGHT, fd1, fd2, bd1, bd2, fix1, fix2);
            beginContactHelper(avatar.getRightSensorName(), AvatarOrientation.OR_LEFT, fd1, fd2, bd1, bd2, fix1, fix2);
            beginContactHelper(avatar.getTopSensorName(), AvatarOrientation.OR_DOWN, fd1, fd2, bd1, bd2, fix1, fix2);

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
