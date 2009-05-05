package com.proemion.spring.clustering;

public class ClusteringConfiguration {
  private String codebaseUrl;
  private Class<?> serviceInterface;
  
  public void setServiceInterface(final Class<?> serviceInterface) {
    this.serviceInterface = serviceInterface;
  }
  
  public Class<?> getServiceInterface() {
    return serviceInterface;
  }
  
  public void setCodebaseUrl(final String codebaseUrl) {
    this.codebaseUrl = codebaseUrl;
  }
  
  public String getCodebaseUrl() {
    return codebaseUrl;
  }
}
