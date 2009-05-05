package com.proemion.spring.clustering.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ClusterMethodInfo {
  public static enum MethodType {NORMAL, TEST}
  public static enum FailureType {RETRY, FAIL, HANDLER}
  
  MethodType type() default MethodType.NORMAL;
  FailureType fail() default FailureType.HANDLER;
  String[] expectedResults() ;
}
