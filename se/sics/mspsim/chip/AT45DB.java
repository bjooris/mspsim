/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
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
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 * AT45DB SPI Flash Simulator
 *
 * Author  : Matt Thompson <matt.thompson@lochisle.com>
 * Created : Fri May 30 2008
 * Updated : $Date:  $
 *           $Revision: $
 */

package se.sics.mspsim.chip;
import java.io.IOException;
import se.sics.mspsim.core.*;

public abstract class AT45DB extends Chip implements USARTListener {

  public static final boolean DEBUG = false;

  public static final int PAGE_SIZE = 264;
  public static final int NUM_PAGES = 2048;
  public static final int SIZE_BYTES = PAGE_SIZE * NUM_PAGES;

  /** Read Commands - Datasheet Table 15-1 */
  public static final int PAGE_READ = 0xD2;
  public static final int CONTINUOUS_ARRAY_READ_LEGACY = 0xE8;
  public static final int CONTINUOUS_ARRAY_READ_HF = 0x0B;			// 66MHz
  public static final int CONTINUOUS_ARRAY_READ_LF = 0x03;			// 33MHz
  public static final int BUFFER1_READ_LF = 0xD1;
  public static final int BUFFER2_READ_LF = 0xD3;
  public static final int BUFFER1_READ = 0xD4;
  public static final int BUFFER2_READ = 0xD6;

  /** Program and Erase Commands - Datasheet Table 15-2 */
  public static final int BUFFER1_WRITE = 0x84;
  public static final int BUFFER2_WRITE = 0x87;
  public static final int BUFFER1_TO_PAGE_ERASE = 0x83;
  public static final int BUFFER2_TO_PAGE_ERASE = 0x86;
  public static final int BUFFER1_TO_PAGE = 0x88;
  public static final int BUFFER2_TO_PAGE = 0x89;
  public static final int PAGE_ERASE = 0x81;
  public static final int BLOCK_ERASE = 0x50;
  public static final int SECTOR_ERASE = 0x7C;
  // These chip erases opcodes are synonymous
  public static final int CHIP_ERASE = 0xC7;
  public static final int CHIP_ERASE1 = 0x94;
  public static final int CHIP_ERASE2 = 0x80;
  public static final int CHIP_ERASE3 = 0x9A;
  public static final int PAGE_PROGRAM_THROUGH_BUFFER1 = 0x82;
  public static final int PAGE_PROGRAM_THROUGH_BUFFER2 = 0x85;
  /** End of Program and Erase Command opcodes */

  /** Protection and Security Commands - Datasheet Table 15-3 */
  /** Additional Commands - Datasheet Table 15-4 */
  public static final int PAGE_TO_BUFFER1 = 0x53;
  public static final int PAGE_TO_BUFFER2 = 0x55;
  public static final int PAGE_TO_BUFFER1_COMPARE = 0x60;
  public static final int PAGE_TO_BUFFER2_COMPARE = 0x61;
  public static final int AUTO_PAGE_REWRITE_BUFFER1 = 0x58;
  public static final int AUTO_PAGE_REWRITE_BUFFER2 = 0x59;
  public static final int DEEP_POWER_DOWN = 0xB9;
  public static final int RESUME_DEEP_POWER_DOWN = 0xAB;
  public static final int STATUS_REGISTER_READ = 0xD7;
  public static final int READ_DEVICE_ID = 0x9F;
  /** Legacy Commands - Datasheet Table 15-5 */

  // Status register bitmasks
  public static final int STATUS_RDY = (1<<7);
  public static final int STATUS_COMP = (1<<6);
  public static final int STATUS_DENSITY = 0x3C;
  public static final int STATUS_PROTECT = (1<<1);
  public static final int STATUS_PAGE_SIZE = 1;


  private static final int STATE_RESET = 0;
  private static final int STATE_IDLE = 1;
  private static final int READ_ADDRESS = 2;
  private int state = STATE_RESET;
  private int next_state = STATE_RESET;


  private boolean Reset; // Reset Pin
  private boolean chipSelect; // CS Pin

  private int pos;

  // AT45 Status Register byte
  private int status = 0x1C | STATUS_RDY;	// AT45DB041 has bits 5-2 set to 0111 - density bits

  private int pageAddress;
  private int bufferAddress;
  private int dummy=0;	// Number of dummy bytes following command



  // AT45 has two page sized RAM buffers
  private byte[] buffer1 = new byte[PAGE_SIZE];
  private byte[] buffer2 = new byte[PAGE_SIZE];

  private TimeEvent writeEvent = new TimeEvent(0) {
    public void execute(long t) {
      setReady(true);
    }};

    private MSP430Core cpu;

    public AT45DB(MSP430Core cpu) {
      this.cpu = cpu;
    }

    private void setReady(boolean ready) {
      if(ready == true)
        status |= STATUS_RDY;
      else
        status &= ~STATUS_RDY;
    }

