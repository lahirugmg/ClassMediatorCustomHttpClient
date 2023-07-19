# ClassMediatorCustomHttpClient

This class mediator can be used to invoke a backend service inside the class mediator using the Apache HTTP client. To engage the class mediator you need  to use a custom mediation policy as below and then use the respond mediator. Make sure to change the backend URL as you need.

<ul>
<li>maxTotal:This parameter represents the maximum total number of HTTP connections that can be open at the same time. It applies to all routes or targets managed by the connection manager. Once this limit is reached, any further requests for new connections will block until an existing connection becomes available or the request times out.</li>
<li>defaultMaxPerRoute:This parameter represents the maximum number of HTTP connections allowed per route or target. Each route corresponds to a unique combination of protocol (HTTP or HTTPS), hostname, and port number. By default, if a specific maximum value is not set for a route, this default value will be used.</li>
<li>connectionTimeout: the time to establish the connection with the remote host</li>
<li>connectionRequestTimeout: This timeout is used to determine how long the client will wait for a connection from the connection manager before giving up.
</li>
<li>socketTimeout: the time waiting for data – after establishing the connection; maximum time of inactivity between two data packets
</li>
</ul>

For example, if maxTotal is set to 100 and defaultMaxPerRoute is set to 20, it means that the connection manager can have a maximum of 100 connections open at any given time, and each route can have up to 20 connections. If there are more routes than the default value, additional connections will be created up to the defaultMaxPerRoute limit per route.

```<?xml version="1.0" encoding="UTF-8"?>
<sequence xmlns="http://ws.apache.org/ns/synapse" name="custom_policy">
   <class name="org.custom.CustomHttpClient">
     <property name="maxTotal" value="10"/>
     <property name="defaultMaxPerRoute" value="20"/>
     <property name="connectionTimeout" value="30000"/>
     <property name="connectionRequestTimeout" value="30000"/>
     <property name="socketTimeout" value="60000"/>
   </class>
   <respond/>
</sequence>
