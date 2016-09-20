/**
 * @author Bart Jooris
 */


package se.sics.mspsim.core;

import se.sics.mspsim.core.EmulationLogger.WarningType;
import se.sics.mspsim.util.Utils;
import java.util.Scanner;

public class DMAxv2 extends IOUnit {

    private static final boolean DEBUG = true;
    
    
    // DMA block offset
    public static final int DMA_BLOCK_CONTROL   = 0x00;
    public static final int DMA_BLOCK_CHANNEL0  = 0x10;
    public static final int DMA_BLOCK_CHANNEL1  = 0x20;
    public static final int DMA_BLOCK_CHANNEL2  = 0x30;

    // DMA control offset
    public static final int DMACTL0             = 0x00;
    public static final int DMACTL1             = 0x02;
    public static final int DMACTL2             = 0x04;
    public static final int DMACTL3             = 0x06;
    public static final int DMACTL4             = 0x08;
    public static final int DMAIV               = 0x0E;
    
    
    // DMA channel offset
    public static final int DMAxCTL     = 0x00;
    public static final int DMAxSAlow   = 0x02;
    public static final int DMAxSAhigh  = 0x04;
    public static final int DMAxDAlow   = 0x06;
    public static final int DMAxDAhigh  = 0x08;
    public static final int DMAxSZ      = 0x0A;
 
    //copied form msp430f5437
    public static final int DMAREQ    = 0x0  ; //DMA channel 0 transfer select 0:  DMA_REQ (sw)
    public static final int TA0CCR0   = 0x1  ; //DMA channel 0 transfer select 1:  Timer0_A (TA0CCR0.IFG)
    public static final int TA0CCR2   = 0x2  ; //DMA channel 0 transfer select 2:  Timer0_A (TA0CCR2.IFG)
    public static final int TA1CCR0   = 0x3  ; //DMA channel 0 transfer select 3:  Timer1_A (TA1CCR0.IFG)
    public static final int TA1CCR2   = 0x4  ; //DMA channel 0 transfer select 4:  Timer1_A (TA1CCR2.IFG)
    public static final int TB0CCR0   = 0x5  ; //DMA channel 0 transfer select 5:  TimerB (TB0CCR0.IFG)
    public static final int TB0CCR2   = 0x6  ; //DMA channel 0 transfer select 6:  TimerB (TB0CCR2.IFG)
    public static final int RES7      = 0x7  ; //DMA channel 0 transfer select 7:  Reserved
    public static final int RES8      = 0x8  ; //DMA channel 0 transfer select 8:  Reserved
    public static final int RES9      = 0x9  ; //DMA channel 0 transfer select 9:  Reserved
    public static final int RES10     = 0xa  ; //DMA channel 0 transfer select 10: Reserved
    public static final int RES11     = 0xb  ; //DMA channel 0 transfer select 11: Reserved
    public static final int RES12     = 0xc  ; //DMA channel 0 transfer select 12: Reserved
    public static final int RES13     = 0xd  ; //DMA channel 0 transfer select 13: Reserved
    public static final int RES14     = 0xe  ; //DMA channel 0 transfer select 14: Reserved
    public static final int RES15     = 0xf  ; //DMA channel 0 transfer select 15: Reserved
    public static final int USCIA0RX  = 0x10 ; //DMA channel 0 transfer select 16: USCIA0 receive
    public static final int USCIA0TX  = 0x11 ; //DMA channel 0 transfer select 17: USCIA0 transmit
    public static final int USCIB0RX  = 0x12 ; //DMA channel 0 transfer select 18: USCIB0 receive
    public static final int USCIB0TX  = 0x13 ; //DMA channel 0 transfer select 19: USCIB0 transmit
    public static final int USCIA1RX  = 0x14 ; //DMA channel 0 transfer select 20: USCIA1 receive
    public static final int USCIA1TX  = 0x15 ; //DMA channel 0 transfer select 21: USCIA1 transmit
    public static final int USCIB1RX  = 0x16 ; //DMA channel 0 transfer select 22: USCIB1 receive
    public static final int USCIB1TX  = 0x17 ; //DMA channel 0 transfer select 23: USCIB1 transmit
    public static final int ADC12IFG  = 0x18 ; //DMA channel 0 transfer select 24: ADC12IFGx
    public static final int RES25     = 0x19 ; //DMA channel 0 transfer select 25: Reserved
    public static final int RES26     = 0x1a ; //DMA channel 0 transfer select 26: Reserved
    public static final int RES27     = 0x1b ; //DMA channel 0 transfer select 27: Reserved
    public static final int RES28     = 0x1c ; //DMA channel 0 transfer select 28: Reserved
    public static final int MPY       = 0x1d ; //DMA channel 0 transfer select 29: Multiplier ready
    public static final int DMA2IFG   = 0x1e ; //DMA channel 0 transfer select 30: previous DMA channel DMA2IFG
    public static final int DMAE0     = 0x1f ; //DMA channel 0 transfer select 31: ext. Trigger (DMAE0)

