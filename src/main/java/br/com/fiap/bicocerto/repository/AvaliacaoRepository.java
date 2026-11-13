package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsBySolicitacaoId(Long solicitacaoId);

    Page<Avaliacao> findByPrestadorId(Long prestadorId, Pageable pageable);
}
