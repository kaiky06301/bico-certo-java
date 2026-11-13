package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.Solicitacao;
import br.com.fiap.bicocerto.model.StatusSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    Page<Solicitacao> findByClienteId(Long clienteId, Pageable pageable);

    Page<Solicitacao> findByCategoriaIdAndStatus(Long categoriaId, StatusSolicitacao status, Pageable pageable);

    Page<Solicitacao> findByPrestadorId(Long prestadorId, Pageable pageable);

    boolean existsByCategoriaId(Long categoriaId);
}
