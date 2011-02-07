/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-01 00:58:33 +0100 (Wed, 01 Jul 2009) $
 * $Revision: 11158 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jmol.app;

import java.awt.Point;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.vecmath.Vector3f;

import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.*;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;

public class Jmol extends JmolPanel {

  public Jmol(JmolApp jmolApp, Splash splash, JFrame frame, Jmol parent, int startupWidth,
      int startupHeight, String commandOptions, Point loc) {
    super(jmolApp, splash, frame, parent, startupWidth, startupHeight, commandOptions, loc);
  }
  public static void main(final String[] args) throws Exception {
    final JmolApp jmolApp = new JmolApp(args);
    startJmol(jmolApp);
    jmolApp.viewer.evalString("load http://scripts.iucr.org/cgi-bin/sendcif?hb5777sup1");
    Logger.info("hello");
    
    OSCPortIn receiver = new OSCPortIn(7111); //OSCPort.defaultSCOSCPort());
    OSCListener listener = new OSCListener() {

        final Lock lock = new ReentrantLock();

        public void acceptMessage(java.util.Date time, OSCMessage message) {
            if (lock.tryLock()) {
                try {

                    Object[] args = message.getArguments();

                    int user1 = (Integer)args[0];
                    float x0 = (Float)args[1];
                    float y0 = (Float)args[2];
                    float z0 = (Float)args[3];
                    float x1 = (Float)args[4];
                    float y1 = (Float)args[5];
                    float z1 = (Float)args[6];
                    
                    Vector3f a = new Vector3f(x1-x0, y1-y0, z1-z0); // The "hand vector"
                    Vector3f b = new Vector3f(1.0f, 0, 0);          // The reference axis
                    
                    // Get angle in radians between hand-vector and reference axis
                    float angle = a.angle(b);
                    // Convert to degrees
                    int degrees = (int)((360.0f * angle)/(2.0 * Math.PI)) ;
                    int rounded = degrees/5;
                    degrees = rounded * 5;

                    // Get axis to rotate around (right angles to A and B)
                    Vector3f c = new Vector3f();
                    c.cross(a, b);

                    // Get zoom factor
                    float distance = a.length();
                    float average_z = (z0 + z1)/2.0f;
                    int zoom = (int)(300.0f * distance * average_z); // zoom 100 is normal
                    //rounded = zoom / 20;
                    //zoom = rounded * 20;
                    
                    
                    // Rotate the reference axis onto the hand vector and zoom in
                    jmolApp.viewer.evalString("reset; " + //draw arrow {0 0 0} {5 0 0}; " +  
                        "zoom " + zoom + "; " +
                        "rotate AXISANGLE {"+c.x+" "+c.y+" "+c.z+" "+-degrees+"}");
                    
                } finally {
                    lock.unlock();
                }
            }
        }
    };
    receiver.addListener("/hands", listener);
    receiver.startListening();

}
  
  public static Jmol getJmol(JFrame baseframe, 
                             int width, int height, String commandOptions) {
    JmolApp jmolApp = new JmolApp(new String[] {});
    jmolApp.startupHeight = height;
    jmolApp.startupWidth = width;
    jmolApp.commandOptions = commandOptions;
    return getJmol(jmolApp, baseframe);
  }
  
}
