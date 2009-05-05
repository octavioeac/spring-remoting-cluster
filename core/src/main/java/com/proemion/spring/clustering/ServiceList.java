package com.proemion.spring.clustering;


import org.aopalliance.intercept.MethodInvocation;

import java.net.URISyntaxException;

public interface ServiceList extends Iterable<RemoteService> {
  
  public abstract boolean isOneAlive();
  
  public abstract void addUri(String url) throws URISyntaxException;
  
  public abstract void removeUri(String url) throws URISyntaxException;
  
  public abstract Iterable<? extends RemoteService> getDeadServices();
  
  public abstract RemoteService claimInvocation(MethodInvocation invocation);
  
}