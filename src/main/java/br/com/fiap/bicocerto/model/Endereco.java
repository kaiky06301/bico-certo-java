package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Endereco salvo pelo usuario para pedir servicos em lugares diferentes. */
@Entity
@Table(name = "enderecos")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 40)
    private String apelido;

    @Column(nullable = false, length = 200)
    private String logradouro;

    @Column(length = 100)
    private String complemento;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(nullable = false, length = 60)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private boolean principal;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Endereco() {
    }

    public Endereco(Usuario usuario, String apelido, String logradouro, String complemento, String bairro,
                    String cidade, String uf) {
        this.usuario = usuario;
        atualizar(apelido, logradouro, complemento, bairro, cidade, uf);
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }

    public void atualizar(String apelido, String logradouro, String complemento, String bairro, String cidade,
                          String uf) {
        this.apelido = apelido;
        this.logradouro = logradouro;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
    }

    /** Texto usado no pedido: "Rua das Flores, 120 - apto 12, Sao Paulo/SP". */
    public String textoCompleto() {
        String rua = complemento == null || complemento.isBlank() ? logradouro : logradouro + " - " + complemento;
        return rua + ", " + cidade + "/" + uf;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getApelido() { return apelido; }
    public String getLogradouro() { return logradouro; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