    public static final int IFG_MASK = 0x08;
    
    private static final int[] INCR = {0,0,-1,1};
    
  private final int dmaVector;
    
    
    private int lastDMAIV;
    
    class Channel {
        int channelNo;
        /* public registers */
        int blockOffset;
        
        public int ctl;
        Integer sourceAddress = new Integer(0);
        Integer destinationAddress= new Integer(0);
        int size;
        
        /* internal registers */
        Integer currentSourceAddress= new Integer(0);
        Integer currentDestinationAddress= new Integer(0);
        int storedSize;

        int srcIncr = 0;
        int dstIncr = 0;
        boolean dstByteMode = false;
        boolean srcByteMode = false;

        DMAxv2Trigger trigger;
        int triggerIndex;
        int transferMode = 0;
        
        boolean enable = false;
        boolean dmaLevel = false; /* edge or level sensitive trigger */
        boolean dmaIE = false;
        boolean dmaIFG = false;
        
        public Channel(int i, int offset) {
            channelNo = i;
            this.blockOffset = offset;
        }

        public void setTrigger(DMAxv2Trigger t, int index) {
            if (DEBUG) log("Setting channel " + channelNo + " trigger to " + (t==null?t:((IOUnit)t).getName()  +":" + index));
            trigger = t;
            triggerIndex = index;
        }
        
