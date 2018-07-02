# spring-webflux-binary-response-exception-handling-example

This repo provides an example (with a failing unit test) of `406` errors returned when both of the following occur:

1. Exceptions are returned from the the Reactive stream instead of the controller method itself.
1. When the controller method produces a different ContentType then the `ExceptionHandler`.

This is reproducible using SpringBoot v2.0.3 and v2.1.0-BUILD-SNAPSHOT (as of 7/1/2018).

### Run with

```bash
./gradlew test
```

### Details

Here's the controller:

```java
@RestController
@RequiredArgsConstructor
class BinaryTestController {

  private final AccessKeyValidationService accessKeyValidationService;

  @GetMapping(path = "/binary-test", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  Mono<byte[]> test(@RequestParam("apiKey") String apiKey, @RequestParam(name = "failFirst", required = false) boolean failFirst) {

    if (failFirst) { throw new IllegalArgumentException();}

    return Mono.just(apiKey)
        .filterWhen(accessKeyValidationService::isValid)
        .switchIfEmpty(Mono.error(new IllegalAccessException()))
        .then(Mono.just("test".getBytes(StandardCharsets.UTF_8)));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Mono<BadResponse>> handleIllegalArgumentException() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
    return new ResponseEntity<>(Mono.just(new BadResponse("better luck next time")), headers, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalAccessException.class)
  ResponseEntity<Mono<BadResponse>> handleIllegalAccessException() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
    return new ResponseEntity<>(Mono.just(new BadResponse("access denied")), headers, HttpStatus.FORBIDDEN);
  }

  @Data
  class BadResponse {
    private final String developerMessage;
  }

}
```

The `AccessKeyValidationService` does a blocking lookup, so naturally needs to return a `Mono` and thus needs to be part of the `Mono` returned from the controller:
```java
@Service
class AccessKeyValidationService {

  Mono<Boolean> isValid(String accessKey) {
    return Mono.defer(() -> Mono.just(blockingLookup(accessKey)))
        .subscribeOn(Schedulers.elastic());
  }

  private Boolean blockingLookup(String accessKey) {
    //Some blocking lookup...
    return accessKey.equals("good");
  }
}
```

A valid apiKey, results in proper binary output:
```bash
$ http localhost:8080/binary-test apiKey==good
HTTP/1.1 200 OK
Content-Length: 4
Content-Type: application/octet-stream

test
```

When the exception is thrown before the Reactive Stream, the intended JSON output is returned:
```bash
$ http localhost:8080/binary-test apiKey==good failFirst==true
HTTP/1.1 400 Bad Request
Content-Length: 44
Content-Type: application/json;charset=UTF-8

{
    "developerMessage": "better luck next time"
}
```

But when the stream receives an `IllegalAccessException` error a `406` is returned instead of the defined error handling:
```bash
$ http localhost:8080/binary-test apiKey==bad
HTTP/1.1 406 Not Acceptable
Content-Length: 157
Content-Type: application/json;charset=UTF-8

{
    "error": "Not Acceptable",
    "message": "Could not find acceptable representation",
    "path": "/binary-test",
    "status": 406,
    "timestamp": "2018-06-30T05:23:09.820+0000"
}
```

Unit tests, including one that fails are included.
