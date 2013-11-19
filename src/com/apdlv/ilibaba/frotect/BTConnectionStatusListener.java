package com.apdlv.ilibaba.frotect;

public interface BTConnectionStatusListener 
{
    void onConnect(String name, String addr);
    void onDisconnect(String name, String addr);
}