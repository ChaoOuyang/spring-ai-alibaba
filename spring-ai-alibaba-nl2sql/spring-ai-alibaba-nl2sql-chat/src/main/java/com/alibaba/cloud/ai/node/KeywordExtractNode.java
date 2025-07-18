/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 关键词、实体、时间等信息抽取，为后续 Schema 召回做准备
 *
 * @author zhangshenghang
 */
public class KeywordExtractNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(KeywordExtractNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public KeywordExtractNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService) {
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取输入，优先使用重写后的查询，否则使用原始输入
		String input = StateUtils.getStringValue(state, QUERY_REWRITE_NODE_OUTPUT,
				StateUtils.getStringValue(state, INPUT_KEY));

		Flux<ChatResponse> keywordExtractionFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse("开始提取关键词..."));

			// 提取证据
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在提取证据..."));
			List<String> evidences = baseNl2SqlService.extractEvidences(input);
			emitter.next(ChatResponseUtil
				.createCustomStatusResponse("提取的证据: " + evidences.stream().collect(Collectors.joining(","))));
			logger.info("[{}] 提取结果 - 证据: {}", this.getClass().getSimpleName(), evidences);

			// 提取关键词
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在提取关键词..."));
			List<String> keywords = baseNl2SqlService.extractKeywords(input, evidences);
			emitter.next(ChatResponseUtil
				.createCustomStatusResponse("提取的关键词: " + keywords.stream().collect(Collectors.joining(","))));
			logger.info("[{}] 提取结果 - 关键词: {}", this.getClass().getSimpleName(), keywords);

			emitter.next(ChatResponseUtil.createCustomStatusResponse("关键词提取完成."));
			emitter.complete();
		});

		var generator = StreamingChatGenerator.builder()
			.startingNode(this.getClass().getSimpleName())
			.startingState(state)
			.mapResult(response -> {
				List<String> evidences = baseNl2SqlService.extractEvidences(input);
				List<String> keywords = baseNl2SqlService.extractKeywords(input, evidences);
				logger.info("[{}] 提取结果 - 证据: {}, 关键词: {}", this.getClass().getSimpleName(), evidences, keywords);
				return Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, keywords, EVIDENCES, evidences, RESULT, keywords);
			})
			.build(keywordExtractionFlux);

		return Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, generator);
	}

}
