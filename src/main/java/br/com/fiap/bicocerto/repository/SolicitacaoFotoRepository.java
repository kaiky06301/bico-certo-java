package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.SolicitacaoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoFotoRepository extends JpaRepository<SolicitacaoFoto, Long> {

    List<SolicitacaoFoto> findBySolicitacaoIdOrderByOrdemAsc(Long solicitacaoId);

    void deleteBySolicitacaoId(Long solicitacaoId);

    /** Exclusao de conta (LGPD): as fotos podem mostrar a casa do cliente, entao saem junto. */
    void deleteBySolicitacaoClienteId(Long clienteId);
}
