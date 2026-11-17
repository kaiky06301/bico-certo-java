package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Todos os servicos que o prestador faz (o principal tambem): ele recebe os pedidos de todas essas categorias.
     * EAGER porque a lista aparece em toda resposta e em toda checagem de acesso (open-in-view esta desligado).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "prestador_categorias",
            joinColumns = @JoinColumn(name = "prestador_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<Categoria> categorias = new LinkedHashSet<>();

    /** Foto de perfil (data URI base64 JPEG/PNG). */
    @Column(columnDefinition = "TEXT")
    private String foto;

    @Column(name = "anos_experiencia")
    private Integer anosExperiencia;

    @Column(length = 200)
    private String especialidades;

    @Column(length = 300)
    private String diferenciais;

    /** Dias em que atende, separados por virgula: SEG,TER,QUA,QUI,SEX,SAB,DOM. */
    @Column(name = "dias_atendimento", length = 30)
    private String diasAtendimento;

    @Column(name = "horario_inicio", length = 5)
    private String horarioInicio;

    @Column(name = "horario_fim", length = 5)
    private String horarioFim;

    @Column(name = "atende_emergencia", nullable = false)
    private boolean atendeEmergencia;

    /** Fotos na galeria de trabalhos realizados (atualizado a cada foto enviada ou apagada). */
    @Column(name = "quantidade_fotos", nullable = false)
    private int quantidadeFotos;

    /** Quantos servicos o prestador ja concluiu pelo Bico Certo (calculado na consulta). */
    @Column(name = "servicos_concluidos", nullable = false)
    private int servicosConcluidos;

    protected Prestador() {
    }

    public Prestador(Usuario usuario, Categoria categoria, String descricao) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.descricao = descricao;
        if (categoria != null) {
            this.categorias.add(categoria);
        }
    }

    public void registrarServicoConcluido() { servicosConcluidos++; }
    public void fotoAdicionada() { quantidadeFotos++; }
    public void fotoRemovida() { quantidadeFotos = Math.max(0, quantidadeFotos - 1); }

    /** Recalcula a media incluindo uma nova nota (1 a 5). */
    public void registrarAvaliacao(int nota) {
        BigDecimal soma = notaMedia.multiply(BigDecimal.valueOf(totalAvaliacoes)).add(BigDecimal.valueOf(nota));
        totalAvaliacoes++;
        notaMedia = soma.divide(BigDecimal.valueOf(totalAvaliacoes), 2, RoundingMode.HALF_UP);
    }

    /** Atende pedidos desta categoria (a principal ou uma das outras). */
    public boolean atende(Long categoriaId) {
        return categoria.getId().equals(categoriaId)
                || categorias.stream().anyMatch(c -> c.getId().equals(categoriaId));
    }

    /** Ids de todas as categorias atendidas, a principal primeiro. */
    public List<Long> idsDasCategorias() {
        List<Long> ids = new ArrayList<>();
        ids.add(categoria.getId());
        getOutrasCategorias().forEach(c -> ids.add(c.getId()));
        return ids;
    }

    public void definirOutrasCategorias(List<Categoria> categorias) {
        this.categorias.clear();
        this.categorias.add(categoria);
        this.categorias.addAll(categorias);
    }

    public void definirExperiencia(Integer anosExperiencia, String especialidades, String diferenciais) {
        this.anosExperiencia = anosExperiencia;
        this.especialidades = especialidades;
        this.diferenciais = diferenciais;
    }

    public void definirAtendimento(List<String> dias, String horarioInicio, String horarioFim, boolean atendeEmergencia) {
        this.diasAtendimento = dias == null || dias.isEmpty() ? null : String.join(",", dias);
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
        this.atendeEmergencia = atendeEmergencia;
    }

    public List<String> getDiasAtendimento() {
        return diasAtendimento == null || diasAtendimento.isBlank() ? List.of() : Arrays.asList(diasAtendimento.split(","));
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Categoria getCategoria() { return categoria; }
    /** Troca o servico principal (a lista completa e refeita em definirOutrasCategorias). */
    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
        this.categorias.add(categoria);
    }
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
    /** Servicos alem do principal. */
    public List<Categoria> getOutrasCategorias() {
        return categorias.stream().filter(c -> !c.getId().equals(categoria.getId())).toList();
    }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public Integer getAnosExperiencia() { return anosExperiencia; }
    public String getEspecialidades() { return especialidades; }
    public String getDiferenciais() { return diferenciais; }
    public String getHorarioInicio() { return horarioInicio; }
    public String getHorarioFim() { return horarioFim; }
    public boolean isAtendeEmergencia() { return atendeEmergencia; }
    public int getQuantidadeFotos() { return quantidadeFotos; }
}
