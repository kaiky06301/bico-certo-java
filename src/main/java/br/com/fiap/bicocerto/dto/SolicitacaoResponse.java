package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitacaoResponse(
        Long id,
        String descricao,
        String bairro,
        String endereco,
        String observacao,
        int quantidadeFotos,
        StatusSolicitacao status,
        String motivoCancelamento,
        Urgencia urgencia,
        TriagemResponse.CategoriaResumo categoria,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        String resumoTriagem,
        String dicaSeguranca,
        OrigemTriagem origemTriagem,
        Contato cliente,
        Contato prestador,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm) {

    /** Telefone so aparece depois que um prestador assumiu o pedido (protecao de dados pessoais). */
    public record Contato(Long id, String nome, String telefone, BigDecimal notaMedia) {
    }

    /** Visao de prestadores e admin: telefone e rua so depois que o pedido foi assumido. */
    public static SolicitacaoResponse from(Solicitacao s) {
        return montar(s, false);
    }

    /** Visao do proprio cliente: ele sempre ve a rua que escolheu. */
    public static SolicitacaoResponse paraDono(Solicitacao s) {
        return montar(s, true);
    }

    private static SolicitacaoResponse montar(Solicitacao s, boolean dono) {
        boolean contatoLiberado = s.getStatus() == StatusSolicitacao.EM_ATENDIMENTO
                || s.getStatus() == StatusSolicitacao.CONCLUIDA;
        Usuario c = s.getCliente();
        Contato cliente = new Contato(c.getId(), c.getNome(), contatoLiberado ? c.getTelefone() : null, null);
        Contato prestador = null;
        if (s.getPrestador() != null) {
            Prestador p = s.getPrestador();
            prestador = new Contato(p.getId(), p.getUsuario().getNome(),
                    contatoLiberado ? p.getUsuario().getTelefone() : null, p.getNotaMedia());
        }
        String endereco = dono || contatoLiberado ? s.getEndereco() : null;
        return new SolicitacaoResponse(s.getId(), s.getDescricao(), s.getBairro(), endereco,
                s.getObservacao(), s.getQuantidadeFotos(), s.getStatus(), s.getMotivoCancelamento(), s.getUrgencia(),
                new TriagemResponse.CategoriaResumo(s.getCategoria().getId(), s.getCategoria().getNome()),
                s.getPrecoMinimo(), s.getPrecoMaximo(), s.getResumoTriagem(), s.getDicaSeguranca(),
                s.getOrigemTriagem(), cliente, prestador, s.getCriadaEm(), s.getAtualizadaEm());
    }
}
