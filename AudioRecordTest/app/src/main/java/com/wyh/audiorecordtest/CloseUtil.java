package com.wyh.audiorecordtest;

import java.io.Closeable;
import java.io.IOException;

public class CloseUtil {
    public static void close(Closeable c){
        if(c!=null){
            try{
                c.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
