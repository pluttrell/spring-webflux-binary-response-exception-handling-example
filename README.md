# spring-webflux-binary-response-exception-handling-example

This repo provides an example with failing unit test of 


Using Spring Webflux included with SpringBoot v2.0.3 and v2.1.0-BUILD-SNAPSHOT I'm seeing `406` errors if Exceptions are thrown out of the Reactive stream vs the controller method itself when the controller method produces a different ContentType then the `ExceptionHandler`.

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
```
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
```
$ http localhost:8080/binary-test apiKey==good
HTTP/1.1 200 OK
Content-Length: 4
Content-Type: application/octet-stream

test
```

When I throw an exception before the Reactive Stream, I get the intended JSON output:
```
$ http localhost:8080/binary-test apiKey==good failFirst==true
HTTP/1.1 400 Bad Request
Content-Length: 44
Content-Type: application/json;charset=UTF-8

{
    "developerMessage": "better luck next time"
}
```

But when I trigger the steam to error with an `IllegalAccessException`, I get a `406`:
```
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
