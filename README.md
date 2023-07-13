# ClassMediatorCustomHttpClient

This class mediator can be used to invoke a backend service inside the class mediator using the Apache HTTP client. To engage the class mediator you need  to use a custom mediation policy as below and then use the respond mediator.

```<?xml version="1.0" encoding="UTF-8"?>
<sequence xmlns="http://ws.apache.org/ns/synapse" name="custom_policy">
   <class name="org.custom.CustomHttpClient"/>
   <respond/>
</sequence>
