package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Prestador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** categoria = servico principal; outrasCategorias = outros servicos que tambem faz. foto = data URI ou null. */
public record PrestadorResponse(
        Long id,
        String nome,
        String bairro,
        TriagemResponse.CategoriaResumo categoria,
        String descricao,
        String regiaoAtendimento,
        String disponibilidade,
        boolean verificado,
        BigDecimal notaMedia,
        int totalAvaliacoes,
        int servicosConcluidos,
        Long usuarioId,
        LocalDateTime membroDesde,
        List<TriagemResponse.CategoriaResumo> outrasCategorias,
        String foto,
        Integer anosExperiencia,
        String especialidades,
        String diferenciais,
        List<String> diasAtendimento,
        String horarioInicio,
        String horarioFim,
        boolean atendeEmergencia,
        int quantidadeFotos) {

    public static PrestadorResponse from(Prestador p) {
        return new PrestadorResponse(p.getId(), p.getUsuario().getNome(), p.getUsuario().getBairro(),
                new TriagemResponse.CategoriaResumo(p.getCategoria().getId(), p.getCategoria().getNome()),
                p.getDescricao(), p.getRegiaoAtendimento(), p.getDisponibilidade(), p.isVerificado(),
                p.getNotaMedia(), p.getTotalAvaliacoes(), p.getServicosConcluidos(), p.getUsuario().getId(),
                p.getUsuario().getCriadoEm(),
                p.getOutrasCategorias().stream()
                        .map(c -> new TriagemResponse.CategoriaResumo(c.getId(), c.getNome())).toList(),
                p.getFoto(), p.getAnosExperiencia(), p.getEspecialidades(), p.getDiferenciais(),
                p.getDiasAtendimento(), p.getHorarioInicio(), p.getHorarioFim(), p.isAtendeEmergencia(),
                p.getQuantidadeFotos());
    }
}
