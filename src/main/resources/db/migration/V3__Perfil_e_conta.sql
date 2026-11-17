-- Perfil completo: endereco do usuario, conta ativa (exclusao), e dados profissionais do prestador.
ALTER TABLE usuarios ADD COLUMN endereco VARCHAR(200);
ALTER TABLE usuarios ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE prestadores ADD COLUMN regiao_atendimento VARCHAR(200);
ALTER TABLE prestadores ADD COLUMN disponibilidade VARCHAR(200);
-- selo "Verificado pelo Bico Certo": concedido pelo ADMIN depois de conferir os documentos
ALTER TABLE prestadores ADD COLUMN verificado BOOLEAN NOT NULL DEFAULT FALSE;

-- completa os prestadores de teste (se existirem)
UPDATE prestadores SET regiao_atendimento = 'Capao Redondo e bairros proximos',
                       disponibilidade = 'Segunda a sabado, das 8h as 18h', verificado = TRUE
 WHERE usuario_id IN (SELECT id FROM usuarios WHERE email IN ('joao@bicocerto.com.br', 'carlos@bicocerto.com.br'));
UPDATE prestadores SET regiao_atendimento = 'Jardim Angela e Capao Redondo',
                       disponibilidade = 'Segunda a sexta, das 7h as 17h'
 WHERE usuario_id IN (SELECT id FROM usuarios WHERE email = 'ana@bicocerto.com.br');
