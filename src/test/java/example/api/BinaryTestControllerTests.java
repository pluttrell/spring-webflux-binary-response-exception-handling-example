package example.api;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BinaryTestControllerTests {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private WebTestClient webTestClient;

  @Test
  void testFailFirst() {

    webTestClient.get()
        .uri("/binary-test?apiKey=good&failFirst=true")
        .exchange()
        .expectStatus().isBadRequest()
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("better luck next time");

  }

  @Test
  void testBadApiKey() {

    webTestClient.get()
        .uri("/binary-test?apiKey=bad")
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("access denied");

  }

  @Test
  void testGoodResponse() {

    webTestClient.get()
        .uri("/binary-test?apiKey=good")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
        .expectBody(byte[].class).isEqualTo("test".getBytes(StandardCharsets.UTF_8));

  }

}