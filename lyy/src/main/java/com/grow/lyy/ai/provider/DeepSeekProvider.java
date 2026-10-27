package com.grow.lyy.ai.provider;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.lyy.common.config.AIProperties;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class DeepSeekProvider implements AIProvider {


    private final AIProperties aiProperties;


    private final ObjectMapper objectMapper = new ObjectMapper();


    private final OkHttpClient client = new OkHttpClient();



    @Override
    public String chat(String prompt) {



        try {


            Map<String, Object> requestBody = Map.of(

                    "model",
                    aiProperties.getModel(),


                    "messages",
                    List.of(
                            Map.of(
                                    "role",
                                    "user",

                                    "content",
                                    prompt
                            )
                    )

            );


            String json =
                    objectMapper.writeValueAsString(
                            requestBody
                    );


            Request request =
                    new Request.Builder()

                            .url(
                                    aiProperties.getBaseUrl()
                                            + "/chat/completions"
                            )

                            .addHeader(
                                    "Authorization",
                                    "Bearer "
                                            + aiProperties.getApiKey()
                            )

                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )

                            .post(
                                    RequestBody.create(
                                            json,
                                            MediaType.parse(
                                                    "application/json"
                                            )
                                    )
                            )

                            .build();



            try(Response response = client.newCall(request).execute()){


                if(!response.isSuccessful()){

                    throw new RuntimeException(
                            "DeepSeek调用失败:"
                                    + response.code()
                    );

                }


                String responseBody =
                        response.body().string();



                JsonNode root =
                        objectMapper.readTree(
                                responseBody
                        );


                return root
                        .path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();

            }


        } catch (IOException e) {

            throw new RuntimeException(
                    "DeepSeek请求异常",
                    e
            );
        }
    }
}