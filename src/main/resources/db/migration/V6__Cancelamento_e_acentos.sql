-- Motivo informado pelo cliente ao cancelar o pedido (aparece no detalhe e no historico)
ALTER TABLE solicitacoes ADD COLUMN motivo_cancelamento VARCHAR(200);

-- Acentuacao dos dados de exemplo (categorias, bairros, cidade e textos do perfil)
UPDATE categorias SET nome = 'Elétrica',
    descricao = 'Tomadas, interruptores, disjuntores, chuveiro elétrico, fiação, curto-circuito, lâmpadas e instalação de ventilador.'
    WHERE nome = 'Eletrica';
UPDATE categorias SET descricao = 'Vazamentos, torneiras, registros, pia ou ralo entupido, descarga, caixa d''água e canos quebrados.'
    WHERE nome = 'Encanamento';
UPDATE categorias SET descricao = 'Faxina residencial, limpeza pós-obra, limpeza de vidros, estofados e organização.'
    WHERE nome = 'Limpeza';
UPDATE categorias SET descricao = 'Pintura de paredes, tetos, portões e grades, massa corrida e retoques.'
    WHERE nome = 'Pintura';
UPDATE categorias SET nome = 'Montagem de móveis',
    descricao = 'Montagem e desmontagem de guarda-roupa, cama, rack, estante e móveis planejados simples.'
    WHERE nome = 'Montagem de moveis';
UPDATE categorias SET descricao = 'Instalação de prateleiras, cortinas, suportes de TV, troca de fechaduras e ajustes gerais.'
    WHERE nome = 'Pequenos reparos';

UPDATE usuarios SET bairro = 'Capão Redondo' WHERE bairro = 'Capao Redondo';
UPDATE usuarios SET bairro = 'Jardim Ângela' WHERE bairro = 'Jardim Angela';
UPDATE usuarios SET bairro = 'Grajaú' WHERE bairro = 'Grajau';
UPDATE usuarios SET nome = 'João Eletricista' WHERE nome = 'Joao Eletricista';
UPDATE solicitacoes SET bairro = 'Capão Redondo' WHERE bairro = 'Capao Redondo';
UPDATE solicitacoes SET bairro = 'Jardim Ângela' WHERE bairro = 'Jardim Angela';
UPDATE solicitacoes SET bairro = 'Grajaú' WHERE bairro = 'Grajau';
UPDATE solicitacoes SET endereco = REPLACE(endereco, 'Sao Paulo/', 'São Paulo/') WHERE endereco LIKE '%Sao Paulo/%';
UPDATE enderecos SET bairro = 'Capão Redondo' WHERE bairro = 'Capao Redondo';
UPDATE enderecos SET bairro = 'Jardim Ângela' WHERE bairro = 'Jardim Angela';
UPDATE enderecos SET cidade = 'São Paulo' WHERE cidade = 'Sao Paulo';
ALTER TABLE enderecos ALTER COLUMN cidade SET DEFAULT 'São Paulo';
UPDATE prestadores SET regiao_atendimento = 'Capão Redondo e bairros próximos' WHERE regiao_atendimento = 'Capao Redondo e bairros proximos';
UPDATE prestadores SET regiao_atendimento = 'Jardim Ângela e Capão Redondo' WHERE regiao_atendimento = 'Jardim Angela e Capao Redondo';
UPDATE prestadores SET disponibilidade = 'Segunda a sábado, das 8h às 18h' WHERE disponibilidade = 'Segunda a sabado, das 8h as 18h';
UPDATE prestadores SET disponibilidade = 'Segunda a sexta, das 7h às 17h' WHERE disponibilidade = 'Segunda a sexta, das 7h as 17h';
UPDATE prestadores SET descricao = 'Eletricista há 12 anos, atendo emergências no mesmo dia.' WHERE descricao = 'Eletricista ha 12 anos, atendo emergencias no mesmo dia.';
UPDATE prestadores SET descricao = 'Conserto vazamentos, desentupo pias e troco registros.' WHERE descricao = 'Conserto vazamentos, desentupo pias e troco registros.';
UPDATE prestadores SET descricao = 'Faxina completa e pós-obra com material próprio.' WHERE descricao = 'Faxina completa e pos-obra com material proprio.';
