package microservices.book.multiplication.service;

import microservices.book.multiplication.domain.Multiplication;
import microservices.book.multiplication.domain.MultiplicationResultAttempt;
import microservices.book.multiplication.domain.User;
import microservices.book.multiplication.event.EventDispatcher;
import microservices.book.multiplication.event.MultiplicationSolvedEvent;
import microservices.book.multiplication.repository.MultiplicationResultAttemptRepository;
import microservices.book.multiplication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

//import jdk.internal.org.jline.utils.Log;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
class MultiplicationServiceImpl implements MultiplicationService {

  private RandomGeneratorService randomGeneratorService;
  private MultiplicationResultAttemptRepository attemptRepository;
  private UserRepository userRepository;
  private EventDispatcher eventDispatcher;

  @Autowired
  public MultiplicationServiceImpl(final RandomGeneratorService randomGeneratorService,
                                   final MultiplicationResultAttemptRepository attemptRepository,
                                   final UserRepository userRepository,
                                   final EventDispatcher eventDispatcher) {
    this.randomGeneratorService = randomGeneratorService;
    this.attemptRepository = attemptRepository;
    this.userRepository = userRepository;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public Multiplication createRandomMultiplication() {
    int factorA = randomGeneratorService.generateRandomFactor();
    int factorB = randomGeneratorService.generateRandomFactor();
    return new Multiplication(factorA, factorB);
  }

  @Transactional // 이 메소드 실행 시 예외가 발생하면 이벤트 전송이 안 되게 함
  @Override
  public boolean checkAttempt(final MultiplicationResultAttempt attempt) {
    // 해당 닉네임의 사용자가 존재하는지 확인함
    Optional<User> user = userRepository.findByAlias(attempt.getUser().getAlias());

    // 조작된 답안을 방지
    Assert.isTrue(!attempt.isCorrect(), "채점한 상태로 보낼 수 없습니다!!");

    // 답안을 채점
    boolean isCorrect = attempt.getResultAttempt() ==
            attempt.getMultiplication().getFactorA() *
                    attempt.getMultiplication().getFactorB();

    MultiplicationResultAttempt checkedAttempt = new MultiplicationResultAttempt(
            user.orElse(attempt.getUser()),
            attempt.getMultiplication(),
            attempt.getResultAttempt(),
            isCorrect
    );

    // 답안을 저장
    attemptRepository.save(checkedAttempt);

    // 이벤트로 결과를 전송
    log.info("Multiplication Solved Event send!: {}", checkedAttempt);
    eventDispatcher.send(
            new MultiplicationSolvedEvent(checkedAttempt.getId(),
                    checkedAttempt.getUser().getId(),
                    checkedAttempt.isCorrect())
    );

    return isCorrect;
  }


  @Override
  public List<MultiplicationResultAttempt> getStatsForUser(final String userAlias) {
    return attemptRepository.findTop5ByUserAliasOrderByIdDesc(userAlias);
  }

  @Override
  public MultiplicationResultAttempt getResultById(final Long resultId) {
    return attemptRepository.findOne(resultId);
  }


}
