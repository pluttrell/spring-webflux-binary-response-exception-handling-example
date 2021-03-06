package example.api;

import example.acceskeys.AccessKeyValidationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class JsonTestController {

  private final AccessKeyValidationService accessKeyValidationService;

  @GetMapping(
      path = "/json-test",
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE
  )
  Mono<GoodResponse> test(@RequestParam("apiKey") String apiKey, @RequestParam(name = "failFirst", required = false) boolean failFirst) {

    if (failFirst) {
      throw new IllegalArgumentException();
    }

    return Mono.just(apiKey)
        .filterWhen(accessKeyValidationService::isValid)
        .switchIfEmpty(Mono.error(new IllegalAccessException()))
        .then(Mono.just(new GoodResponse("test")));

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
  private class GoodResponse {

    private final String field1;

  }

  @Data
  private class BadResponse {

    private final String developerMessage;

  }

}
