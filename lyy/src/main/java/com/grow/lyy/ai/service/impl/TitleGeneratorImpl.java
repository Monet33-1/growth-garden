package com.grow.lyy.ai.service.impl;


import com.grow.lyy.ai.provider.AIProvider;
import com.grow.lyy.ai.service.TitleGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TitleGeneratorImpl implements TitleGenerator {


    private final AIProvider aiProvider;


    @Override
    public String generateTitle(String content) {


        String prompt = """
                你是一个随笔标题生成助手。

                根据下面的文字生成一个简短、有诗意的标题。

                要求：
                1. 不超过10个字
                2. 不要解释
                3. 只返回标题本身


                随笔内容：
                %s
                """.formatted(content);


        return aiProvider.chat(prompt).trim();

    }
}