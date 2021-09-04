package microservices.book.multiplication.service;

import microservices.book.multiplication.domain.Multiplication;
import microservices.book.multiplication.domain.MultiplicationResultAttempt;
import microservices.book.multiplication.domain.User;
import microservices.book.multiplication.event.EventDispatcher;
import microservices.book.multiplication.event.MultiplicationSolvedEvent;
import microservices.book.multiplication.repository.MultiplicationResultAttemptRepository;
import microservices.book.multiplication.repository.UserRepository;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class MultiplicationServiceImplTest {

  private MultiplicationServiceImpl multiplicationServiceImpl;

  @Mock
  private RandomGeneratorService randomGeneratorService;

  @Mock
  private MultiplicationResultAttemptRepository attemptRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private EventDispatcher eventDispatcher; // 이벤트 디스패처

  @Before
  public void setUp() {
    // initMocks 를 호출해 Mockito 가 어노테이션을 처리하도록 지시
    MockitoAnnotations.initMocks(this);
    multiplicationServiceImpl = new MultiplicationServiceImpl(randomGeneratorService, attemptRepository,
            userRepository, eventDispatcher);
  }

  @Test
  public void createRandomMultiplicationTest() {
    // given (randomGeneratorService 가 처음에 50, 나중에 30을 반환하도록 설정)
    given(randomGeneratorService.generateRandomFactor()).willReturn(50, 30);

    // when
    Multiplication multiplication = multiplicationServiceImpl.createRandomMultiplication();

    // then
    assertThat(multiplication.getFactorA()).isEqualTo(50);
    assertThat(multiplication.getFactorB()).isEqualTo(30);
  }

  @Test
  public void checkCorrectAttemptTest() {
    // given
    Multiplication multiplication = new Multiplication(50, 60);
    User user = new User("john_doe");
    MultiplicationResultAttempt attempt = new MultiplicationResultAttempt(
            user, multiplication, 3000, false);
    MultiplicationResultAttempt verifiedAttempt = new MultiplicationResultAttempt(
            user, multiplication, 3000, true);
    // 이벤트 객체 생성
    MultiplicationSolvedEvent event = new MultiplicationSolvedEvent(attempt.getId(),
            attempt.getUser().getId(), true);
    given(userRepository.findByAlias("john_doe")).willReturn(Optional.empty());

    // when
    boolean attemptResult = multiplicationServiceImpl.checkAttempt(attempt);

    // then
    assertThat(attemptResult).isTrue();
    verify(attemptRepository).save(verifiedAttempt);
    verify(eventDispatcher).send(eq(event)); // 디스패처를 이용해서 이벤트 전송함
  }

  @Test
  public void checkWrongAttemptTest() {
    // given
    Multiplication multiplication = new Multiplication(50, 60);
    User user = new User("john_doe");
    MultiplicationResultAttempt attempt = new MultiplicationResultAttempt(
            user, multiplication, 3010, false);
    // 이벤트 객체 생성
    MultiplicationSolvedEvent event = new MultiplicationSolvedEvent(attempt.getId(),
            attempt.getUser().getId(), false);
    given(userRepository.findByAlias("john_doe")).willReturn(Optional.empty());

    // when
    boolean attemptResult = multiplicationServiceImpl.checkAttempt(attempt);

    // then
    assertThat(attemptResult).isFalse();
    verify(attemptRepository).save(attempt);
    verify(eventDispatcher).send(eq(event)); // 디스패처를 이용해서 이벤트 전송함
  }

  @Test
  public void retrieveStatsTest() {
    // given
    Multiplication multiplication = new Multiplication(50, 60);
    User user = new User("john_doe");
    MultiplicationResultAttempt attempt1 = new MultiplicationResultAttempt(
            user, multiplication, 3010, false);
    MultiplicationResultAttempt attempt2 = new MultiplicationResultAttempt(
            user, multiplication, 3051, false);
    List<MultiplicationResultAttempt> latestAttempts = Lists.newArrayList(attempt1, attempt2);
    given(userRepository.findByAlias("john_doe")).willReturn(Optional.empty());
    given(attemptRepository.findTop5ByUserAliasOrderByIdDesc("john_doe"))
            .willReturn(latestAttempts);

    // when
    List<MultiplicationResultAttempt> latestAttemptsResult =
            multiplicationServiceImpl.getStatsForUser("john_doe");

    // then
    assertThat(latestAttemptsResult).isEqualTo(latestAttempts);
  }
}