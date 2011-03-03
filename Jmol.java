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
import javax.vecmath.Vector2f;
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
        int state = 0;
        float rotX = 0;
        float srotX = 0;
        float erotX = 0;
        float rotY = 0;
        float srotY = 0;
        float erotY = 0;

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

                    float angle = (float) Math.atan2((double)(y1-y0), (double)(z1-z0));
                    int degrees = (int)((360.0f * angle)/(2.0 * Math.PI)) ;
                    int rounded = degrees/5;
                    degrees = rounded * 5;
                    int x_degrees = degrees;

                    angle = (float) Math.atan2((double)(x1-x0), (double)(z1-z0));
                    degrees = (int)((360.0f * angle)/(2.0 * Math.PI)) ;
                    rounded = degrees/5;
                    degrees = rounded * 5;
                    int y_degrees = degrees;                    
                    
                    // Change state
                    int oldstate = state;
                    if (Math.abs(x0 - x1) < 0.06) {
                      if (state != 1) { // entering state
                        srotX = x_degrees;
                        state = 1;
                      }
                      rotX = erotX + (x_degrees - srotX);
                    }
                    else if (Math.abs(y0 - y1) < 0.06) {
                      if (state != 2) {
                        state = 2;
                      }
                      rotY = erotY + (y_degrees - srotY);
                    }
                    else {
                      state = 0;
                    }
                    
                    if (state != oldstate) { // Leaving state
                      if (oldstate == 1)
                        erotX = rotX;
                      if (oldstate == 2)
                        erotY = rotY;
                    }
                    
                    
                    // Get zoom factor
                    int zoom = 100;
                    /*
                    float average_z = (z0 + z1)/2.0f;
                    int zoom = (int)(600.0f * (y0-0.4) * average_z); // zoom 100 is normal
                    */
                    
                    // Get left-right translation
                    float trans = (x0 - 0.4f)*300;
                    
                    // Rotate the reference axis onto the hand vector and zoom in
                    jmolApp.viewer.evalString("reset; " + //draw arrow {0 0 0} {5 0 0}; " +  
                        "zoom " + zoom + "; " +
                        "rotate y " + rotY + "; " +
                        "rotate axisangle { 1 0 0 " + -rotX + "}; " +
                        "translate x " + trans + ";" +
                         "set echo top left; echo " + state);
                    
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
