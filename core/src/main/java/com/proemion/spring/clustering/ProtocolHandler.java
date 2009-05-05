package com.proemion.spring.clustering;


import org.aopalliance.intercept.MethodInvocation;


public interface ProtocolHandler {
  
  InvocationResult invoke(RemoteService service, MethodInvocation invocation);
  
  boolean testConnection(RemoteService service);
  
}
