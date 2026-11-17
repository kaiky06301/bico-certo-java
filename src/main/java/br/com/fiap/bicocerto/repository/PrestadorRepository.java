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

    boolean existsByOutrasCategoriasId(Long categoriaId);

    /** Prestadores que atendem a categoria (como servico principal ou como outro servico). */
    @Query("""
            SELECT COUNT(p) FROM Prestador p
            WHERE p.categoria.id = :categoriaId
               OR EXISTS (SELECT 1 FROM Prestador p2 JOIN p2.outrasCategorias c WHERE p2 = p AND c.id = :categoriaId)
            """)
    long contarQueAtendem(@Param("categoriaId") Long categoriaId);

    @Query("""
            SELECT p FROM Prestador p
            WHERE p.categoria.id = :categoriaId
               OR EXISTS (SELECT 1 FROM Prestador p2 JOIN p2.outrasCategorias c WHERE p2 = p AND c.id = :categoriaId)
            """)
    List<Prestador> listarQueAtendem(@Param("categoriaId") Long categoriaId);

    @Query("""
            SELECT p FROM Prestador p
            WHERE (:categoriaId IS NULL OR p.categoria.id = :categoriaId
                   OR EXISTS (SELECT 1 FROM Prestador p2 JOIN p2.outrasCategorias c WHERE p2 = p AND c.id = :categoriaId))
              AND (:bairro IS NULL OR LOWER(p.usuario.bairro) = LOWER(CAST(:bairro AS string)))
            """)
    Page<Prestador> buscar(@Param("categoriaId") Long categoriaId, @Param("bairro") String bairro, Pageable pageable);
}
