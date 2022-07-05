# Recommendation service

Recommendation service is a Demo app to test async programming via Spring webflux
It would suggest 5 books and 5 albums on your search request.

Tech stack:
- Java 11, Gradle
- Spring boot, web-flux, web-client
- Health checks & metrics
- Functional tests

## How to run

```bash
./gradlew bootRun
```

After that, you can get recommendations by
```bash
curl "http://localhost:8080/search?name=Help\!"
curl "http://localhost:8080/search?name=To+kill+a+mockingbird" # Use + or %20 for whitespace
```

To get more than 5 items per system, you can run the program with
```bash
./gradlew -Presponse.limit=10 bootRun
```

## Technical solution

I chose non-blocking, reactive programming because it provides easy concurrency and could separate subsequent calls
execution without any affection of the primary call. Due to I/O bound nature of the application, async programming would be more suitable as our threads
are not blocked by waiting for responses.

Each upstreaming service is limited to respond within 5 seconds. And if it's failed to respond in that time,
or return 4xx or 5xx response code, or failed by any other error downstream service will return an error at `errorMessage` field.
I think such behaviour is more appropriate as it enhances UX:
- user will get an answer within 5 seconds rather than 1 minute
- if one upstream service is failed, the user will get results from the other service and the error message

## Implementation details

- Gradle and Spring boot frameworks provide faster implementation without additional configuration setup.
- Spring webflux and webclient are modern libraries, which provide async programming
- Spring actuate is used as a health and metrics library. Health checks are available at:
  - `curl "http://localhost:8080/actuator/health/ping"` - application is up and running
  - `curl "http://localhost:8080/actuator/health/googleBooks"` - google books API is available (It was not in the assignment, but I decided to implement it)
  - `curl "http://localhost:8080/actuator/health/itunes"` - google books API is available
- Metrics at prometheus format are available in `curl "http://localhost:8080/actuator/prometheus"`. Not only response time, but also default (JVM, logger, webserver)
- Functional tests are using mockwebserver for mocking iTunes and google books responses

## Further development

Currently, we are using open endpoints with a strict rate limiter. If we want to increase RPS to upstreaming services,
we need to authorise our application and pass auth token.

We could also use cache to return data even when the network is down or the upstreaming service is failed to answer.
Cache could contain results for the most popular and/or least recent search requests.
