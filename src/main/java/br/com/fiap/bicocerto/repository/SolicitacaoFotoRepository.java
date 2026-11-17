package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.SolicitacaoFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitacaoFotoRepository extends JpaRepository<SolicitacaoFoto, Long> {

    List<SolicitacaoFoto> findBySolicitacaoIdOrderByOrdemAsc(Long solicitacaoId);

    @Modifying
    @Query("delete from SolicitacaoFoto f where f.solicitacao.id = :solicitacaoId")
    void apagarDoPedido(@Param("solicitacaoId") Long solicitacaoId);

    /** Exclusao de conta (LGPD): as fotos podem mostrar a casa do cliente, entao saem junto. */
    @Modifying
    @Query("delete from SolicitacaoFoto f where f.solicitacao.id in (select s.id from Solicitacao s where s.cliente.id = :clienteId)")
    void apagarDoCliente(@Param("clienteId") Long clienteId);
}
