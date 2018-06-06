# Event-Driven microservices with Kafka Streams

Event-Driven microservices codebase applying the idea of [stateful streams processing](https://docs.confluent.io/current/streams/concepts.html#stateful-stream-processing) with [Kafka](https://kafka.apache.org/) and [Kafka Streams](https://kafka.apache.org/documentation/streams/). These blog posts are a great introduction to that concept:
- [The Data Dichotomy: Rethinking the Way We Treat Data and Services](https://www.confluent.io/blog/data-dichotomy-rethinking-the-way-we-treat-data-and-services/)
- [Build Services on a Backbone of Events](https://www.confluent.io/blog/build-services-backbone-events/)
- [Using Apache Kafka as a Scalable, Event-Driven Backbone for Service Architectures](https://www.confluent.io/blog/apache-kafka-for-service-architectures/)
- [Event sourcing using Kafka](https://blog.softwaremill.com/event-sourcing-using-kafka-53dfd72ad45d)

You can think of this project as a more comprehensive supplement to the basic code examples of Kafka Streams. Kafka Streams is used here for many purposes like aggregation, building materialized views, persisting the domain model or just exposing the output of services. Furthermore, there is no traditional database, local state stores instead (PostgreSQL plays a role of legacy system only).

So what is this application doing? It generates a simple football statistics: match scores, team rankings (matches played, won, lost, drawn, points...) and player statistics (goals scored, yellow and red cards). It should be a little more comprehensible than a typical code examples based on orders, payments etc. 


## Architecture

![architecture](docs/architecture.png)

Services:
- __kafka__ & __zookeeper__
- __postgres__ - in the role of an external data source
- __[football-match](football-match/)__ - transforms REST requests into [Events](football-common/src/main/java/org/djar/football/model/event/)
- __connect__ - Kafka Connect service with [Debezium PostgreSQL connector](http://debezium.io/docs/connectors/postgresql/); monitors changes in the database
- __[football-player](football-player/)__ - receives notifications from __connect__ service and creates only a single Event [PlayerStartedCareer](football-common/src/main/java/org/djar/football/model/event/PlayerStartedCareer.java) using [Processor API](https://kafka.apache.org/11/documentation/streams/developer-guide/processor-api.html) (see the [code](football-player/src/main/java/org/djar/football/player/snapshot/DomainUpdater.java))
- __[football-view](football-view/)__ - builds views from Events [Kafka Streams DSL](https://kafka.apache.org/11/documentation/streams/developer-guide/dsl-api.html) (see [below](#events-and-streams))
- __[football-ui](football-ui/)__ - saves the statistics into materialized views (local state stores) and exposes via [REST API](football-ui/src/main/java/org/djar/football/ui/controller/StatisticsController.java)

Additional modules:
- __[football-common](football-common/)__ - contains some code that is shared among microservices (obviously it's not a good practice for production), especially:
    - [Events](football-common/src/main/java/org/djar/football/model/event/) - __[football-match](football-match/)__, __[football-player](football-player/)__, __[football_view](football-view/)__
    - [statistics](football-common/src/main/java/org/djar/football/model/view/) - __[football-view](football-view/)__, __[football-ui](football-ui/)__
- __[football-tests](football-tests/)__ - integration tests


## Events and streams

Each [Event](football-common/src/main/java/org/djar/football/model/event/) represents a state change that occurred to the system.

| Event               | Fields                                                 |
| ------------------- | ------------------------------------------------------ |
| MatchScheduled      | match id, season, home club, away club, date           |
| MatchStarted        | match id, home club, away club                         |
| GoalScored          | goal id, match id, minute, scorer, scored for          |
| CardReceived        | card id, match id, minute, receiver, type (yellow/red) |
| MatchFinished       | match id                                               |
| PlayerStartedCareer | palyer id, name                                        |

These Events are the source for stream processing. In order to determine the result of a match, you must join MatchStarted and GoalScored streams and then count the goals (see the [code](football-view/src/main/java/org/djar/football/view/projection/StatisticsBuilder.java)).


## Kafka topics

![topics](docs/topics.png)


## REST endpoints

REST controllers:
- [Query interface](football-ui/src/main/java/org/djar/football/ui/controller/StatisticsController.java)
- [Command interface](football-match/src/main/java/org/djar/football/match/controller/MatchController.java)


## How to run

- Add kafka bootstrap address to your /etc/hosts:
    ```
    127.0.0.1 kafka
    127.0.0.1 postgres
    127.0.0.1 connect
    127.0.0.1 football-match
    127.0.0.1 football-player
    127.0.0.1 football-view
    127.0.0.1 football-ui
    ```
- Run Kafka and microservices:
    ```
    docker-compose up -d
    ```
- So far, there is no test data or data generator besides a simple [integration test](football-tests/)