    public void dataReceived(USART source, int data) {
      int buf_num = 1;

      if (chipSelect) {
        //if (DEBUG) {
        //  System.out.println("AT45DB: byte received: " + data);
        //}

        switch(state) {

        case READ_ADDRESS:
          pos++;
          if(pos == 1) {
            pageAddress = ((data & 0xF) << 7);
          }else if (pos == 2) {
            pageAddress |= ((data & 0xFE) >> 1);
            bufferAddress = ((data & 1) << 9); 
          }else if (pos == 3) {
            bufferAddress |= data;

            if(DEBUG)
              System.out.println("AT45DB: Address - PA[10-0]: " + pageAddress + " BA[8-0]: " + bufferAddress);

            if(dummy == 0) {
              if(DEBUG) System.out.println("AT45DB: State " + state + " -> " + next_state);
              state = next_state;
            }
          }else{
            if(--dummy == 0) {
              if(DEBUG) System.out.println("AT45DB: State " + state + " -> " + next_state);
              state = next_state;
            }
          }
          source.byteReceived(0);
          break;

        case BUFFER1_READ:
        case BUFFER2_READ:
          // Return bytes from the RAM buffer
          buf_num = (state == BUFFER1_READ ? 1 : 2);
          source.byteReceived(readBuffer(buf_num, bufferAddress++));
          if(bufferAddress >= PAGE_SIZE)
            System.out.println("AT45DB: ERROR: Buffer Read past buffer size: " + bufferAddress);
          break;

        case BUFFER1_WRITE:
        case BUFFER2_WRITE:
          buf_num = (state == BUFFER1_WRITE ? 1 : 2);
          writeBuffer(buf_num, bufferAddress++, data);
          if(bufferAddress >= PAGE_SIZE)
            System.out.println("AT45DB: ERROR: Buffer Write past buffer size: " + bufferAddress);
          source.byteReceived(0);
          break;

        case STATUS_REGISTER_READ:
          // Chip select false will transition state, as the status register can be
          // polled by clocking data on SI until CS is false
          source.byteReceived(status);
          break;

        case STATE_RESET:
        case STATE_IDLE:
          // data is a command byte
          switch(data) {

          case BUFFER1_TO_PAGE_ERASE:
          case BUFFER2_TO_PAGE_ERASE:
            if(DEBUG)
              System.out.println("AT45DB: Buffer" + (data == BUFFER1_TO_PAGE_ERASE ? "1" : "2") + " to Page with Erase Command");
            pos = 0;
            state = READ_ADDRESS;
            next_state = data;
            dummy = 0;
            setReady(false);
            source.byteReceived(0);
            break;

          case BUFFER1_READ:
          case BUFFER2_READ:
            if(DEBUG)
              System.out.println("AT45DB: Read Buffer Command " + (data == BUFFER1_READ ? "Buffer1" : "Buffer2"));
            pos = 0;
            state = READ_ADDRESS;
            next_state = data;
            dummy = 1;
            setReady(false);
            source.byteReceived(0);
            break;

          case BUFFER1_WRITE:
          case BUFFER2_WRITE:
            if(DEBUG)
              System.out.println("AT45DB: Write Buffer Command " + (data == BUFFER1_WRITE ? "Buffer1" : "Buffer2"));
            pos = 0;
            state = READ_ADDRESS;
            next_state = data;
            dummy = 0;
            setReady(false);
            source.byteReceived(0);
            break;

          case PAGE_TO_BUFFER1:
          case PAGE_TO_BUFFER2:
            if(DEBUG)
              System.out.println("AT45DB: Page To Buffer " + (data == PAGE_TO_BUFFER1 ? "1" : "2") + " Command");
            pos = 0;
            state = READ_ADDRESS;
            next_state = data;
            dummy = 0;
            setReady(false);
            source.byteReceived(0);
            break;

          case STATUS_REGISTER_READ:
            if(DEBUG) System.out.println("AT45DB: Read status register command.  status: " + status);
            state = STATUS_REGISTER_READ;
            source.byteReceived(0);
            break;
          default:
            System.out.println("AT45DB: WARNING: Command not implemented: " + data);
          source.byteReceived(0);
          break;
          }
          break;
        default:
          source.byteReceived(0);
        break;
        }
      }
    }

    private int readBuffer(int num, int address) {
      //if(DEBUG) {
      //  System.out.println("AT45DB: Reading RAM Buffer" + num + " Address: " + Integer.toHexString(address));
      //}
      if(num == 1)
        return buffer1[address & 0x1ff];
      else
        return buffer2[address & 0x1ff];
    }

    private void writeBuffer(int num, int address, int data) {
      //if(DEBUG) {
      //	  System.out.println("AT45DB: Writing RAM Buffer" + num + " Address: " + Integer.toHexString(address) + " Data: " + data);
      //}
      if(num == 1)
        buffer1[address & 0x1ff] = (byte)data;
      else
        buffer2[address & 0x1ff] = (byte)data;
    }

    public void setReset(boolean reset) {
      Reset = reset;
      if(Reset == true)
        state = STATE_RESET;
      if(DEBUG) {
        System.out.println("AT45DB: Reset: " + Reset);
      }
    }
    public void setChipSelect(boolean select) {
      chipSelect = select;
      if(chipSelect == false) {
        switch(state) {

        case BUFFER1_TO_PAGE_ERASE:
        case BUFFER2_TO_PAGE_ERASE:
          bufferToPage((state == BUFFER1_TO_PAGE_ERASE ? 1 : 2));
          setReady(true);
          break;

        case PAGE_TO_BUFFER1:
        case PAGE_TO_BUFFER2:
          pageToBuffer((state == PAGE_TO_BUFFER1 ? 1 : 2));
          setReady(true);
          break;

        default:
          setReady(true);
        break;
        }

        state = STATE_IDLE;
      }

      if(DEBUG) {
        System.out.println("AT45DB: Chip Select: " + chipSelect);
      }
    }

    private void bufferToPage(int buf) {
      try {
        seek(pageAddress * PAGE_SIZE);
        if(buf == 1)
          write(buffer1);
        else
          write(buffer2);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void pageToBuffer(int buf) {
      try {
        seek(pageAddress * PAGE_SIZE);
        if(buf == 1)
          read(buffer1);
        else
          read(buffer2);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public int getModeMax() {
      return 0;
    }

    public String getName() {
      return "AT45DB: external flash";
    }

    public abstract void seek(long pos) throws IOException;
    public abstract int read(byte[] b) throws IOException;
    public abstract void write(byte[] b) throws IOException;

} // AT45DB