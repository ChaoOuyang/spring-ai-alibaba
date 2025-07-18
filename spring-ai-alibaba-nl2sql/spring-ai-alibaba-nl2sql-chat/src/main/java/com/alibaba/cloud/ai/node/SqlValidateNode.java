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

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.SQL_VALIDATE_EXCEPTION_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_VALIDATE_NODE_OUTPUT;

/**
 * 校验 SQL 语句的语法正确性
 *
 * @deprecated 此节点已废弃，建议使用 SemanticConsistencNode 进行语义一致性校验
 * @author zhangshenghang
 */
@Deprecated
public class SqlValidateNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlValidateNode.class);

	private final ChatClient chatClient;

	private final DbConfig dbConfig;

	private final DbAccessor dbAccessor;

	public SqlValidateNode(ChatClient.Builder chatClientBuilder, DbAccessor dbAccessor, DbConfig dbConfig) {
		this.chatClient = chatClientBuilder.build();
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());
		logger.warn("此节点已废弃，建议使用 SemanticConsistencNode 进行语义一致性校验");

		// 获取SQL语句
		String sql = StateUtils.getStringValue(state, "SQL_GENERATE_OUTPUT");
		logger.info("[{}] 开始验证SQL语句: {}", this.getClass().getSimpleName(), sql);

		Flux<ChatResponse> sqlValidationFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse("开始验证SQL语句..."));
			// 构建查询参数并执行验证
			DbQueryParameter dbQueryParameter = new DbQueryParameter();
			dbQueryParameter.setSql(sql);

			try {
				// 执行SQL验证
				dbAccessor.executeSqlAndReturnObject(dbConfig, dbQueryParameter);
				logger.info("[{}] SQL语法验证通过", this.getClass().getSimpleName());
				emitter.next(ChatResponseUtil.createCustomStatusResponse("SQL语法验证通过."));
				emitter.complete();

			}
			catch (Exception e) {
				// 处理验证失败情况
				String errorMessage = e.getMessage();
				logger.error("[{}] SQL语法验证失败 - 原因: {}", this.getClass().getSimpleName(), errorMessage);
				emitter.next(ChatResponseUtil.createCustomStatusResponse("SQL语法验证失败: " + errorMessage));
				emitter.complete();
			}
		});

		var generator = StreamingChatGenerator.builder()
			.startingNode(this.getClass().getSimpleName())
			.startingState(state)
			.mapResult(response -> {
				DbQueryParameter dbQueryParameter = new DbQueryParameter();
				dbQueryParameter.setSql(sql);
				try {
					dbAccessor.executeSqlAndReturnObject(dbConfig, dbQueryParameter);
					return Map.of(SQL_VALIDATE_NODE_OUTPUT, true);
				}
				catch (Exception e) {
					return Map.of(SQL_VALIDATE_NODE_OUTPUT, false, SQL_VALIDATE_EXCEPTION_OUTPUT, e.getMessage());
				}
			})
			.build(sqlValidationFlux);

		return Map.of(SQL_VALIDATE_NODE_OUTPUT, generator);
	}

}
