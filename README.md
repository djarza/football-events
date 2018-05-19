# Kafka Streams and Event Sourcing example
Implementation of basic Event Sourcing features using Kafka and Kafka Streams.
The application receives REST requests corresponding to a football match events (like goals, yellow and red cards etc.)
and transforms them into Events. These Events are the origin of simple football statistics and domain snapshots.
