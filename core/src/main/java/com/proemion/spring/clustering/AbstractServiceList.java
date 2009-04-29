package com.proemion.spring.clustering;

import org.aopalliance.intercept.MethodInterceptor;

public abstract class AbstractServiceList<T extends AbstractServiceList.AbstractServiceListEntry> implements ServiceList<T>{

  public static abstract class AbstractServiceListEntry implements ServiceList.ServiceListEntry{
    private final String url;
    private volatile boolean connected = false;
    private volatile boolean valid = true;
    private volatile MethodInterceptor connector;
    public AbstractServiceListEntry (String url) {
      this.url = url;
    }
    
    public void invalidate(){
      valid = false;
    }
    public boolean isAlive(){
      return valid && connected;
    }
    
    public boolean isValid(){
      return valid;
    }
    
    public void setConnected(boolean connected){
      this.connected = connected;
    }
    
    /**
     * @param connector the connector to set
     */
    public void setConnector(MethodInterceptor connectorObject) {
      this.connector = connectorObject;
    }
    
    /**
     * @return the connector
     */
    public MethodInterceptor getConnector() {
      return connector;
    }
    
    /**
     * @return the url
     */
    public String getUrl() {
      return url;
    }
    
    @Override
    public int hashCode() {
      return getUrl().hashCode();
    }
    
    @Override
    public boolean equals(Object other){
      if (!(other instanceof ServiceListEntry)) return false;
      return getUrl().equals(((ServiceListEntry)other).getUrl());
    }
    
    @Override
    public String toString() {
      return getUrl()+" ["+ (connected?"connected":"dead")+", "+(valid?"active":"deleted")+"]";
    }
  }
  
}
