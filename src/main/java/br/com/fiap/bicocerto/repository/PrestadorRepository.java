package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.Prestador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    Optional<Prestador> findByUsuarioId(Long usuarioId);

    List<Prestador> findByCategoriaId(Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);

    @Query("""
            SELECT p FROM Prestador p
            WHERE (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
              AND (:bairro IS NULL OR LOWER(p.usuario.bairro) = LOWER(CAST(:bairro AS string)))
            """)
    Page<Prestador> buscar(@Param("categoriaId") Long categoriaId, @Param("bairro") String bairro, Pageable pageable);
}
