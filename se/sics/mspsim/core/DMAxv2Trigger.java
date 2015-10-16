package se.sics.mspsim.core;

public interface DMAxv2Trigger {
    public void setDMA(DMAxv2 dma);
    public boolean getDMATriggerState(int index);
    public void clearDMATrigger(int index);
}
