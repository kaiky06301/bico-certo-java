package br.com.fiap.bicocerto.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica os eventos de solicitacao no RabbitMQ.
 * A publicacao acontece somente DEPOIS do commit da transacao, para o consumidor nunca ler
 * uma solicitacao que ainda nao foi gravada (ou que sofreu rollback).
 */
@Component
public class SolicitacaoEventoProducer {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventoProducer.class);

    private final AmqpTemplate amqpTemplate;
    private final JsonMapper jsonMapper;

    public SolicitacaoEventoProducer(AmqpTemplate amqpTemplate, JsonMapper jsonMapper) {
        this.amqpTemplate = amqpTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void publicarAposCommit(SolicitacaoEvento evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicar(evento);
                }
            });
        } else {
            publicar(evento);
        }
    }

    void publicar(SolicitacaoEvento evento) {
        try {
            amqpTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                    jsonMapper.writeValueAsString(evento));
            log.info("Evento {} publicado para a solicitacao {}", evento.tipo(), evento.solicitacaoId());
        } catch (AmqpException ex) {
            // a solicitacao ja esta salva; sem o broker, apenas as notificacoes deixam de ser geradas
            log.error("Falha ao publicar evento {} da solicitacao {}: {}", evento.tipo(), evento.solicitacaoId(),
                    ex.getMessage());
        }
    }
}
