package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitacaoResponse(
        Long id,
        String descricao,
        String bairro,
        StatusSolicitacao status,
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

    public static SolicitacaoResponse from(Solicitacao s) {
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
        return new SolicitacaoResponse(s.getId(), s.getDescricao(), s.getBairro(), s.getStatus(), s.getUrgencia(),
                new TriagemResponse.CategoriaResumo(s.getCategoria().getId(), s.getCategoria().getNome()),
                s.getPrecoMinimo(), s.getPrecoMaximo(), s.getResumoTriagem(), s.getDicaSeguranca(),
                s.getOrigemTriagem(), cliente, prestador, s.getCriadaEm(), s.getAtualizadaEm());
    }
}
