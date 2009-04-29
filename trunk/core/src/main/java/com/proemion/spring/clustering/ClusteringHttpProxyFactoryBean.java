package com.proemion.spring.clustering;

/*
 * $Id: FileLegacyBOImpl.java 2921 2008-06-17 14:57:59Z cw1009 $
 * $HeadURL: http://10.10.1.210:8081/svn/pbos/trunk/bos/src/main/java/rm/proemion/bo/FileLegacyBOImpl.java $
 * $Rev: 2921 $
 * $Date: 2008-06-17 16:57:59 +0200 (Tue, 17 Jun 2008) $
 * $Author: cw1009 $
 *
 * Copyright (c) 2005-2008, Proemion GmbH.
 * Developed 2008 by Proemion GmbH.
 * All rights reserved.
 *
 * Use is subject to licence terms.
 *
 * This software uses software developed by the Apache Software
 * Foundation
 * Copyright (c) 1999-2008 The Apache Software Foundation.
 *
 */
import com.proemion.spring.clustering.ServiceList.ServiceListEntry;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enables Clustering via HTTP Remoting
 * 
 */
public class ClusteringHttpProxyFactoryBean extends UrlBasedRemoteAccessor implements FactoryBean, MethodInterceptor, HttpInvokerClientConfiguration, DisposableBean{
  Logger logger = LoggerFactory.getLogger(ClusteringHttpProxyFactoryBean.class);
  // Statics
  private final Pattern httpAddressPattern = Pattern.compile("http://(.+):([0-9]*)/(.+)");
  private long refreshEndpointsMillis = TimeUnit.SECONDS.toMillis(60);
  
  //Connection handling
  private volatile ServiceList<?> serviceList;
  
  //Timer
  private volatile Timer timer;
  
  //Proxy
  private Object serviceProxy;
  private String codebaseUrl;
  
  public class ServiceChecker implements Runnable{
    
    private final ServiceListEntry entry;
    private final Object monitor = new Object();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    public ServiceChecker (ServiceListEntry entry) {
      this.entry = entry;
    }
    
    public boolean check() {
      connected.set(false);
      Thread thread = new Thread(this);
      thread.start();
      synchronized (monitor) {
        try {
          monitor.wait(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        if (thread.isAlive()) thread.interrupt();
      }
      
      return connected.get();
    }
    
    @Override
    public void run() {
      HttpURLConnection connection;
      try {
        connection = (HttpURLConnection) new URL(entry.getUrl()).openConnection();
        connection.setConnectTimeout(1000);
        connection.connect();
        connected.set(true);
      } catch (Exception e) {
        connected.set(false);
        logger.warn("No connection to: {}... ignoring", entry);
        logger.warn("Message was: {}", e.toString());
      }
      synchronized (monitor) {
        monitor.notifyAll();
      }
      
    }
    
  }
  
  protected ServiceListEntry lookupUrl(MethodInvocation invocation) throws RemoteLookupFailureException{
    refreshServicesIfNeededUntilFoundOne();
    return serviceList.getNext(invocation);
  }
  
  protected ServiceListEntry getUrl(MethodInvocation invocation) throws RemoteException{
    return lookupUrl(invocation);
  }
  
  private void refreshServicesIfNeededUntilFoundOne(){
    while(!serviceList.isOneAlive()){
      logger.info("No services alive - refreshing endpoints.");
      refreshServices();
    }
  }
  
  /**
   * Synchronize to prevent too much traffic
   */
  public synchronized void refreshServices(){
    for(ServiceListEntry entry: serviceList.getDeadList()){
      ServiceChecker checker = new ServiceChecker(entry);
      if (checker.check()){
        logger.info("Reactivated {}", entry.getUrl());
        entry.setConnected(true);
      } else {
        logger.warn("Can not connect to " + entry.getUrl()+ " - ignoring.");
      }
    }
  }
  
  
  public String checkUrl(String url) throws IOException{
    Matcher res = httpAddressPattern.matcher(url);
    if (!res.matches()) throw new MalformedURLException("Wrong address syntax - '" + url +"'. Correct syntax is http://host:port/service");
    URLConnection con = new URL(url).openConnection();
    con.setConnectTimeout(1000);
    con.setReadTimeout(1000);
    con.connect();
    return url;
  }
  
  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable{
    return ErrorUtils.executeWithRetry(10, "Invocation failed - retrying...", 200, new ErrorUtils.CallableWithRecovery<Object>(){
      private ServiceListEntry service;
      public void recover() throws Throwable{
        service.setConnected(false);
      }
      
      public Object call() throws Throwable{
        
        service = getUrl(invocation);
        MethodInterceptor bean = getConnector(service);
        
        logger.debug("invoke service at url [{}]", service);
        return bean.invoke(invocation);
      }
    }, RemoteAccessException.class);
  }
  
  
  protected MethodInterceptor getConnector(ServiceListEntry service) {
    if (service.getConnector() == null){ //May need a new connector
      synchronized(service){ //Sync so that no one is working at the same service
        if (service.getConnector() == null) { //Recheck, some other thread may have initialized it already
          HttpInvokerClientInterceptor factoryBean = new HttpInvokerClientInterceptor();
          factoryBean.setBeanClassLoader(getBeanClassLoader());
          factoryBean.setServiceUrl(service.getUrl());
          factoryBean.setCodebaseUrl(getCodebaseUrl());
          factoryBean.setServiceInterface(getServiceInterface());
          factoryBean.afterPropertiesSet();
          service.setConnector(factoryBean);
        }
      }
    }
    
    return service.getConnector();
  }
  
  public void setServiceList(ServiceList serviceList){
    this.serviceList = serviceList;
  }
  
  @Override
  public void afterPropertiesSet(){
    setServiceUrl("legacy");
    super.afterPropertiesSet();
    
    if (getServiceInterface() == null) {
      throw new IllegalArgumentException("Property 'serviceInterface' is required");
    }
    this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
    
    initRefreshTimer();
  }
  
  private void initRefreshTimer(){
    timer = new Timer("Remoting Timer", true);
    timer.schedule(new TimerTask(){
      @Override
      public void run(){
        try{
          refreshServices();
        } catch(Exception e){
          logger.warn("Can not refresh services", e);
        }
      }
    }, refreshEndpointsMillis, refreshEndpointsMillis);
  }
  
  public void destroy() throws Exception{
    timer.cancel();
  }
  
  public void setRefreshEndpointsMillis(int refreshEndpointsMillis){
    this.refreshEndpointsMillis = refreshEndpointsMillis;
  }
  
  public void setCodebaseUrl(String codebaseUrl) {
    this.codebaseUrl = codebaseUrl;
  }
  
  @Override
  public String getCodebaseUrl() {
    return this.codebaseUrl;
  }
  
  public Object getObject() {
    return this.serviceProxy;
  }
  
  @SuppressWarnings("unchecked")
  public Class getObjectType() {
    return getServiceInterface();
  }
  
  public boolean isSingleton() {
    return true;
  }
  
  
//  public static void main(String[] args) {
//    ClusteringHttpProxyFactoryBean httpBean = new ClusteringHttpProxyFactoryBean();
//    RoundRobinServiceList list = new RoundRobinServiceList();
//    httpBean.setServiceList(list);
//    httpBean.refreshServices();
//    list.addUrl("http://intranet:80/index.php?id=180");
//    list.addUrl("http://lalelu");
//    list.addUrl("http://www.google.de");
//    
//    httpBean.refreshServices();
//  }
}