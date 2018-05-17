# Kafka Streams and Event Sourcing example
Implementation of basic Event Sourcing features using Kafka and Kafka Streams.
The application receives REST requests corresponding to a football match events (like goals, yellow and red cards etc.)
and transforms them into Events. These Events are the origin of simple football statistics and domain snapshots.

## Known issues
Random errors:
- kafka
    ```
    Error while executing topic command : KeeperErrorCode = Session expired for /config/topics/fb-event.match-finished
    WARN Unable to reconnect to ZooKeeper service, session 0x1636e4467c70006 has expired (org.apache.zookeeper.ClientCnxn)
    ERROR org.apache.zookeeper.KeeperException$SessionExpiredException: KeeperErrorCode = Session expired for /config/topics/fb-event.match-finished
       at org.apache.zookeeper.KeeperException.create(KeeperException.java:127)
       at org.apache.zookeeper.KeeperException.create(KeeperException.java:51)
       at kafka.zookeeper.AsyncResponse.maybeThrow(ZooKeeperClient.scala:472)
       at kafka.zk.KafkaZkClient.setOrCreateEntityConfigs(KafkaZkClient.scala:270)
       at kafka.zk.AdminZkClient.createOrUpdateTopicPartitionAssignmentPathInZK(AdminZkClient.scala:99)
       at kafka.zk.AdminZkClient.createTopic(AdminZkClient.scala:56)
       at kafka.admin.TopicCommand$.createTopic(TopicCommand.scala:111)
       at kafka.admin.TopicCommand$.main(TopicCommand.scala:63)
       at kafka.admin.TopicCommand.main(TopicCommand.scala)
     (kafka.admin.TopicCommand$)
    ```
- connect
    ```
    2018-05-17 14:02:01,862 INFO   ||  Kafka Connect started   [org.apache.kafka.connect.runtime.Connect]
    2018-05-17 14:02:05,412 ERROR  ||  Uncaught exception in herder work thread, exiting:    [org.apache.kafka.connect.runtime.distributed.DistributedHerder]
    org.apache.kafka.connect.errors.ConnectException: Error while attempting to create/find topic(s) 'fb.connect.offsets'
       at org.apache.kafka.connect.util.TopicAdmin.createTopics(TopicAdmin.java:247)
       ...
    Caused by: java.util.concurrent.ExecutionException: org.apache.kafka.common.errors.UnknownServerException: KeeperErrorCode = Session expired for /brokers/topics/fb.connect.offsets
       at org.apache.kafka.common.internals.KafkaFutureImpl.wrapAndThrow(KafkaFutureImpl.java:45)
       ...
       ... 11 more
    Caused by: org.apache.kafka.common.errors.UnknownServerException: KeeperErrorCode = Session expired for /brokers/topics/fb.connect.offsets
    2018-05-17 14:02:05,438 INFO   ||  Kafka Connect stopping   [org.apache.kafka.connect.runtime.Connect]
    2018-05-17 14:02:05,438 INFO   ||  Stopping REST server   [org.apache.kafka.connect.runtime.rest.RestServer]
    2018-05-17 14:03:05,472 INFO   ||  Stopped ServerConnector@760a4916{HTTP/1.1}{172.18.0.9:8083}   [org.eclipse.jetty.server.ServerConnector]
    2018-05-17 14:03:05,498 INFO   ||  Stopped o.e.j.s.ServletContextHandler@6be2c838{/,null,UNAVAILABLE}   [org.eclipse.jetty.server.handler.ContextHandler]
    2018-05-17 14:03:32,948 WARN   ||     [org.eclipse.jetty.servlet.ServletHandler]
    javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
    1. java.lang.IllegalStateException: ServiceLocatorImpl(__HK2_Generated_0,1,641403533) has been shut down
    
       at org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:489)
       ...
    Caused by: A MultiException has 1 exceptions.  They are:
    1. java.lang.IllegalStateException: ServiceLocatorImpl(__HK2_Generated_0,1,641403533) has been shut down
    
       at org.jvnet.hk2.internal.FactoryCreator.getFactoryHandle(FactoryCreator.java:106)
       ...
       ... 23 more
    Caused by: java.lang.IllegalStateException: ServiceLocatorImpl(__HK2_Generated_0,1,641403533) has been shut down
       at org.jvnet.hk2.internal.ServiceLocatorImpl.checkState(ServiceLocatorImpl.java:2371)
       ...
       ... 31 more
    2018-05-17 14:03:32,950 INFO   ||  172.18.0.1 - - [1526565722682] "GET /connectors HTTP/1.1" 500 294  90268   [org.apache.kafka.connect.runtime.rest.RestServer]
    2018-05-17 14:03:32,951 WARN   ||  /connectors   [org.eclipse.jetty.server.HttpChannel]
    javax.servlet.ServletException: javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
    1. java.lang.IllegalStateException: ServiceLocatorImpl(__HK2_Generated_0,1,641403533) has been shut down
    
       at org.eclipse.jetty.server.handler.HandlerCollection.handle(HandlerCollection.java:130)
       ...
    ```
