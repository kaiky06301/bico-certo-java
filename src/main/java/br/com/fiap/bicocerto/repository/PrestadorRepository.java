package br.com.fiap.bicocerto.repository;

import br.com.fiap.bicocerto.model.Prestador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** "categorias" = todos os servicos do prestador (principal + outros): quem atende a categoria. */
public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    Optional<Prestador> findByUsuarioId(Long usuarioId);

    boolean existsByCategoriasId(Long categoriaId);

    long countByCategoriasId(Long categoriaId);

    List<Prestador> findByCategoriasId(Long categoriaId);

    Page<Prestador> findByCategoriasId(Long categoriaId, Pageable pageable);

    Page<Prestador> findByUsuarioBairroIgnoreCase(String bairro, Pageable pageable);

    Page<Prestador> findByCategoriasIdAndUsuarioBairroIgnoreCase(Long categoriaId, String bairro, Pageable pageable);
}
