package microservices.book.multiplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import microservices.book.multiplication.domain.Multiplication;
import microservices.book.multiplication.domain.MultiplicationResultAttempt;
import microservices.book.multiplication.domain.User;
import microservices.book.multiplication.service.MultiplicationService;
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

import static microservices.book.multiplication.controller.MultiplicationResultAttemptController.ResultResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringRunner.class)
@WebMvcTest(MultiplicationResultAttemptController.class)
public class MultiplicationResultAttemptControllerTest {

  @MockBean
  private MultiplicationService multiplicationService;

  @Autowired
  private MockMvc mvc;

  // 이 객체는 initFields() 메소드를 이용해 자동으로 초기화
  private JacksonTester<MultiplicationResultAttempt> jsonResult;
  private JacksonTester<ResultResponse> jsonResponse;

  @Before
  public void setup() {
    JacksonTester.initFields(this, new ObjectMapper());
  }

  @Test
  public void postResultReturnCorrect() throws Exception {
    genericParameterizedTest(true);
  }

  @Test
  public void postResultReturnNotCorrect() throws Exception {
    genericParameterizedTest(false);
  }

  void genericParameterizedTest(final boolean correct) throws Exception {
    // given (지금 서비스를 테스트하는 것이 아님, MultiplicationResultAttemptController가 잘 동작하는 지만 테스트함)
    given(multiplicationService
            .checkAttempt(any(MultiplicationResultAttempt.class)))
            .willReturn(correct); // 무조건 correct 값을 리턴하는 것으로 했음
    
    User user = new User("john");
    Multiplication multiplication = new Multiplication(50, 70);
    MultiplicationResultAttempt attempt = new MultiplicationResultAttempt(
            user, multiplication, 3600); // attempt 생성

    // when
    MockHttpServletResponse response = mvc.perform(
            post("/results").contentType(MediaType.APPLICATION_JSON)
                    .content(jsonResult.write(attempt).getJson()))  // attempt을 내용으로 해서 /results 에 POST 요청을 함 (무조건 correct 값이 리턴됨: given 절 참조할 것)
            .andReturn().getResponse(); 

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getContentAsString()).isEqualTo(
            jsonResponse.write(new ResultResponse(correct)).getJson());   // 결과가 true 또는 false 인지 확인
  }

}