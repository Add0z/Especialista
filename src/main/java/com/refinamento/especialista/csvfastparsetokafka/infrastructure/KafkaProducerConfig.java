package com.refinamento.especialista.csvfastparsetokafka.infrastructure;

import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> stringProducerFactory(
            KafkaProperties properties) {

        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties());

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(
            ProducerFactory<String, String> stringProducerFactory) {

        return new KafkaTemplate<>(stringProducerFactory);
    }

    @Bean
    public ProducerFactory<String, byte[]> bytesProducerFactory(
            KafkaProperties properties) {

        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties());

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, byte[]> bytesKafkaTemplate(
            ProducerFactory<String, byte[]> bytesProducerFactory) {

        return new KafkaTemplate<>(bytesProducerFactory);
    }
}
