/**
 * Copyright (c) 2011, Swedish Institute of Computer Science.
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
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson
 */

package se.sics.mspsim.config;

import java.util.ArrayList;
import se.sics.mspsim.core.ClockSystem;
import se.sics.mspsim.core.DMAxv2;
import se.sics.mspsim.core.USCI;
import static se.sics.mspsim.core.GenericUSCI.RXIFG;
import se.sics.mspsim.core.GenericUSCI;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.InterruptMultiplexer;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Multiplier32;
import se.sics.mspsim.core.PMM;
import se.sics.mspsim.core.SysReg;
import se.sics.mspsim.core.Timer;
import static se.sics.mspsim.core.Timer.SRC_ACLK;
import static se.sics.mspsim.core.Timer.SRC_CAOUT;
import static se.sics.mspsim.core.Timer.SRC_GND;
import static se.sics.mspsim.core.Timer.SRC_PORT;
import static se.sics.mspsim.core.Timer.SRC_SMCLK;
import static se.sics.mspsim.core.Timer.SRC_VCC;
import se.sics.mspsim.core.UnifiedClockSystem;
import se.sics.mspsim.util.Utils;

public class MSP430f5437Config extends MSP430Config {

    // NOTE: this MCU also needs to configure 
    // - positions of all timers (A0, A1, B)
    // - memory configuration
    // - 
    public static final String portConfig[] = {
            "P1=200,IN 00,OUT 02,DIR 04,REN 06,DS 08,SEL 0A,IV_L 0E,IV_H 0F,IES 18,IE 1A,IFG 1C",
            "P2=200,IN 01,OUT 03,DIR 05,REN 07,DS 09,SEL 0B,IV_L 1E,IV_H 1F,IES 19,IE 1B,IFG 1D",
            "P3=220,IN 00,OUT 02,DIR 04,REN 06,DS 08,SEL 0A",
            "P4=220,IN 01,OUT 03,DIR 05,REN 07,DS 09,SEL 0B",
            "P5=240,IN 00,OUT 02,DIR 04,REN 06,DS 08,SEL 0A",
            "P6=240,IN 01,OUT 03,DIR 05,REN 07,DS 09,SEL 0B",
            "P7=260,IN 00,OUT 02,DIR 04,REN 06,DS 08,SEL 0A",
            "P8=260,IN 01,OUT 03,DIR 05,REN 07,DS 09,SEL 0B",
            "P9=280,IN 00,OUT 02,DIR 04,REN 06,DS 08,SEL 0A",
            "P:=280,IN 01,OUT 03,DIR 05,REN 07,DS 09,SEL 0B",
            };
    
    
    public static final int[] TIMERMAP_A0 = new int[] {
      SRC_PORT + 0x10,  SRC_ACLK,           SRC_SMCLK,      SRC_PORT + 0x10,    // Timer
      SRC_PORT + 0x11,  SRC_PORT + 0x80,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x12,  SRC_PORT + 0x81,    SRC_GND,        SRC_VCC,            // Cap 1
      SRC_PORT + 0x13,  SRC_PORT + 0x82,    SRC_GND,        SRC_VCC,            // Cap 2
      SRC_PORT + 0x14,  SRC_PORT + 0x83,    SRC_GND,        SRC_VCC,            // Cap 3
      SRC_PORT + 0x15,  SRC_PORT + 0x84,    SRC_GND,        SRC_VCC,            // Cap 4
    };

    public static final int[] TIMERMAP_A1 = new int[] {
      SRC_PORT + 0x20,  SRC_ACLK,           SRC_SMCLK,      SRC_PORT + 0x20,    // Timer
      SRC_PORT + 0x21,  SRC_PORT + 0x85,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x22,  SRC_PORT + 0x86,    SRC_GND,        SRC_VCC,            // Cap 1
      SRC_PORT + 0x23,  SRC_PORT + 0x73,    SRC_GND,        SRC_VCC,            // Cap 2
    };
    
    public static final int[] TIMERMAP_B0 = new int[] {
      SRC_PORT + 0x47,  SRC_ACLK,           SRC_SMCLK,      SRC_PORT + 0x47,    // Timer
      SRC_PORT + 0x40,  SRC_PORT + 0x40,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x41,  SRC_PORT + 0x41,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x42,  SRC_PORT + 0x42,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x43,  SRC_PORT + 0x43,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x44,  SRC_PORT + 0x44,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x45,  SRC_PORT + 0x45,    SRC_GND,        SRC_VCC,            // Cap 0
      SRC_PORT + 0x46,  SRC_ACLK,           SRC_GND,        SRC_VCC,            // Cap 0
    };
    
