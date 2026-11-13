package br.com.fiap.bicocerto.messaging;

import br.com.fiap.bicocerto.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Consome os eventos de forma assincrona e gera as notificacoes para prestadores e clientes. */
@Component
public class SolicitacaoEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventoConsumer.class);

    private final JsonMapper jsonMapper;
    private final NotificacaoService notificacaoService;

    public SolicitacaoEventoConsumer(JsonMapper jsonMapper, NotificacaoService notificacaoService) {
        this.jsonMapper = jsonMapper;
        this.notificacaoService = notificacaoService;
    }

    @RabbitListener(queues = RabbitConfig.FILA)
    public void receber(String mensagem) {
        SolicitacaoEvento evento = jsonMapper.readValue(mensagem, SolicitacaoEvento.class);
        log.info("Evento {} recebido para a solicitacao {}", evento.tipo(), evento.solicitacaoId());
        notificacaoService.processar(evento);
    }
}
