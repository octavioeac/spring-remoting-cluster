package com.proemion.spring.clustering.fail;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.testng.Assert.fail;

import com.proemion.spring.clustering.ProtocolHandler;
import com.proemion.spring.clustering.RemoteService;
import com.proemion.spring.clustering.ServiceList;

import org.aopalliance.intercept.MethodInvocation;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("boxing")
public class ParanoidFailureHandlerTest {
  
  private class Setup {
    private ServiceList serviceList;
    private RemoteService aService1;
    private RemoteService aService2;
    private List<RemoteService> services;
    private ProtocolHandler protoHandler;
    private ParanoidFailureHandler failureHandler;
    
    public void reset() {
      EasyMock.reset(serviceList, aService1, aService2, protoHandler);
    }
    
    public void replay() {
      EasyMock.replay(serviceList, aService1, aService2, protoHandler);
    }
    
    public void verify() {
      EasyMock.verify(serviceList, aService1, aService2, protoHandler);
    }
  }
  
  public Setup setUp() throws Exception{
    Setup setup = new Setup();
    setup.serviceList = createMock(ServiceList.class);
    
    setup.aService1 = createMock("service1", RemoteService.class);
    setup.aService2 = createMock("service2", RemoteService.class);
    setup.services = new ArrayList<RemoteService>();
    setup.services.add(setup.aService1);
    setup.services.add(setup.aService2);
    
    setup.protoHandler = createMock(ProtocolHandler.class);
    
    setup.failureHandler = new ParanoidFailureHandler();
    setup.failureHandler.setProtocolHandler(setup.protoHandler);
    setup.failureHandler.setServiceList(setup.serviceList);
    setup.failureHandler.setReactivationTime(60000000);
    setup.failureHandler.setTestTimeout(2000);
    setup.failureHandler.afterPropertiesSet();
    
    return setup;
  }
  
  @Test
  public void testForceReactivations() throws Exception {
    Setup setup = setUp();
    setup.reset();
    expect(setup.serviceList.isOneAlive())
    .andReturn(Boolean.TRUE).anyTimes();
    expect(setup.serviceList.iterator()).andReturn(setup.services.iterator()).once();
    expect(setup.protoHandler.testConnection(same(setup.aService1))).andReturn(Boolean.TRUE).once();
    expect(setup.protoHandler.testConnection(same(setup.aService2))).andReturn(Boolean.FALSE).once();
    setup.aService1.setActive(true);
    setup.aService2.setActive(false);
    
    
    setup.replay();
    setup.failureHandler.forceReactivations();
    setup.verify();
  }
  
  @Test
  public void testFailedExecution() throws Exception {
    Setup setup = setUp();
    setup.reset();
    
    setup.aService2.setActive(false);
    
    
    setup.replay();
    setup.failureHandler.failedInvocation(setup.aService2, createMock(MethodInvocation.class));
    setup.verify();
  }
  
  @Test
  public void timedoutExecution() throws Exception {
    Setup setup = setUp();
    setup.reset();
    
    setup.aService2.setActive(false);
    
    
    setup.replay();
    setup.failureHandler.timedOutInvocation(setup.aService2, createMock(MethodInvocation.class));
    setup.verify();
  }
  
  @Test
  public void testForceReactivationFail() throws Exception {
    final Setup setup = setUp();
    setup.reset();
    setup.failureHandler.setMaxRetryCount(10);
    expect(setup.serviceList.isOneAlive()).andReturn(Boolean.FALSE).anyTimes();
    expect(setup.serviceList.iterator()).andAnswer(new IAnswer<Iterator<RemoteService>>(){
      @Override
      public Iterator<RemoteService> answer() throws Throwable {
        return setup.services.iterator();
      }
    }).times(10);
    expect(setup.protoHandler.testConnection(same(setup.aService1))).andReturn(Boolean.FALSE).times(10);
    expect(setup.protoHandler.testConnection(same(setup.aService2))).andReturn(Boolean.FALSE).times(10);
    setup.aService1.setActive(false);
    expectLastCall().times(10);
    setup.aService2.setActive(false);
    expectLastCall().times(10);
    
    
    setup.replay();
    try {
      setup.failureHandler.forceReactivations();
      fail("No Exception thrown");
    }catch (IllegalStateException e) {
      setup.verify();
    } catch (Throwable e) {
      fail("Expected IllegalStateException", e);
    }
  }
  
}
