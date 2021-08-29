package microservices.book.multiplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.book.multiplication.domain.Multiplication;
import microservices.book.multiplication.domain.MultiplicationResultAttempt;
import microservices.book.multiplication.domain.User;
import microservices.book.multiplication.service.MultiplicationService;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringRunner.class)
@WebMvcTest(MultiplicationResultAttemptController.class)
public class MultiplicationResultAttemptControllerTest {

  @MockBean
  private MultiplicationService multiplicationService;

  @Autowired
  private MockMvc mvc;

  // 이 객체는 initFields() 메소드를 이용해 자동으로 초기화
  private JacksonTester<MultiplicationResultAttempt> jsonResultAttempt;
  private JacksonTester<List<MultiplicationResultAttempt>> jsonResultAttemptList;

  @Before
  public void setup() {
    JacksonTester.initFields(this, new ObjectMapper());
  }

  @Test
  // 채점 결과가 true 인지 검사하는 메소드
  public void postResultReturnCorrect() throws Exception {
    genericParameterizedTest(true);
  }

  @Test
  // 채점 결과가 false 인지 검사하는 메소드
  public void postResultReturnNotCorrect() throws Exception {
    genericParameterizedTest(false);
  }

  void genericParameterizedTest(final boolean correct) throws Exception {
    // given (지금 서비스를 테스트하는 것이 아님)
	// 채점 결과로 무조건 correct 값을 반환하도록 함
	//  /*
    given(multiplicationService
            .checkAttempt(any(MultiplicationResultAttempt.class)))
            .willReturn(correct);
    //*/
    // 새로운 답안 생성
    User user = new User("john");
    Multiplication multiplication = new Multiplication(50, 70);
    MultiplicationResultAttempt attempt = new MultiplicationResultAttempt(
            user, multiplication, 3500, correct);

    // when
    // 채점된 답안을 얻음
    MockHttpServletResponse response = mvc.perform(
            post("/results").contentType(MediaType.APPLICATION_JSON) // 답안 채점 요청
                    .content(jsonResultAttempt.write(attempt).getJson()))
            .andReturn().getResponse();

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    // 채점 결과가 correct 값과 같은지 검사함
    assertThat(response.getContentAsString()).isEqualTo(
            jsonResultAttempt.write(
                    new MultiplicationResultAttempt(attempt.getUser(),  // correct 값을 담은 새로운 답안 객체 생성
                            attempt.getMultiplication(),
                            attempt.getResultAttempt(),
                            correct) 
            ).getJson());
  }

  @Test
  public void getUserStats() throws Exception {
    // given
    User user = new User("john_doe");
    Multiplication multiplication = new Multiplication(50, 70);
    MultiplicationResultAttempt attempt = new MultiplicationResultAttempt(
            user, multiplication, 3500, true);
    List<MultiplicationResultAttempt> recentAttempts = Lists.newArrayList(attempt, attempt);
    // (1) recentAttempts 리스트를 반환하도록 설정함
    given(multiplicationService
            .getStatsForUser("john_doe"))
            .willReturn(recentAttempts);

    // when
    // (3) 콘트롤러 실행 
    MockHttpServletResponse response = mvc.perform(
            get("/results").param("alias", "john_doe"))
            .andReturn().getResponse();

    // then
    // (3) 리턴 받은 것이 recentAttempts 리스트와 동일한지 확인함
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getContentAsString()).isEqualTo(
            jsonResultAttemptList.write(
                    recentAttempts
            ).getJson());
  }

}