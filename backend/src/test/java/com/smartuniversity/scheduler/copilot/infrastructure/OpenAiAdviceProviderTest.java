package com.smartuniversity.scheduler.copilot.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartuniversity.scheduler.copilot.api.CopilotRequest;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiAdviceProviderTest {
    final ObjectMapper mapper=new ObjectMapper();
    final UUID course=UUID.fromString("50000000-0000-4000-8000-000000000001");
    String response(String answer, List<UUID> ids) throws Exception {
        String text=mapper.writeValueAsString(Map.of("answer",answer,"recommendedCourseIds",ids));
        return mapper.writeValueAsString(Map.of("status","completed","output",List.of(Map.of("type","message","content",
                List.of(Map.of("type","output_text","text",text))))));
    }
    OpenAiAdviceProvider provider(RestClient client) { return new OpenAiAdviceProvider(client,mapper,true,"test-only-provider-key","test-model"); }
    @Test void sendsStructuredOutputWithoutStoringConversation() throws Exception {
        var builder=RestClient.builder().baseUrl("https://api.openai.com/v1");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST)).andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer test-only-provider-key"))
                .andExpect(jsonPath("$.store").value(false)).andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.input[0].role").value("user"))
                .andRespond(withSuccess(response("Consider CS101",List.of(course)),MediaType.APPLICATION_JSON));
        var advice=provider(builder.build()).advise("{\"catalog\":[]}",
                new CopilotRequest(UUID.randomUUID(),"Help me choose",List.of(),List.of(),null),Set.of(course));
        assertThat(advice.answer()).isEqualTo("Consider CS101");
        assertThat(advice.courseIds()).containsExactly(course); server.verify();
    }
    @Test void rejectsInventedCourseIdsAndIncompleteOrMalformedResponses() throws Exception {
        var provider=provider(RestClient.create());
        assertThatThrownBy(()->provider.parse(response("Invented",List.of(UUID.randomUUID())),Set.of(course))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->provider.parse("{\"status\":\"incomplete\"}",Set.of(course))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->provider.parse("invalid JSON",Set.of(course))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->provider.parse(response(" ",List.of()),Set.of(course))).isInstanceOf(ApiException.class);
    }
    @Test void disabledOrMissingKeyNeverCallsProvider() {
        var disabled=new OpenAiAdviceProvider(RestClient.create(),mapper,false,"test-key","test-model");
        assertThat(disabled.available()).isFalse();
        assertThat(new OpenAiAdviceProvider(RestClient.create(),mapper,true,"","test-model").available()).isFalse();
        assertThatThrownBy(()->disabled.advise("",new CopilotRequest(UUID.randomUUID(),"Question",List.of(),List.of(),null),Set.of()))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getCode()).isEqualTo("AI_NOT_CONFIGURED"));
    }
    @Test void providerErrorsDoNotExposeUpstreamSecrets() {
        var builder=RestClient.builder().baseUrl("https://api.openai.com/v1");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("sensitive upstream details"));
        assertThatThrownBy(()->provider(builder.build()).advise("{}",
                new CopilotRequest(UUID.randomUUID(),"Question",List.of(),List.of(),null),Set.of(course)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getMessage()).doesNotContain("sensitive","test-only-provider-key"));
        server.verify();
    }
}
