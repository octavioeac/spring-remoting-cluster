package com.proemion.spring.protocol.http;

import java.util.concurrent.TimeUnit;

public class DummyService implements DummyInterface {
  
  @Override
  public void blockingMethod() {
    block();
  }
  
  private void block() {
    try {
      Thread.sleep(TimeUnit.MINUTES.toMillis(5));
    } catch (InterruptedException e) {
      System.out.println("interrupted blocking method");
      //      e.printStackTrace();
    }
  }
  
  @Override
  public boolean heartbeatMethod() {
    return true;
  }
  
  @Override
  public boolean simpleMethod() {
    System.out.println("simpleMethod called");
    return true;
  }
  
  @Override
  public void failingMethod() throws Exception {
    throw new NullPointerException("Catch me if you can");
  }
  
  @Override
  public void heartbeatMethod2() {
    return;
  }
  
  @Override
  public void blockingMethod2() {
    block();
  }
  
}
