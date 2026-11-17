package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String senha;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(nullable = false)
    private boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senha, String telefone, String bairro, Papel papel) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.telefone = telefone;
        this.bairro = bairro;
        this.papel = papel;
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public Papel getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
    public void setSenha(String senhaCriptografada) { this.senha = senhaCriptografada; }

    /**
     * Exclusao da conta (LGPD): os dados pessoais sao apagados e o login deixa de funcionar, mas o registro
     * continua existindo para nao quebrar o historico de pedidos e avaliacoes de outras pessoas.
     */
    public void anonimizar(String senhaInutilizavel) {
        this.nome = "Usuario removido";
        this.email = "removido-" + id + "@bicocerto.invalid";
        this.telefone = null;
        this.senha = senhaInutilizavel;
        this.ativo = false;
    }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