        public void triggerInterrupt() {
            /* trigger if trigger should be... */
            if (dmaIV == 0) {
                dmaIV = (channelNo + 1) * 2;
                if (DEBUG) log("triggering interrupt channel : " + channelNo + " with IV: 0x" + Utils.hex(dmaIV,4));
                cpu.flagInterrupt(dmaVector, DMAxv2.this, true);
            } else if (dmaIV > (channelNo + 1) ) {
                /* interrupt already triggered, but set to this lower IRQ */
                if (DEBUG) log("override triggering interrupt channel : " + channelNo);
                dmaIV =(channelNo + 1)  * 2;
            }
        }
        
        
        public void write(int address, int data) {
            switch(address) {
            case DMAxCTL:
                ctl = data;
                transferMode = (data >> 12) & 7;
                dstIncr = INCR[(data >> 10) & 3];
                srcIncr = INCR[(data >> 8) & 3];
                dstByteMode = (data & 0x80) > 0; /* bit 7 */
                srcByteMode = (data & 0x40) > 0; /* bit 6 */
                dmaLevel = (data & 0x20) > 0; /* bit 5 */
                boolean enabling = !enable && (data & 0x10) > 0;  
                enable = (data & 0x10) > 0; /* bit 4 */
                boolean clearingIFG = dmaIFG && ((data & IFG_MASK) == 0);
                dmaIFG = (data & IFG_MASK) > 0; /* bit 3 */
                dmaIE = (data & 0x04) > 0; /* bit 2 */
                if (DEBUG) {
                    log("DMA Ch." + channelNo + ": conf srcInc: " + srcIncr + " dstInc:" + dstIncr
                        + " en: " + enable + " srcB:" + srcByteMode + " dstB:" + dstByteMode + " lvl: " + dmaLevel +
                        " transMode: " + transferMode + " ie:" + dmaIE + " ifg:" + dmaIFG);
                }
                if (dmaIE && clearingIFG) {                
                        resetDMAIV();
                }
                if (dmaIFG & dmaIE) {
                    triggerInterrupt();
                }
				if (enabling) {
					if ((trigger != null) && (trigger instanceof DMAxv2Trigger) && ((DMAxv2Trigger)trigger).getDMATriggerState(triggerIndex)) {
						trigger(trigger, triggerIndex);
					}
				}
                break;
            case DMAxSAlow:
                sourceAddress &= ~(0xFFFF);
                sourceAddress |= ( data & 0xFFFF);
                currentSourceAddress = sourceAddress;
				log("DMA Ch." + channelNo + " LO sourceAddress : " + Utils.hex(sourceAddress,8) + " : " + Utils.hex(data,8)  );         
                if ((data == 0) && (destinationAddress !=0 ) ) {
					cpu.profiler.printStackTrace(System.out);
					//System.out.println(Thread.currentThread().getStackTrace());
					for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
						System.out.println(ste);
					}
					new Scanner(System.in).nextLine();
				}
                break;
            case DMAxSAhigh:
                sourceAddress &= 0xFFFF;
                sourceAddress |= (( data & 0xFFFF) << 16);
                currentSourceAddress = sourceAddress;
                if (data == 0) {
					cpu.profiler.printStackTrace(System.out);
				}
				log("DMA Ch." + channelNo + " HI sourceAddress : " + Utils.hex(sourceAddress,8) + " : " + Utils.hex(data,8)  );         
                break;
            case DMAxDAlow:
                destinationAddress &= ~(0xFFFF);
                destinationAddress |= ( data & 0xFFFF);
                currentDestinationAddress = destinationAddress;
                if (data == 0) {
					cpu.profiler.printStackTrace(System.out);
				}
				log("DMA Ch." + channelNo + " LO destinationAddress : " + Utils.hex(destinationAddress,8) + " : " + Utils.hex(data,8)  );         
                break;
            case DMAxDAhigh:
                destinationAddress &= (0xFFFF);
                destinationAddress |= (( data & 0xFFFF) << 16);
                currentDestinationAddress = destinationAddress;
                if (data == 0) {
					cpu.profiler.printStackTrace(System.out);
				}
				log("DMA Ch." + channelNo + " HI destinationAddress : " + Utils.hex(destinationAddress,8) + " : " + Utils.hex(data,8)  );         
                break;
            case DMAxSZ:
                size = data;
                storedSize = data;
				log("DMA Ch." + channelNo + " Size : " + Utils.hex(size,8) );         
                break;
            }
            
        }
        
        public int read(int address) {
            switch(address) {
            case DMAxCTL:
                /* set the IFG */
                ctl = (ctl & ~IFG_MASK) | (dmaIFG ? IFG_MASK : 0);
                if (DEBUG) {
                    //log ("DMA"+channelNo+"CTL: 0x" + Utils.hex(ctl,4));
                }
                return ctl;
            case DMAxSAlow:
                return sourceAddress & 0xFFFF;
            case DMAxSAhigh:
                return (sourceAddress >> 16) & 0xFFFF;
            case DMAxDAlow:
                return destinationAddress & 0xFFFF;
            case DMAxDAhigh:
                return (destinationAddress >> 16) & 0xFFFF;
            case DMAxSZ:
                return size;
            }
            logw(WarningType.EXECUTION, "Illegal read of DMA Channel register");
            return 0;
        }
        
