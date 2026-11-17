package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.PrestadorFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrestadorFotoRepository extends JpaRepository<PrestadorFoto, Long> {

    List<PrestadorFoto> findByPrestadorIdOrderByCriadaEmDesc(Long prestadorId);

    long countByPrestadorId(Long prestadorId);

    Optional<PrestadorFoto> findByIdAndPrestadorId(Long id, Long prestadorId);

    void deleteByPrestadorId(Long prestadorId);
}
