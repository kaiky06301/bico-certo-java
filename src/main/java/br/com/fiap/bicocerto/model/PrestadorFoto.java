package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Foto de um trabalho que o prestador ja fez (galeria do perfil). */
@Entity
@Table(name = "prestador_fotos")
public class PrestadorFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imagem;

    @Column(length = 80)
    private String legenda;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected PrestadorFoto() {
    }

    public PrestadorFoto(Prestador prestador, String imagem, String legenda) {
        this.prestador = prestador;
        this.imagem = imagem;
        this.legenda = legenda;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Prestador getPrestador() { return prestador; }
    public String getImagem() { return imagem; }
    public String getLegenda() { return legenda; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