        private TimeEvent triggerEvent = new TimeEvent(0, "DMA trigger") {
            public void execute(long t) {
                trigger(trigger, triggerIndex);
            }
        };
        
        
        public void trigger(DMAxv2Trigger trigger, int index) {
            /* perform memory move and possibly clear triggering flag!!! */
            /* NOTE: show config byte/word also !!! */
            if (enable) {
                int data = cpu.currentSegment.read(currentSourceAddress, Memory.AccessMode.BYTE, Memory.AccessType.READ);
                //data = cpu.memory[currentSourceAddress];
                if (DEBUG) 
                    log("DMA ch. " + channelNo + " Triggered transfer from: $" +
                        Utils.hex(currentSourceAddress, 5) + " : 0x" + Utils.hex(data,2) + " " + (data < 32? '.' : (char) data) + " to $" +
                        Utils.hex(currentDestinationAddress, 5) + 
                        " size:" + (storedSize - ( size - 1) )+ "/" + storedSize + " index:" + index);
                // flag already cleared by the memory read above
//                trigger.clearDMAxv2Trigger(index);
                DMAxv2.this.cpu.currentSegment.write(currentDestinationAddress, data, Memory.AccessMode.BYTE);
                
                currentSourceAddress += srcIncr;
                currentDestinationAddress += dstIncr;
                size--;
                if (size == 0) {
                    currentSourceAddress = sourceAddress;
                    currentDestinationAddress = destinationAddress;
                    size = storedSize;
                    if ((transferMode & 0x04) == 0) {
                        enable = false;
                        ctl &= ~0x0010;
                        if (DEBUG) {
                            log("DMA ch. " + channelNo + " EoT: end of transfer with IV: 0x" + Utils.hex(dmaIV,4));
                        }
                    }
                    /* flag interrupt and update interrupt vector */
                    dmaIFG = true;
                    ctl |= IFG_MASK;
                    if (dmaIE) {
                        triggerInterrupt();
                    }
                }
            }
        }

