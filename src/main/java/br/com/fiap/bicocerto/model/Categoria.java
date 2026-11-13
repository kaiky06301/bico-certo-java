package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "preco_minimo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMinimo;

    @Column(name = "preco_maximo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMaximo;

    protected Categoria() {
    }

    public Categoria(String nome, String descricao, BigDecimal precoMinimo, BigDecimal precoMaximo) {
        atualizar(nome, descricao, precoMinimo, precoMaximo);
    }

    public void atualizar(String nome, String descricao, BigDecimal precoMinimo, BigDecimal precoMaximo) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoMinimo = precoMinimo;
        this.precoMaximo = precoMaximo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPrecoMinimo() { return precoMinimo; }
    public BigDecimal getPrecoMaximo() { return precoMaximo; }
}
