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
 *
 * -----------------------------------------------------------------
 *
 * WismoteNode
 *
 * @author Niclas Finne
 * @author Floris Van den Abeele
 * @author Bart Jooris
 */

package se.sics.mspsim.platform.rm090;
import java.io.IOException;
import java.util.ArrayList;
import se.sics.mspsim.chip.Button;
import se.sics.mspsim.chip.CC2520;
import se.sics.mspsim.chip.Leds;
import se.sics.mspsim.config.MSP430f5437Config;
import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.core.UnifiedClockSystem;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.ui.SerialMon;
import se.sics.mspsim.util.ArgumentManager;

public class RM090Node extends GenericNode implements PortListener, USARTListener {

   @SuppressWarnings("unchecked")
   ArrayList<Pin>[] nodePorts = (ArrayList<Pin>[])new ArrayList[11];
   
   private StringBuilder ledString = new StringBuilder("rgb");
   private boolean ledChanged = false;

   abstract public class Pin {
        public  int portId;
        public  int pinId;
        public  int pinMask;
        public boolean state;
        abstract public void evaluate(int data);
        public boolean checkIfStateChanged(int data) {
            boolean newState = ((data & pinMask) != 0);
            boolean result = (newState != state);
            state = newState;
            //System.err.println("pinMask result portid= " +portId + " pinid " + pinId + " r "+ result);
            return result;            
        }
        
        Pin(int portId, int pinId, boolean state) {
            this.portId= portId;
            this.pinId = pinId;
            this.pinMask = 1 << pinId;
            this.state = state;
            if (nodePorts[portId] == null) {
                nodePorts[portId] = new ArrayList<Pin>();
            }
            nodePorts[portId].add(this);
        }
    }
    private abstract class CC2520_GPIO extends Pin {
        public  int gpioId;
        CC2520_GPIO(int gpioId, int portId, int pinId, boolean state) {
            super(portId, pinId, state);
            this.gpioId = gpioId;
        }
        
        public void register2Radio() {
            radio.configGPIO(gpioId, cpu.getIOUnit(IOPort.class, "P"+portId), pinId);
        }
    }
        
    private CC2520_GPIO CC2520_GPIO0 = new CC2520_GPIO(0, 1, 4, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                radio.setGPIO(0, state);
            }
        }
    };

    private CC2520_GPIO CC2520_GPIO1 = new CC2520_GPIO(1, 1, 5, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                radio.setGPIO(1, state);
            }
        }
    };

    private CC2520_GPIO CC2520_GPIO2 = new CC2520_GPIO(2, 1, 6, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                radio.setGPIO(2, state);
            }
        }
    };
    
    private CC2520_GPIO CC2520_GPIO3 = new CC2520_GPIO(3, 1, 7, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                radio.setGPIO(3, state);
            }
        }
    };
    
    private CC2520_GPIO CC2520_GPIO4 = new CC2520_GPIO(4, 2, 0, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                radio.setGPIO(4, state);
            }
        }
    };

    private CC2520_GPIO CC2520_GPIO5 = new CC2520_GPIO(5, 2, 1, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                radio.setGPIO(5, state);
            }
        }
    };
    
    private Pin CC2520_RESET = new Pin(2, 4, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(~data)) {
                radio.setResetPin(state);
            }
        }
    };
    
    private Pin CC2520_VREG = new Pin(2, 5, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                //System.out.println("CC2520_VREG init from data " + data + " m" + pinMask);
                radio.setVRegOn(state);
            }
        }
    };
    
    private Pin CC2520_CS = new Pin(3, 0, false) /*active low */{
        @Override
        public void evaluate(int data) {
            //log("eval CS " + data + " "+ pinMask) ;
            if (super.checkIfStateChanged(~data)) {
                radio.setChipSelect(state);
            }
        }
    };
    
    private Pin LED0 = new Pin(8, 2, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                leds.setLeds(LEDS_RED, state);
                ledString.setCharAt(0, (state?'R':'r'));
                ledChanged = true;
            }
        }
    };
    
    private Pin LED1 = new Pin(8, 3, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                leds.setLeds(LEDS_GREEN, state);
                ledString.setCharAt(1, (state?'G':'g'));
                ledChanged = true;
            }
        }
    };
    
    private Pin LED2 = new Pin(8, 4, false) {
        @Override
        public void evaluate(int data) {
            if (super.checkIfStateChanged(data)) {
                leds.setLeds(LEDS_BLUE, state);
                ledString.setCharAt(2, (state?'B':'b'));
                ledChanged = true;
            }
        }
    };

    /* RM090 specific note: button also on P2.7 as in wismote */
    /* P2.7 - Button 1*/
    public static final int BUTTON1_PIN = 7;
    /* P2.6 - Button 2*/
    public static final int BUTTON2_PIN = 6;

    /* RM090 specific note: leds on different ports and pins than wismote */
    /* P8.2 - Red (left?) led */
    private static final int LEDS_RED        = 1 << 0; // TODO: is this just and identifier?
    /* P8.3 - Green (middle?) led */
    private static final int LEDS_GREEN       = 1 << 1; 
    /* P8.4 - Blue (right?) led */
    private static final int LEDS_BLUE        = 1 << 2;

    /* This array apparently contains the colour encodings of the leds, the values are RGB values... */
    private static final int[] LEDS = { 0xff2020, 0x20ff20, 0x2020ff }; 

    //private M25P80 flash;
    //private String flashFile;
    private CC2520 radio;
    private Leds leds;
    private Button button;
    private RM090Gui gui;

    public RM090Node() {
        super("RM090mote", new MSP430f5437Config());
        cpu.aclkFrq = 32000;
        UnifiedClockSystem unit = cpu.getIOUnit(UnifiedClockSystem.class, "UnifiedClockSystem");
        if ((unit != null) && (unit instanceof UnifiedClockSystem)) {
            ((UnifiedClockSystem)unit).ACLK_FRQ = 32000;
        }

        
    }

    public Leds getLeds() {
        return leds;
    }

    public Button getButton() {
        return button;
    }

