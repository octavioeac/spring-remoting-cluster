package com.proemion.spring.clustering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class ErrorUtils {
  private static final Logger logger = LoggerFactory.getLogger(ErrorUtils.class);
  
  public static <T> T executeWithRetry(int retryCount, String errorMessage, int sleepMillis, Callable<T> callable, Class<?>... dontIgnoreExceptions) throws Throwable {
    Set<Class<?>> dontIgnoreSet = convertToSet(dontIgnoreExceptions);
    Throwable lastException = null;
    
    for (int tryCount = 0; tryCount < retryCount; tryCount++) {
      try {
        return callable.call();
      } catch (Throwable e) {
        
        if (!dontIgnoreSet.isEmpty() && !contains(dontIgnoreSet, e)){
          throw e;
        }
        
        lastException = e;
        logger.warn("{} Cause: {}", errorMessage, e.getMessage());
        //                logger.info("Sleeping for {} millis", sleepMillis);
        //                sleep(sleepMillis);
        logger.info("Retrying...");
        if (callable instanceof CallableWithRecovery) {
          try {
            ((CallableWithRecovery<?>) callable).recover();
          } catch (Throwable throwable) {
            logger.warn("Recovery action failed.");
          }
        }
      }
    }
    throw lastException;
  }
  
  private static boolean contains(Set<Class<?>> dontIgnoreSet, Throwable e){
    for(Class<?> c: dontIgnoreSet){
      if (c.isInstance(e)) return true;
    }
    return false;
    
  }
  
  private static <T> Set<T> convertToSet(T... objects){
    Set<T> res = new HashSet<T>();
    res.addAll(Arrays.asList(objects));
    return res;
  }
  
  public interface Callable<T> {
    T call() throws Throwable;
  }
  
  public interface CallableWithRecovery<T> extends Callable<T> {
    void recover() throws Throwable;
  }
}