        public String info() {
            return getName() + (enable ? " Enabled " : " Disabled")
                    + "  Index: " + triggerIndex + "  Trigger: " + (trigger==null?trigger:((IOUnit)trigger).getName())
                    + "\n    current source: 0x"
                    + cpu.getAddressAsString(currentSourceAddress)
                    + " destination: 0x"
                    + cpu.getAddressAsString(currentDestinationAddress)
                    + "  size: " + (storedSize - size) + "/" + storedSize;
        }
    }

    private Channel channels[] = new Channel[3];
    private int dmactl0;
    private int dmactl1;
    private int dmactl2;
    private int dmactl3;
    private int dmactl4;
    private int dmaIV;
    

    /* MAX 16 triggers ? */
    private DMAxv2Trigger[] dmaTrigger = new DMAxv2Trigger[32]; 
   private int[] dmaTriggerIndex = new int[32];
    
    public DMAxv2(String id, MSP430Core cpu, int[] memory, int offset, int vector) {
        super(id, cpu, memory, offset);
        channels[0] = new Channel(0, offset + DMA_BLOCK_CHANNEL0);
        channels[1] = new Channel(1, offset + DMA_BLOCK_CHANNEL1);
        channels[2] = new Channel(2, offset + DMA_BLOCK_CHANNEL2);
        this.dmaVector = vector;
    }

    
    public void setDMATrigger(int totindex, DMAxv2Trigger trigger, int tIndex) {
        dmaTrigger[totindex] = trigger;
        dmaTriggerIndex[totindex] = tIndex;
        trigger.setDMA(this);
    }
    
    public void trigger(DMAxv2Trigger trigger, int index) {
        /* could make this a bit and have a bit-pattern if more dma channels but
         * with 3 channels it does not make sense. Optimize later - maybe with
         * flag in DMA triggers so that they now if a channel listens at all.
         */
        for (int i = 0; i < channels.length; i++) {
//            System.out.println("DMA Channel:" + i + " " + channels[i].trigger + " = " + trigger);
            if (channels[i].trigger == trigger && channels[i].triggerIndex == index) {
                cpu.scheduleCycleEvent(channels[i].triggerEvent,0);
            }
        }
    }
    
    //If a channel triggers the interrupt the first thing the ISR handler is:
    //OR checking the IV
    //OR checking the IFG and clearing it
    private void resetDMAIV() {
        if (DEBUG) {
          log(cpu.cycles + ": Clearing IFG for Channel" + ((dmaIV / 2) -1) );
        }
        // Clear interrupt flags!
        channels[(dmaIV / 2) -1].ctl &= ~IFG_MASK;
        channels[(dmaIV / 2) -1].dmaIFG = false;

        /* flag this interrupt off */
        cpu.flagInterrupt(dmaVector, this, false);
        dmaIV = 0;

        for (int i = 0; i < channels.length; i++) {
            if (channels[i].dmaIE && channels[i].dmaIFG) {
                channels[i].triggerInterrupt();
            }
        }      
    }

    
    public void interruptServiced(int vector) {
      if (MSP430Core.debugInterrupts) {
        log(getName() + " >>>> interrupt Serviced for channel: " + (dmaIV/2 - 1) + 
            " at cycles: " + cpu.cycles + " <<<");
      }
    }

    
    public void write(int address, int value, boolean word, long cycles) {
        if (DEBUG) {
            if (address >= offset + DMA_BLOCK_CONTROL + 1 && address < offset + DMA_BLOCK_CHANNEL0) {
                log("DMA debug --- $0x" + Utils.hex(address-(offset + DMA_BLOCK_CONTROL), 5) + ": 0x" + Utils.hex(value, 5) + "--- @" + cpu.getTimeMillis());
            }
            else if (address < offset + DMA_BLOCK_CHANNEL0  || address >= offset + DMA_BLOCK_CHANNEL2) {
                log("DMA debug -+- $0x" + Utils.hex(address-(offset + DMA_BLOCK_CHANNEL2), 5) + ": 0x" + Utils.hex(value, 5));
            }
            else {
                //log("DMA write to: $0x" + Utils.hex(address, 4) + ": 0x" + Utils.hex(value, 4));
            }
        }
        address -= offset;
        switch (address) {
            case DMACTL0:
                /* DMA Control 0 */
                dmactl0 = value;
                channels[0].setTrigger(dmaTrigger[(value >> 0) & 0x1f], dmaTriggerIndex[(value >> 0) & 0x1f]);
                channels[1].setTrigger(dmaTrigger[(value >> 8) & 0x1f], dmaTriggerIndex[(value >> 8) & 0x1f]);
                break;
            case DMACTL1:
                /* DMA Control 1 */
                dmactl1 = value;
                channels[2].setTrigger(dmaTrigger[(value >> 0) & 0x1f], dmaTriggerIndex[(value >> 0) & 0x1f]);
                break;
            case DMACTL2:
                /* DMA Control 2 */
                dmactl2 = value;
                break;
            case DMACTL3:
                /* DMA Control 3 */
                dmactl3 = value;
                break;
            case DMACTL4:
                /* DMA Control 4 */
                dmactl4 = value;
                break;
            case DMAIV:
                /* DMA IV  is READ ONLY*/
                //dmaIV = value;
                resetDMAIV();
                break;
            default:
                /* must be word ??? */
                Channel c = channels[(address - DMA_BLOCK_CHANNEL0) / 0x10];
                c.write(address & 0x0F, value);
        }
    }

    public int read(int address, boolean word, long cycles) {
        if (DEBUG) {
            //log("DMA read from: $0x" + Utils.hex(address, 4));
        }
        
        address -= offset;
        switch (address) {
        case DMACTL0:
            /* DMA Control 0 */
            return dmactl0;
        case DMACTL1:
            /* DMA Control 1 */
            return dmactl1; 
        case DMACTL2:
            /* DMA Control 2 */
            return dmactl2; 
        case DMACTL3:
            /* DMA Control 3 */
            return dmactl3; 
        case DMACTL4:
            /* DMA Control 4 */
            return dmactl4; 
        case DMAIV:
            int val = dmaIV;
            /* DMA IV*/
            resetDMAIV();
            return val; 
        default:
            /* must be word ??? */
            Channel c = channels[(address - DMA_BLOCK_CHANNEL0) / 0x10];
            return c.read(address & 0x0F);
        }
    }

    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append("  DMACTLx 0: 0x" + Utils.hex16(dmactl0) + " 1: 0x" + Utils.hex16(dmactl1) + " 2: 0x" + Utils.hex16(dmactl2) + " 3: 0x" + Utils.hex16(dmactl3) + " 4: 0x" + Utils.hex16(dmactl4) + " iv: 0x" + Utils.hex16(dmaIV));
        for (Channel c : channels) {
            sb.append("\n  ").append(c.info());
        }
        return sb.toString();
    }

}
