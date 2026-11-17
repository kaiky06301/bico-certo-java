package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.Solicitacao;
import br.com.fiap.bicocerto.model.StatusSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    Page<Solicitacao> findByClienteId(Long clienteId, Pageable pageable);

    List<Solicitacao> findByClienteId(Long clienteId);

    boolean existsByClienteIdAndStatusIn(Long clienteId, java.util.Collection<StatusSolicitacao> status);

    boolean existsByPrestadorIdAndStatus(Long prestadorId, StatusSolicitacao status);

    Page<Solicitacao> findByCategoriaIdInAndStatus(java.util.Collection<Long> categoriaIds, StatusSolicitacao status, Pageable pageable);

    Page<Solicitacao> findByPrestadorId(Long prestadorId, Pageable pageable);

    boolean existsByCategoriaId(Long categoriaId);

    Page<Solicitacao> findByStatus(StatusSolicitacao status, Pageable pageable);

    /** Trava a linha ao assumir/concluir: evita dois prestadores assumindo o mesmo pedido ao mesmo tempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Solicitacao> findParaAtualizarById(Long id);
}
