package com.proemion.spring.clustering;

public class InvocationResult {
  public static enum ResultType{
    REMOTING_ERROR,
    REMOTING_TIMEOUT,
    SERVER_METHOD_EXCEPTION,
    SERVER_METHOD_RETURNED,
  }
  private final ResultType resultType;
  private final Object result;
  
  public InvocationResult (final ResultType resultType, final Object result) {
    this.resultType = resultType;
    this.result = result;
    
  }
  
  public ResultType getResultType() {
    return resultType;
  }
  
  public Object getResult() {
    return result;
  }
}