//    public M25P80 getFlash() {
//        return flash;
//    }

//    public void setFlash(M25P80 flash) {
//        this.flash = flash;
//        registry.registerComponent("xmem", flash);
//    }

    public void dataReceived(USARTSource source, int data) {
        radio.dataReceived(source, data);
        //flash.dataReceived(source, data);
        /* if nothing selected, just write back a random byte to these devs */
        if (!radio.getChipSelect() /*&& !flash.getChipSelect()*/) {
            source.byteReceived(0);
        }
    }

    public void portWrite(IOPort source, int data) {
        //System.out.println("P" + source.getPort() + ": = " + data + " " + source.getOut());
        for(Pin pin : nodePorts[source.getPort()]) {
            pin.evaluate(source.getOut());
        }
        if (ledChanged) {
            if (DEBUG) log("Leds = " + ledString + " @ " + cpu.getTimeMillis());
            ledChanged = false;
        }
    }

    private void setupNodePorts() {
//        if (flashFile != null) {
//            setFlash(new FileM25P80(cpu, flashFile));
//        }
        ArrayList<Pin> portPins;
        
        for(int i=0; i < nodePorts.length; i++) {
            portPins = nodePorts[i];
            if (portPins != null && !portPins.isEmpty()) {
                //System.out.println("Port " + i + "has some pins registered");
                IOPort port = cpu.getIOUnit(IOPort.class, "P"+i);
                port.addPortListener(this);
            }
        }
        
        /*
        IOPort port1 = cpu.getIOUnit(IOPort.class, "P1");
        port1.addPortListener(this);
        IOPort port2 = cpu.getIOUnit(IOPort.class, "P2");
        port2.addPortListener(this);
        cpu.getIOUnit(IOPort.class, "P3").addPortListener(this);
        cpu.getIOUnit(IOPort.class, "P4").addPortListener(this);
        //cpu.getIOUnit(IOPort.class, "P5").addPortListener(this); // RM090: not necessary for RM090
        cpu.getIOUnit(IOPort.class, "P8").addPortListener(this);
           */
        IOUnit usart0 = cpu.getIOUnit("USCIB0");
        if (usart0 instanceof USARTSource) { /* RM090: again, CC2520 pin layout is identical to wismote */
            radio = new CC2520(cpu);
            CC2520_GPIO0.register2Radio();
            CC2520_GPIO1.register2Radio();
            CC2520_GPIO2.register2Radio();
            CC2520_GPIO3.register2Radio();
            CC2520_GPIO4.register2Radio();
            CC2520_GPIO5.register2Radio();
            radio.setSOConfig(cpu.getIOUnit(IOPort.class, "P3"), 2);
            ((USARTSource) usart0).addUSARTListener(this);
        } else {
            throw new EmulationException("Could not setup rm090 mote - missing USCIB0");
        }
        leds = new Leds(cpu, LEDS);
        button = new Button("Button1", cpu, cpu.getIOUnit(IOPort.class, "P2"), 7, true);
        //button = new Button("Button2", cpu, port2, 6, true);

        /* RM090: TODO: serial port for RM090, will it work? */
        IOUnit usart = cpu.getIOUnit("USCIA1");
        System.out.println("UART " + usart);
        if (usart instanceof USARTSource) {
            registry.registerComponent("serialio", usart);
        } else {
            throw new EmulationException("Could not setup rm090 mote - missing USCIA1");
        }
    }

    public void setupNode() {
        // create a filename for the flash file
        // This should be possible to take from a config file later!
        String fileName = config.getProperty("flashfile");
        if (fileName == null) {
            fileName = firmwareFile;
            if (fileName != null) {
                int ix = fileName.lastIndexOf('.');
                if (ix > 0) {
                    fileName = fileName.substring(0, ix);
                }
                fileName = fileName + ".flash";
            }
        }
        if (DEBUG) System.out.println("Using flash file: " + (fileName == null ? "no file" : fileName));

        //this.flashFile = fileName;

        setupNodePorts();

        if (!config.getPropertyAsBoolean("nogui", true)) {
            setupGUI();

            // Add some windows for listening to serial output
            IOUnit usart = cpu.getIOUnit("USCI A1");
            if (usart instanceof USARTSource) {
                SerialMon serial = new SerialMon((USARTSource)usart, "USCI A1 Port Output");
                registry.registerComponent("serialgui", serial);
            }
            else {
				throw new EmulationException("Could not setup rm090 mote - missing USCIA1");
            }
        }
    }

    public void setupGUI() {
        if (gui == null) {
            gui = new RM090Gui(this);
            registry.registerComponent("nodegui", gui);
        }
    }

    public int getModeMax() {
        return 0;
    }

    public static void main(String[] args) throws IOException {
        RM090Node node = new RM090Node();
        ArgumentManager config = new ArgumentManager();
        config.handleArguments(args);
        node.setupArgs(config);
    }
}
