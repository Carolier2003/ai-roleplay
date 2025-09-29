package com.carol.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * Vector Store 配置
 * 配置Redis向量数据库
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.ai.vectorstore.redis.index:character_knowledge_idx}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:ai_roleplay_character}")
    private String keyPrefix;


    @Bean
    public JedisPooled jedisPooled() {
        log.info("初始化 Jedis 连接池");
        log.info("连接 Redis: {}:{}", redisHost, redisPort);
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JedisPooled jedisPooled) {
        log.info("初始化 Redis Vector Store");
        log.info("索引名称: {}", indexName);
        log.info("键前缀: {}", keyPrefix);
        
        try {
            // 使用builder模式配置Redis Vector Store
            RedisVectorStore vectorStore = RedisVectorStore.builder(jedisPooled, embeddingModel)
                    .indexName(indexName)
                    .prefix(keyPrefix)
                    .metadataFields(
                            RedisVectorStore.MetadataField.numeric("character_id"),
                            RedisVectorStore.MetadataField.tag("knowledge_type"),
                            RedisVectorStore.MetadataField.numeric("importance_score")
                    )
                    .initializeSchema(true) // 自动初始化索引
                    .batchingStrategy(new TokenCountBatchingStrategy())
                    .build();
            
            log.info("✅ Redis Vector Store 初始化成功");
            return vectorStore;
            
        } catch (Exception e) {
            log.error("❌ Redis Vector Store 初始化失败", e);
            throw new RuntimeException("Vector Store 初始化失败", e);
        }
    }
}
