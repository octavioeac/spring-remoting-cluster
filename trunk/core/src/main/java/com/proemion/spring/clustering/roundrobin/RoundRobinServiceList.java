package com.proemion.spring.clustering.roundrobin;


import com.proemion.spring.clustering.AbstractServiceList;
import com.proemion.spring.clustering.AbstractServiceList.AbstractServiceListEntry;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread safe class, holding the current state of the services
 * @author su1007
 *
 */
public class RoundRobinServiceList extends AbstractServiceList<RoundRobinServiceList.RoundRobinServiceEntry>{
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  public class RoundRobinServiceEntry extends AbstractServiceListEntry{
    
    public RoundRobinServiceEntry (String serviceUrl) {
      super(serviceUrl);
    }
    
    @Override
    public void invalidate() {
      super.invalidate();
      refresh(this);
    }
    
    @Override
    public void setConnected(boolean connected) {
      super.setConnected(connected);
      refresh(this);
    }
  }
  private final List<RoundRobinServiceEntry> fullList;
  
  //Concurrent Collections holding "dead" and "alive" services
  private final Set<RoundRobinServiceEntry> deadSet = new CopyOnWriteArraySet<RoundRobinServiceEntry>();
  private final Set<RoundRobinServiceEntry> aliveSet = new CopyOnWriteArraySet<RoundRobinServiceEntry>();
  
  //Volatile attributes, use read lock for non-atomic operations, write lock to set
  private volatile List<RoundRobinServiceEntry> orderedAliveList = new ArrayList<RoundRobinServiceEntry>();
  private volatile int aliveServicesCount;
  
  //Locks
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
  private final Lock read = rwLock.readLock();
  private final Lock write = rwLock.writeLock();
  
  //invocation counter to
  private final AtomicInteger invocationCount = new AtomicInteger(0);
  
  public RoundRobinServiceList(){
    fullList = new CopyOnWriteArrayList<RoundRobinServiceEntry>();
  }
  public RoundRobinServiceList(List<String> serviceUrls){
    this();
    for (String urlString : serviceUrls) {
      addUrl(urlString);
    }
  }
  
  private void refresh(RoundRobinServiceEntry entry) {
    write.lock();
    try {
      if (entry.isAlive()){
        deadSet.remove(entry);
        aliveSet.add(entry);
        logger.info("alive "+entry.getUrl());
      } else {
        aliveSet.remove(entry);
        
        if (entry.isValid()){
          deadSet.add(entry);
          logger.info("dead "+entry.getUrl());
        } else {
          deadSet.remove(entry);
          fullList.remove(entry);
          logger.info("removed "+entry.getUrl());
        }
      }
      ArrayList<RoundRobinServiceEntry> newAliveList = new ArrayList<RoundRobinServiceEntry>(aliveSet);
      orderedAliveList = Collections.unmodifiableList(newAliveList);
      aliveServicesCount = orderedAliveList.size();
      invocationCount.set(aliveServicesCount-1);
    } finally {
      write.unlock();
    }
  }
  
  @Override
  public RoundRobinServiceEntry getNext(MethodInvocation invocation){
    read.lock();
    try{
      int pointer = invocationCount.incrementAndGet();
      pointer = pointer % aliveServicesCount;
      return orderedAliveList.get(pointer);
    } finally {
      read.unlock();
    }
  }
  
  /* (non-Javadoc)
   * @see rm.proemion.commons.spring.ServiceList#getDeadList()
   */
  public Collection<RoundRobinServiceEntry> getDeadList(){
    return Collections.unmodifiableCollection(deadSet);
  }
  
  /* (non-Javadoc)
   * @see rm.proemion.commons.spring.ServiceList#isOneAlive()
   */
  public boolean isOneAlive() {
    return aliveServicesCount > 0;
  }
  
  /* (non-Javadoc)
   * @see rm.proemion.commons.spring.ServiceList#iterator()
   */
  @Override
  public Iterator<RoundRobinServiceEntry> iterator() {
    return Collections.unmodifiableCollection(fullList).iterator();
  }
  
  public void setUrls(String urls){
    if (!fullList.isEmpty()) throw new IllegalStateException("urls are already set; reset not possible");
    String[] serviceUrls = urls.split("[\\s,;]");
    
    
    write.lock();
    try{
      for (String url : serviceUrls) {
        RoundRobinServiceEntry entry = new RoundRobinServiceEntry(url);
        fullList.add(entry);
        refresh(entry);
      }
    }finally {
      write.unlock();
    }
  }
  
  public void addUrl(String url){
    RoundRobinServiceEntry entry = new RoundRobinServiceEntry(url);
    
    if (fullList.contains(entry)) return;
    write.lock();
    try{
      fullList.add(entry);
      refresh(entry);
    }finally {
      write.unlock();
    }
  }
  
  /* (non-Javadoc)
   * @see rm.proemion.commons.spring.ServiceList#removeUrl(java.lang.String)
   */
  public void removeUrl(String url) {
    RoundRobinServiceEntry entry = new RoundRobinServiceEntry(url);
    
    if (!fullList.contains(entry)) return;
    
    write.lock();
    try{
      for (RoundRobinServiceEntry anEntry : fullList) {
        if (anEntry.equals(entry)){
          anEntry.invalidate();
        }
      }
    }finally {
      write.unlock();
    }
  }
}
