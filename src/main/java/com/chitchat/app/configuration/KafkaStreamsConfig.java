package com.chitchat.app.configuration;

import com.chitchat.app.entity.enums.KafkaTopic;
import com.chitchat.app.util.AppConstants;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, AppConstants.KAFKA_STREAMS_APP_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public StreamsBuilderFactoryBeanConfigurer streamsConfigurer() {
        return fb -> {
            Map<String, Object> adminProps = Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            try (AdminClient admin = AdminClient.create(adminProps)) {
                admin.createTopics(List.of(
                        new NewTopic(KafkaTopic.CHAT_MESSAGES.getTopicName(), 6, (short) 1),
                        new NewTopic(KafkaTopic.PRESENCE_EVENTS.getTopicName(), 3, (short) 1),
                        new NewTopic(KafkaTopic.PRESENCE_STATE.getTopicName(), 3, (short) 1),
                        new NewTopic(KafkaTopic.NOTIFICATIONS.getTopicName(), 3, (short) 1)
                )).all().get();
            } catch (Exception ex) {
                // topics may already exist — safe to ignore
            }
        };
    }
}
