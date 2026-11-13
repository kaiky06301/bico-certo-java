package br.com.fiap.bicocerto.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara exchange, fila e binding dos eventos de solicitacao (mesmo padrao de nomes usado em aula). */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "solicitacao-exchange";
    public static final String FILA = "solicitacao-evento-queue";
    public static final String ROUTING_KEY = "solicitacao-evento-rout-key";

    @Bean
    DirectExchange solicitacaoExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Queue solicitacaoEventoQueue() {
        return new Queue(FILA, true);
    }

    @Bean
    Binding solicitacaoEventoBinding() {
        return BindingBuilder.bind(solicitacaoEventoQueue()).to(solicitacaoExchange()).with(ROUTING_KEY);
    }
}
