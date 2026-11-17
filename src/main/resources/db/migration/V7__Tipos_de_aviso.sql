-- Tipo do aviso (icone e titulo no app) e dados para abrir direto o que interessa
ALTER TABLE notificacoes ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'PEDIDO';
ALTER TABLE notificacoes ADD COLUMN valor NUMERIC(10,2);
ALTER TABLE notificacoes ADD COLUMN orcamento_id BIGINT;

UPDATE notificacoes SET tipo = 'CONTRATACAO' WHERE mensagem LIKE '%vai atender%';
UPDATE notificacoes SET tipo = 'CONCLUSAO' WHERE mensagem LIKE '%concluido%';
UPDATE notificacoes SET tipo = 'CANCELAMENTO' WHERE mensagem LIKE '%cancelou%';
