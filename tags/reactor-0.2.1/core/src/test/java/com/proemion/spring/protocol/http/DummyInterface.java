package com.proemion.spring.protocol.http;

import com.proemion.spring.clustering.annotations.ClusterMethodInfo;
import com.proemion.spring.clustering.annotations.ClusterMethodInfo.MethodType;

public interface DummyInterface {
  
  boolean simpleMethod();
  
  void failingMethod() throws Exception;
  
  @ClusterMethodInfo(type=MethodType.HEARTBEAT, expectedResults="true")
  boolean heartbeatMethod();
  
  @ClusterMethodInfo(type=MethodType.HEARTBEAT)
  void heartbeatMethod2();
  
  void blockingMethod();
  
  @ClusterMethodInfo(type=MethodType.NORMAL, timeout=500)
  void blockingMethod2();
  
}
