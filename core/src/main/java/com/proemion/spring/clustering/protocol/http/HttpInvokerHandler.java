package com.proemion.spring.clustering.protocol.http;

import com.proemion.spring.clustering.ClusteringConfiguration;
import com.proemion.spring.clustering.InvocationResult;
import com.proemion.spring.clustering.ProtocolDefinition;
import com.proemion.spring.clustering.ProtocolHandler;
import com.proemion.spring.clustering.RemoteService;
import com.proemion.spring.clustering.InvocationResult.ResultType;
import com.proemion.spring.clustering.annotations.ClusterMethodInfo;
import com.proemion.spring.clustering.annotations.ClusterMethodInfo.MethodType;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpInvokerClientInterceptor;
import org.springframework.security.util.SimpleMethodInvocation;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class HttpInvokerHandler implements ProtocolHandler, BeanClassLoaderAware, InitializingBean{
  
  Logger logger = LoggerFactory.getLogger(getClass());
  
  private class HttpServiceDefinition implements ProtocolDefinition{
    
    private final URI uri;
    private final MethodInterceptor interceptor;
    public HttpServiceDefinition (final URI uri, final MethodInterceptor interceptor) {
      this.uri = uri;
      this.interceptor = interceptor;
    }
    
    @Override
    public URI getURI() {
      return uri;
    }
    
    public MethodInterceptor getInterceptor() {
      return interceptor;
    }
    
  }
  
  private ClusteringConfiguration configuration;
  private ClassLoader beanClassLoader;
  private volatile List<Method> testMethods;
  
  public HttpInvokerHandler () {
  }
  
  @Override
  public InvocationResult invoke (final RemoteService service, final MethodInvocation invocation) {
    logger.debug("call to {}, routing to {}",invocation.getMethod().getName(), service);
    HttpServiceDefinition definition = getServiceDefinition(service);
    InvocationResult result;
    MethodInterceptor interceptor = (definition).getInterceptor();
    if (interceptor == null) {
      throw new NullPointerException("No interceptor found! "+definition);
    }
    try {
      Object returnValue = interceptor.invoke(invocation);
      result = new InvocationResult(ResultType.SERVER_METHOD_RETURNED, returnValue);
    } catch (RemoteAccessException e) {
      result = new InvocationResult(ResultType.REMOTING_ERROR, e);
    } catch (Throwable e) {
      result = new InvocationResult(ResultType.SERVER_METHOD_EXCEPTION, e);
    }
    
    return result;
  }
  
  private HttpServiceDefinition getServiceDefinition(final RemoteService service) {
    if (service.getProtocolDefinition() != null) {
      return (HttpServiceDefinition) service.getProtocolDefinition();
    }
    URI uri = service.getURI();
    HttpInvokerClientInterceptor clientInterceptor = new HttpInvokerClientInterceptor();
    clientInterceptor.setBeanClassLoader(beanClassLoader);
    clientInterceptor.setServiceUrl(uri.toString());
    clientInterceptor.setCodebaseUrl(configuration.getCodebaseUrl());
    clientInterceptor.setServiceInterface(configuration.getServiceInterface());
    clientInterceptor.afterPropertiesSet();
    
    HttpServiceDefinition definition = new HttpServiceDefinition(uri, clientInterceptor);
    service.setProtocolDefinition(definition);
    return definition;
  }
  
  @Override
  public void setBeanClassLoader(final ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }
  
  public void setConfiguration(final ClusteringConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public ClusteringConfiguration getConfiguration() {
    return configuration;
  }
  
  @Override
  public boolean testConnection(final RemoteService service) {
    HttpServiceDefinition definition = getServiceDefinition(service);
    boolean testConnection;
    MethodInterceptor interceptor = (definition).getInterceptor();
    if (testMethods.isEmpty()) {
      testConnection = fallbackTests(definition);
    } else {
      testConnection = true;
      for (Method method : testMethods) {
        testConnection = testConnection && testMethod(interceptor, method);
      }
    }
    return testConnection;
  }
  
  private boolean fallbackTests(final ProtocolDefinition definition) {
    HttpURLConnection connection;
    try {
      connection = (HttpURLConnection) definition.getURI().toURL().openConnection();
      connection.setConnectTimeout(1000);
      connection.connect();
      return true;
    } catch (Exception e) {
      logger.warn("No connection to: {}... ignoring", definition);
      logger.warn("Message was: {}", e.toString());
      return false;
    }
  }
  
  public boolean testMethod(final MethodInterceptor interceptor, final Method method){
    logger.debug("testing {}, routing to {}",method.getName(), interceptor);
    MethodInvocation invocation = new SimpleMethodInvocation(null,method, new Object[0]);
    try {
      ClusterMethodInfo info = method.getAnnotation(ClusterMethodInfo.class);
      Object result = interceptor.invoke(invocation);
      
      if (info.expectedResults().length == 0) {
        return true;
      }
      for (String expected : info.expectedResults()) {
        if (result instanceof Boolean) {
          Boolean.valueOf(expected.toString()).equals(result);
        }
        if (expected.equals(String.valueOf(result.toString()))) {
          return true;
        }
        
      }
    } catch (Throwable e) {
      logger.warn("error while testing connection {}",(Object)e);
    }
    return false;
  }
  
  @Override
  public void afterPropertiesSet() throws Exception {
    ClusteringConfiguration config = getConfiguration();
    if (config == null) {
      throw new IllegalArgumentException("No configuration set");
    }
    Class<?> serviceInterface = config.getServiceInterface();
    Method[] methods = serviceInterface.getMethods();
    List<Method> tests = new ArrayList<Method>();
    for(Method method : methods) {
      //      logger.debug("Method {} has ClusterConfig? {}", method, (method.getAnnotation(ClusterMethodInfo.class) != null));
      if (method.getParameterTypes().length == 0) {
        ClusterMethodInfo info = method.getAnnotation(ClusterMethodInfo.class);
        if ((info != null) && (info.type() == MethodType.TEST)) {
          tests.add(method);
        }
      }
    }
    if (tests.isEmpty()) {
      logger.warn("No test methods given!");
    }
    this.testMethods = tests;
  }
  
}
