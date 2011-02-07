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
                    
                    // Convert to degrees
                    int x_degrees = (int)((360.0f * x1 * 10)/(2.0 * Math.PI)) ;
                    int rounded = x_degrees/5;
                    x_degrees = rounded * 5;

  /*                  // Convert to degrees
                    int y_degrees = (int)((360.0f * y1 * 10)/(2.0 * Math.PI)) ;
                    rounded = y_degrees/5;
                    y_degrees = rounded * 5;*/

                    // Get zoom factor
                    float distance = a.length();
                    float average_z = (z0 + z1)/2.0f;
                    int zoom = (int)(600.0f * (y0-0.3) * average_z); // zoom 100 is normal
                    //rounded = zoom / 20;
                    //zoom = rounded * 20;
                    
                    // Get left-right translation
                    float trans = (x0 - 0.4f)*300;
                    
                    
                    // Rotate the reference axis onto the hand vector and zoom in
                    jmolApp.viewer.evalString("reset; " + //draw arrow {0 0 0} {5 0 0}; " +  
                        "zoom " + zoom + "; " +
                        "rotate y " + x_degrees + "; " +
//                        "rotate x " + y_degrees + "; " +
                        "translate x " + trans + ";" +
                        "set echo top left; echo " + y0);
                    
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