    public MSP430f5437Config() {
        /* 64 vectors for the MSP430f54xx series */
        maxInterruptVector = 63;
        MSP430XArch = true;
        flashControllerOffset = 0x140;
        sfrOffset = 0x100;
        
        /* configuration for the timers - need to set-up new source maps!!! */
        TimerConfig timerA0 = new TimerConfig(54, 53, 5, 0x340, TIMERMAP_A0, "TimerA0", 0x340 + 0x2e);
        TimerConfig timerA1 = new TimerConfig(49, 48, 3, 0x380, TIMERMAP_A1, "TimerA1", 0x380 + 0x2e);
        TimerConfig timerB0 = new TimerConfig(60, 59, 7, 0x3C0, TIMERMAP_B0, "TimerB0",  0x3C0 + 0x2e);
        timerConfig = new TimerConfig[] {timerA0, timerA1, timerB0};
        
        uartConfig = new UARTConfig[] {
                new UARTConfig("USCIA0", 57, 0x5c0),
                new UARTConfig("USCIB0", 56, 0x5e0),
                new UARTConfig("USCIA1", 46, 0x600),
                new UARTConfig("USCIB1", 45, 0x620),
                new UARTConfig("USCIA2", 52, 0x640),
                new UARTConfig("USCIB2", 51, 0x660),
                new UARTConfig("USCIA3", 44, 0x680),
                new UARTConfig("USCIB3", 43, 0x6a0)
        };
        
        /* configure memory */
        infoMemConfig(0x1800, 128 * 4);
        mainFlashConfig(0x5c00, 256 * 1024);
        ramConfig(0x1c00, 16 * 1024);
        ioMemSize(0x800); /* 2 KB of IO Memory */
        
        watchdogOffset = 0x15c;
        // bsl, IO, etc at a later stage...
    }

    public int setup(MSP430Core cpu, ArrayList<IOUnit> ioUnits) {
    
        Multiplier32 mp = new Multiplier32(cpu, cpu.memory, 0x4c0);
        cpu.setIORange(0x4c0, 0x2e, mp);

        /* this code should be slightly more generic... and be somewhere else... */
        for (int i = 0, n = uartConfig.length; i < n; i++) {
            GenericUSCI usci = new GenericUSCI(cpu, i, cpu.memory, this);
            /* setup 0 - 1f as IO addresses */
            cpu.setIORange(uartConfig[i].offset, 0x20, usci);
//            System.out.println("Adding IOUnit USCI: " + usci.getName());
            ioUnits.add(usci);
        }
        IOPort last = null;
        ioUnits.add(last = IOPort.parseIOPort(cpu, 47, portConfig[0], last));
        ioUnits.add(last = IOPort.parseIOPort(cpu, 42, portConfig[1], last));
        
        for (int i = 2; i < portConfig.length; i++) {
            ioUnits.add(last = IOPort.parseIOPort(cpu, 0, portConfig[i], last));
        }

        /* XXX: Stub IO units: Sysreg and PMM */
        SysReg sysreg = new SysReg(cpu, cpu.memory);
        cpu.setIORange(SysReg.ADDRESS, SysReg.SIZE, sysreg);
        ioUnits.add(sysreg);

        PMM pmm = new PMM(cpu, cpu.memory, 0x120);
        cpu.setIORange(0x120, PMM.SIZE, pmm);
        ioUnits.add(pmm);

        
        DMAxv2 dma = new DMAxv2("dma", cpu, cpu.memory, 0x500, 50);
        cpu.setIORange(0x500, 0x40, dma);

        /* configure the DMA */
        
        // Commented out some stuff for RM090 - Line 136 in GenericUSCI (dma should be enabled first before using it)
        
        //~ dma.setDMATrigger(DMAxv2.USCIA0RX, (GenericUSCI)(cpu.getIOUnit("USCIA0")), GenericUSCI.RXIFG);
        //~ dma.setDMATrigger(DMAxv2.USCIA0TX, (GenericUSCI)(cpu.getIOUnit("USCIA0")), GenericUSCI.TXIFG);
        //~ dma.setDMATrigger(DMAxv2.USCIA1RX, (GenericUSCI)(cpu.getIOUnit("USCIA1")), GenericUSCI.RXIFG);
        //~ dma.setDMATrigger(DMAxv2.USCIA1TX, (GenericUSCI)(cpu.getIOUnit("USCIA1")), GenericUSCI.TXIFG);
        dma.setDMATrigger(DMAxv2.USCIB0RX, (GenericUSCI)(cpu.getIOUnit("USCIB0")), GenericUSCI.RXIFG);
        dma.setDMATrigger(DMAxv2.USCIB0TX, (GenericUSCI)(cpu.getIOUnit("USCIB0")), GenericUSCI.TXIFG);
        //~ dma.setDMATrigger(DMAxv2.USCIB1RX, (GenericUSCI)(cpu.getIOUnit("USCIB1")), GenericUSCI.RXIFG);
        //~ dma.setDMATrigger(DMAxv2.USCIB1TX, (GenericUSCI)(cpu.getIOUnit("USCIB1")), GenericUSCI.TXIFG);
        //dma.setInterruptMultiplexer(new InterruptMultiplexer(cpu, 50));
        ioUnits.add(dma);        
        
        return portConfig.length + uartConfig.length;
    }

    @Override
    public String getAddressAsString(int addr) {
        return Utils.hex20(addr);
    }

    @Override
    public ClockSystem createClockSystem(MSP430Core cpu, int[] memory, Timer[] timers) {
        return new UnifiedClockSystem(cpu, memory, 0, timers);
    }

}
