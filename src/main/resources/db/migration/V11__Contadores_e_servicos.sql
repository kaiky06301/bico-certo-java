-- Contadores guardados na própria tabela (atualizados pelo serviço a cada mudança)
ALTER TABLE prestadores ADD COLUMN servicos_concluidos INT NOT NULL DEFAULT 0;
ALTER TABLE prestadores ADD COLUMN quantidade_fotos INT NOT NULL DEFAULT 0;
ALTER TABLE solicitacoes ADD COLUMN quantidade_fotos INT NOT NULL DEFAULT 0;

UPDATE prestadores SET servicos_concluidos =
    (SELECT COUNT(*) FROM solicitacoes s WHERE s.prestador_id = prestadores.id AND s.status = 'CONCLUIDA');
UPDATE prestadores SET quantidade_fotos =
    (SELECT COUNT(*) FROM prestador_fotos f WHERE f.prestador_id = prestadores.id);
UPDATE solicitacoes SET quantidade_fotos =
    (SELECT COUNT(*) FROM solicitacao_fotos f WHERE f.solicitacao_id = solicitacoes.id);

-- prestador_categorias passa a guardar TODOS os serviços do prestador (o principal também)
INSERT INTO prestador_categorias (prestador_id, categoria_id)
SELECT p.id, p.categoria_id FROM prestadores p
WHERE NOT EXISTS (SELECT 1 FROM prestador_categorias pc WHERE pc.prestador_id = p.id AND pc.categoria_id = p.categoria_id);
