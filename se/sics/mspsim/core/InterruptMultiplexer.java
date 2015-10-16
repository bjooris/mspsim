package se.sics.mspsim.core;

public class InterruptMultiplexer implements InterruptHandler {

    private final MSP430Core cpu;
    private final int vector;

    int interruptBits;

    public InterruptMultiplexer(MSP430Core cpu, int vector) {
        this.cpu = cpu;
        this.vector = vector;
    }

    public void updateInterrupt(boolean value, int bit) {
        if (value) interruptBits |= 1 << bit;
        else interruptBits &= ~(1 << bit);
        System.out.println("updateInterrupt v:" + vector + " t: " + value + " b: " + bit + " ib:" + interruptBits);
        cpu.flagInterrupt(vector, this, interruptBits > 0);
    }

    
    public void interruptServiced(int vector) {
        /* do nothing??? */
        System.out.println("interruptServiced-------------------------------------------------------------" + vector);
        cpu.flagInterrupt(vector, this, false);
    }

    public String getName() {
        return "InterruptMultiplexer vector " + vector +  " bits: " + interruptBits;
    }

}
