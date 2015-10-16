/**
 * Copyright (c) 2012, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 */

package se.sics.mspsim.platform.rm090;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import se.sics.mspsim.chip.Leds;
import se.sics.mspsim.core.StateChangeListener;
import se.sics.mspsim.platform.AbstractNodeGUI;

/**
 * @author Niclas Finne
 * @author Floris Van den Abeele
 */
public class RM090Gui extends AbstractNodeGUI {

    private static final long serialVersionUID = -8713047619139235630L;
    
    public class Led {
        int x;
        int y;
        int width;
        int height;
        Color cBorder;
        Color cCore;
        
        
       Led (int x, int y, int width, int height, Color c) {
           this.x = x;
           this.y = y;
           this.width = width;
           this.height = height;
           this.cBorder = new Color(c.getRed()-20, c.getGreen()-20, c.getBlue()-20, 0xa0);
           this.cCore =   new Color(c.getRed(), c.getGreen(), c.getBlue(), 0xff);
       }
       
       public void paint(Graphics g) {
            if (g == null) return;
            //Image mImage;
            /*
            mImage = createImage(width+2, height+2);
            if (mImage == null) return;
            Graphics2D offG = (Graphics2D) mImage.getGraphics();
            offG.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                    */
            g.setColor(cBorder);
            g.fillOval(x - 2, y - 2, width, height);
            g.setColor(cCore);
            g.fillOval(x, y, width - 5, height - 2);            
            // Put the offscreen image on the screen.
            //g.drawImage(mImage, x-2, y-2, null);
       }
    }
    private static final Led[] leds = new Led[4];

    private static final Color BUTTON_C = new Color(0x60ffffff);

    // TODO: this should probably be aligned with the rm090 photo...
    private static  Rectangle LEDS_BOUNDS;

    private final RM090Node node;
    private final StateChangeListener ledsListener = new StateChangeListener() {
        public void stateChanged(Object source, int oldState, int newState) {
            //System.out.println("updated GUI");
            repaint(LEDS_BOUNDS);
            //paintComponent(getGraphics());  
            //paintImmediately(LEDS_BOUNDS);
            //repaint();
        }
    };
    private boolean buttonDown = false;
    private boolean resetDown = false;

    public RM090Gui(RM090Node node) {
        super("RM090Gui", "images/rm090.jpg");
        this.node = node;
        leds[0] = new Led (249, 127, 12, 8, new Color(0x34ff4242));
        leds[1] = new Led (249, 117, 12, 8, new Color(0x3442ff42));
        leds[2] = new Led (249, 107, 12, 8, new Color(0x3442ff42));
        leds[3] = new Led (211, 247, 8, 12, new Color(0x34ff4242));
        int minLedx = leds[0].x;
        int minLedy = leds[0].y;
        int maxLedx = leds[0].x;
        int maxLedy = leds[0].y;
        int maxDim  = leds[0].width;
        
        for (Led led : leds) {
            if (led.x > maxLedx) maxLedx = led.x;
            if (led.x < minLedx) minLedx = led.x;
            if (led.y > maxLedy) maxLedy = led.y;
            if (led.y < minLedy) minLedy = led.y;
            if (led.width > maxDim) maxDim = led.width;
            if (led.height > maxDim) maxDim = led.height;
        }
       
        LEDS_BOUNDS = new Rectangle(  minLedx - 2,
                            minLedy - 2, 
                            maxLedx - minLedx + maxDim + 2,
                            maxLedy - minLedy + maxDim + 2 );
    }

    protected void startGUI() {
        MouseAdapter mouseHandler = new MouseAdapter() {

            // For the button sensor and reset button on the Sky nodes.
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                //System.out.println("click " + x + " : " +y);
                // TODO: determine coordinates of buttons on the image...
                if (x > 95 && x < 107) {
                    if (y > 245 && y < 256) {
                        buttonDown = true;
                        RM090Gui.this.node.getButton().setPressed(true);
                        repaint(95, 245, 13, 13);
                    }
                }
                if (x > 103 && x < 115) {
                    if (y > 26 && y < 38) {
                        resetDown = true;
                        repaint(103, 115, 13, 13);
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (buttonDown) {
                    buttonDown = false;
                    RM090Gui.this.node.getButton().setPressed(false);
                    repaint(95, 245, 13, 13);

                } else if (resetDown) {
                    int x = e.getX();
                    int y = e.getY();
                    resetDown = false;
                    repaint(103, 115, 13, 13);
                    if (x > 103 && x < 115) {
                        if (y > 26 && y < 38) {
                            RM090Gui.this.node.getCPU().reset();
                        }
                    }
                }
            }
        };

        this.addMouseListener(mouseHandler);
        node.getLeds().addStateChangeListener(ledsListener);        
    }

    protected void stopGUI() {
        node.getLeds().removeStateChangeListener(ledsListener);
    }

    protected void paintComponent(Graphics g) {
        Color old = g.getColor();
        Image mImage;
        mImage = createImage(LEDS_BOUNDS.width, LEDS_BOUNDS.width);
        g.drawImage(mImage, LEDS_BOUNDS.x, LEDS_BOUNDS.y, null);

        
        super.paintComponent(g);

        int changed = node.getLeds().getLeds();
        if ((changed & 1) > 0) leds[0].paint(g);
        if ((changed & 2) > 0) leds[1].paint(g);
        if ((changed & 4) > 0) leds[2].paint(g);
        leds[3].paint(g);


        if (buttonDown) {
            g.setColor(BUTTON_C);
            g.fillOval(8, 236, 9, 9);
        }
        if (resetDown) {
            g.setColor(BUTTON_C);
            g.fillOval(8, 271, 9, 9);
        }
        g.setColor(old);
    }
    
}
