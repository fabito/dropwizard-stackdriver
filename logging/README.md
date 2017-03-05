## 

Registers a new console appender which uses a JsonLayout compatible 
with the Stackdriver [LogEntry](https://cloud.google.com/logging/docs/view/logs_index)


### Configuring

```yaml
logging:
  level: INFO
  loggers:
    my.package: DEBUG
  appenders:
  - type: GKEConsole
    timeZone: UTC
    target: stdout
```

#### TODO:

* find out if its possible to aggregate log entries per request (same way appengine does)