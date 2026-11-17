package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "prestadores")
public class Prestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(length = 500)
    private String descricao;

    @Column(name = "regiao_atendimento", length = 200)
    private String regiaoAtendimento;

    @Column(length = 200)
    private String disponibilidade;

    @Column(nullable = false)
    private boolean verificado;

    @Column(name = "nota_media", nullable = false, precision = 3, scale = 2)
    private BigDecimal notaMedia = BigDecimal.ZERO;

    @Column(name = "total_avaliacoes", nullable = false)
    private int totalAvaliacoes;

    /** Quantos servicos o prestador ja concluiu pelo Bico Certo (calculado na consulta). */
    @org.hibernate.annotations.Formula("(select count(*) from solicitacoes s where s.prestador_id = id and s.status = 'CONCLUIDA')")
    private int servicosConcluidos;

    protected Prestador() {
    }

    public Prestador(Usuario usuario, Categoria categoria, String descricao) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.descricao = descricao;
    }

    /** Recalcula a media incluindo uma nova nota (1 a 5). */
    public void registrarAvaliacao(int nota) {
        BigDecimal soma = notaMedia.multiply(BigDecimal.valueOf(totalAvaliacoes)).add(BigDecimal.valueOf(nota));
        totalAvaliacoes++;
        notaMedia = soma.divide(BigDecimal.valueOf(totalAvaliacoes), 2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getRegiaoAtendimento() { return regiaoAtendimento; }
    public void setRegiaoAtendimento(String regiaoAtendimento) { this.regiaoAtendimento = regiaoAtendimento; }
    public String getDisponibilidade() { return disponibilidade; }
    public void setDisponibilidade(String disponibilidade) { this.disponibilidade = disponibilidade; }
    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }
    public BigDecimal getNotaMedia() { return notaMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }

    public int getServicosConcluidos() { return servicosConcluidos; }
}
