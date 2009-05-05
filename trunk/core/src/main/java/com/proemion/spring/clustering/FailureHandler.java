package com.proemion.spring.clustering;


import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.RemoteAccessException;

public interface FailureHandler {
  void failedInvocation(RemoteService service, MethodInvocation invocation) throws RemoteAccessException;
  
  void timedOutInvocation(RemoteService service, MethodInvocation invocation) throws RemoteAccessException;
  
  void stateOk(RemoteService service);
  
  void forceReactivations();
}
