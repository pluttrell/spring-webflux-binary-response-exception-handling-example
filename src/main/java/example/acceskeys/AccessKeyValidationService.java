package example.acceskeys;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AccessKeyValidationService {

  public Mono<Boolean> isValid(String accessKey) {
    return Mono.defer(() -> Mono.just(blockingLookup(accessKey)))
        .subscribeOn(Schedulers.elastic());
  }

  private Boolean blockingLookup(String accessKey) {
    //Some blocking lookup...
    return accessKey.equals("good");
  }

}