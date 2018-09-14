package cmbt;
/**
 * Copyright (c) 2001-2017 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/epl-v10.html
 */

import robocode.HitByBulletEvent;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import java.util.ArrayList;
import java.awt.*;
import java.awt.geom.*;
import robocode.util.Utils;
import java.util.List;


/**
 * PaintingRobot - a sample robot that demonstrates the onPaint() and
 * getGraphics() methods.
 * Also demonstrate feature of debugging properties on RobotDialog
 * <p/>
 * Moves in a seesaw motion, and spins the gun around at each end.
 * When painting is enabled for this robot, a red circle will be painted
 * around this robot.
 *
 * @author Stefan Westen (original SGSample)
 * @author Pavel Savara (contributor)
 */
public class WaterRooster extends AdvancedRobot {

    /**
     * PaintingRobot's run method - Seesaw
     */

    //VARIABLES
    List<WaveBullet> waves = new ArrayList<WaveBullet>();
    static int[] stats = new int[31];
    int direction = 1;
    double power = 1;

    public void run() {

        while (true) {

            setAdjustGunForRobotTurn(true);
            setAdjustRadarForGunTurn(true);
            double random = Math.random() * 150 + 1;
            double random1 = Math.random() * 150 + 1;

            setColors(Color.RED, Color.BLACK, Color.WHITE);

            setTurnRadarRight(Double.POSITIVE_INFINITY);

            ahead(random);


            back(random1);


        }
    }

    public class WaveBullet
    {
        private double startX, startY, startBearing, power;
        private long   fireTime;
        private int    direction;
        private int[]  returnSegment;

        public WaveBullet(double x, double y, double bearing, double power,
                          int direction, long time, int[] segment)
        {
            startX         = x;
            startY         = y;
            startBearing   = bearing;
            this.power     = power;
            this.direction = direction;
            fireTime       = time;
            returnSegment  = segment;
        }

        public double getBulletSpeed()
        {
            return 20 - power * 3;
        }

        public double maxEscapeAngle()
        {
            return Math.asin(8 / getBulletSpeed());
        }

        public boolean checkHit(double enemyX, double enemyY, long currentTime)
        {
            // if the distance from the wave origin to our enemy has passed
            // the distance the bullet would have traveled...
            if (Point2D.distance(startX, startY, enemyX, enemyY) <=
                    (currentTime - fireTime) * getBulletSpeed())
            {
                double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);
                double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
                double guessFactor =
                        Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
                int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1));
                returnSegment[index]++;
                return true;
            }
            return false;
        }
    } // end WaveBullet class



    /**
     * Fire when we see a robot
     */


    public void onScannedRobot(ScannedRobotEvent e) {
        // demonstrate feature of debugging properties on RobotDialog
        //enemy absoute bearing

        double absBearing = getHeadingRadians() + e.getBearingRadians();
        //enemy location
        double enemyx = getX() + Math.sin(absBearing) * e.getDistance();
        double enemyy = getY() + Math.sin(absBearing) * e.getDistance();

        //radar lock on
        // Absolute bearing to target
        double radarTurn = absBearing - getRadarHeadingRadians();

        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));

        //gun lock on
        double gunTurn = getHeadingRadians() + e.getBearingRadians() - getGunHeadingRadians();

        setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurn));

        for (int i=0; i < waves.size(); i++)
        {
            WaveBullet currentWave = (WaveBullet)waves.get(i);
            if (currentWave.checkHit(enemyx, enemyy, getTime()))
            {
                waves.remove(currentWave);
                i--;
            }
        }


        if (e.getVelocity() != 0)
        {
            if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0)
                direction = -1;
            else
                direction = 1;
        }
        int[] currentStats = stats;

        WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power,
                direction, getTime(), currentStats);

        int bestindex = 15;	// initialize it to be in the middle, guessfactor 0.
        for (int i=0; i<31; i++)
            if (currentStats[bestindex] < currentStats[i]) {
                bestindex = i;
            }
        // this should do the opposite of the math in the WaveBullet:
        double guessfactor = (double)(bestindex - (stats.length - 1) / 2)
                / ((stats.length - 1) / 2);
        double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
        double gunAdjust = Utils.normalRelativeAngle(
                absBearing - getGunHeadingRadians() + angleOffset);
        setTurnGunRightRadians(gunAdjust);


        if (setFireBullet() != null)
            waves.add(newWave);



        setDebugProperty("lastScannedRobot", e.getName() + " at " + e.getBearing() + " degrees at time " + getTime());

        fire(1);

    }


    /**
     * We were hit!  Turn perpendicular to the bullet,
     * so our seesaw might avoid a future shot.
     * In addition, draw orange circles where we were hit.
     */
    public void onHitByBullet(HitByBulletEvent e) {
        // demonstrate feature of debugging properties on RobotDialog
        setDebugProperty("lastHitBy", e.getName() + " with power of bullet " + e.getPower() + " at time " + getTime());

        // show how to remove debugging property
        setDebugProperty("lastScannedRobot", null);

        // gebugging by painting to battle view
        Graphics2D g = getGraphics();

        g.setColor(Color.orange);
        g.drawOval((int) (getX() - 55), (int) (getY() - 55), 110, 110);
        g.drawOval((int) (getX() - 56), (int) (getY() - 56), 112, 112);
        g.drawOval((int) (getX() - 59), (int) (getY() - 59), 118, 118);
        g.drawOval((int) (getX() - 60), (int) (getY() - 60), 120, 120);

        turnLeft(90 - e.getBearing());
    }

    /**
     * Paint a red circle around our PaintingRobot
     */
    public void onPaint(Graphics2D g) {
        g.setColor(Color.red);
        g.drawOval((int) (getX() - 50), (int) (getY() - 50), 100, 100);
        g.setColor(new Color(0, 0xFF, 0, 30));
        g.fillOval((int) (getX() - 60), (int) (getY() - 60), 120, 120);
    }
}





