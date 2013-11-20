package com.apdlv.ilibaba.bt;

public interface SPPStatusListener 
{
    void onConnect(String name, String addr);
    void onDisconnect(String name, String addr);
    void setStatus(boolean connected);
}