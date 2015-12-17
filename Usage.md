# Contents #



# Introduction #

The clustering framework consists of four parts:
  * a ServiceList which holds status informations about the services and implements the load balancing
  * a ProtocolHandler which implements the specific protocol
  * a FailureHandler which implements failover strategies
  * the RemoteClusteringProxyFactoryBean which handles the method invocations and the rest

# Using Maven (preferred) #

  1. Get Maven and a Java runtime 5.0 or higher.
  1. Add the repository to your repository list: (http://spring-remoting-cluster.googlecode.com/svn/maven_repo/)
  1. add these dependcies to your maven project:
```
      <!-- This is needed for the annotations -->
      <dependency>
        <groupId>com.proemion.spring.cluster</groupId>
        <artifactId>cluster-api</artifactId>
        <version>0.2.1-M2</version>
      </dependency>
      
      <!-- This is needed for the real clustering -->
      <dependency>
        <groupId>com.proemion.spring.cluster</groupId>
        <artifactId>cluster-core</artifactId>
        <version>0.2.1-M2</version>
      </dependency>
```
  1. use it
  1. give us some feedback

# ~~Using the Binaries~~ #

  1. ~~Get a Java runtime 5 or higher.~~
  1. ~~Download the latest build from [here](http://code.google.com/p/spring-remoting-cluster/downloads/list)~~
  1. ~~extract the archive and add both jars to your classpath~~
  1. ~~use it~~
  1. ~~give us some feedback~~

# Building from Source #

  1. Get a Java SDK 5 or higher, a subversion client and maven.
  1. Checkout the [source code](http://code.google.com/p/spring-remoting-cluster/source/checkout)
  1. run ` mvn clean install `
> > Currently there's a bug when you try to build it the first time. Just run a single `mvn -N install` before
  1. add the resulting jars to the classpath or add the maven dependencies
  1. use it
  1. give us some feedback

# Sample #
```

<!-- generic clustering configuration -->
<bean name="clusterConfig" class="com.proemion.spring.clustering.ClusteringConfiguration">
  <!-- put your interface here -->
  <property name="serviceInterface" value="my.Interface" />
</bean>

<!-- setup a round-robin service list with different uris/urls -->
<bean name="serviceList" class="com.proemion.spring.clustering.algo.roundrobin.RoundRobinServiceList">
  <!-- separate different URIs with spaces -->
  <property name="serviceURIs" value="http://host1:port1/path1 http://host2:port2/path2" />
</bean>                                                                                   

<!-- setup a http invoker handler which does the normal spring remoting via http -->
<bean name="protoHandler" class="com.proemion.spring.clustering.protocol.http.HttpInvokerHandler">
  <property name="configuration" ref="clusterConfig"/>
</bean>                                                                                   

<!-- setup a paranoid failure handler (marks every failing service as "dead" for some time) -->
<bean name="failHandler" class="com.proemion.spring.clustering.fail.ParanoidFailureHandler">
  <property name="protocolHandler" ref="protoHandler"/>
  <property name="serviceList" ref="serviceList"/>
</bean>                                                                                   

<!-- setup the final clustering bean -->
<bean name="httpInvokerProxy" class="com.proemion.spring.clustering.RemoteClusteringProxyFactoryBean">   
  <property name="configuration" ref="clusterConfig" />
  <property name="serviceList" ref="serviceList" />                                        
  <property name="failureHandler" ref="failHandler" />                                        
  <property name="protocolHandler" ref="protoHandler" />                                        
</bean>
```