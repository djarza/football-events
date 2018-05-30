# Integration tests

How to run:
1. Add kafka bootstrap address to /etc/hosts:
```
127.0.0.1 kafka
127.0.0.1 postgres
127.0.0.1 connect
127.0.0.1 football-match
127.0.0.1 football-player
127.0.0.1 football-query
```
2. Run integration tests:
```
cd football-tests
mvn test
```
