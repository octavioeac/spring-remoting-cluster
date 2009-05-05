package com.proemion.spring.clustering;

import org.aopalliance.intercept.MethodInvocation;

import java.net.URI;

public interface RemoteService {
  
  public ProtocolDefinition getProtocolDefinition();
  
  public void setProtocolDefinition(ProtocolDefinition protocolDefinition);
  
  public FailureDefinition getFailureDefinition();
  
  public void setFailureDefinition(FailureDefinition failureDefinition);
  
  public URI getURI();
  
  public boolean isActive();
  
  public boolean isDeleted();
  
  public void setActive(boolean active);
  
  public void delete();
  
  public void abandonInvocation(MethodInvocation invocation);
}
