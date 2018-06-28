package example.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JsonTestControllerTests {


  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private WebTestClient webTestClient;

  @Test
  void testFailFirst() {

    webTestClient.get()
        .uri("/json-test?apiKey=good&failFirst=true")
        .exchange()
        .expectStatus().isBadRequest()
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("better luck next time");

  }

  @Test
  void testBadApiKey() {

    webTestClient.get()
        .uri("/json-test?apiKey=bad")
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("access denied");

  }

  @Test
  void testGoodResponse() {

    webTestClient.get()
        .uri("/json-test?apiKey=good")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.field1").isEqualTo("test");

  }

}