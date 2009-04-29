package com.proemion.spring.clustering;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.Collection;
import java.util.Iterator;

public interface ServiceList<T extends ServiceList.ServiceListEntry> extends Iterable<T> {
  
  public static interface ServiceListEntry{
    public void invalidate();
    public boolean isAlive();
    public boolean isValid();
    public void setConnected(boolean connected);
    
    /**
     * @param connector the connector to set
     * @deprecated Move to AbstractServiceListEntry!
     */
    @Deprecated
    public void setConnector(MethodInterceptor connectorObject);
    
    /**
     * @return the connector
     * @deprecated Move to AbstractServiceListEntry!
     */
    @Deprecated
    public MethodInterceptor getConnector() ;
    
    public String getUrl();
  }
  
  public abstract ServiceListEntry getNext(MethodInvocation invocation);
  
  public abstract Collection<T> getDeadList();
  
  public abstract boolean isOneAlive();
  
  public abstract Iterator<T> iterator();
  
  public abstract void addUrl(String url);
  
  public abstract void removeUrl(String url);
  
}