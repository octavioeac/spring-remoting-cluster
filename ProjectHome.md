# Background #
The Spring HttpInvokerProxyFactoryBean creates a proxy and routes every method call on this proxy to a single webserver (There are also similar beans for JMS, RMI and Burlap).

This enables you to forward method calls transparent to a server.

One drawback of this remoting solutions is the lack of failover or load balancing. This is essential when developing server to server communications, or similar critical communications.

# Idea #
A simple framework which
  * provides a replacement for the Spring HttpInvokerProxyFactoryBean
  * is protocol transparent (support for JMS, RMI and Burlap as well)
  * provides fallback strategies
  * enables load balancing across multiple servers
  * uses existing spring functionality

# Goal #
More reliable, robust applications.

# Status #
The project is still under heavy development.
There is currently a working solution, which lacks some features and might not be stable under all conditions.

# Getting Started #
See our [Usage](Usage.md) page