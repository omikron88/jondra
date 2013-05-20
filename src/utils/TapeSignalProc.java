/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

/**
 *
 * @author Administrator
 */
public class TapeSignalProc {
    
    private final int init = 128;
    
    private int buff[];
    private int buffsize;
    private int head;
    private int min, max, avg;
    
    public TapeSignalProc(int size) {
        buffsize = size;
        buff = new int[size];
        head = 0;
        for(int n=0; n<buffsize; n++) {
            buff[n] = init;
        }
    }
    
    public boolean addSample(int sample) {
        buff[head] = sample;
        max = init; 
        min = init;
        for(int n=0; n<buffsize; n++) {
            if (buff[head] > max) { 
                max = buff[head];
            }
            if (buff[head] < min) { 
                min = buff[head]; 
            }
            head++;
            if (head==buffsize) { 
                head = 0; 
            }
            
        }
        head++;
        if (head==buffsize) { 
            head = 0; 
        }
            
        avg = (min + max) / 2;
        return (sample > avg);
    }
}
