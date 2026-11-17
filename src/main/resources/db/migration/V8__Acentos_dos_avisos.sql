-- Avisos antigos com o texto sem acento (gerados antes da V6/V7)
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'pedido de eletrica', 'pedido de elétrica');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'Joao Eletricista', 'João Eletricista');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'O telefone dele ja esta liberado no pedido.', 'O telefone e o endereço já estão liberados no pedido.');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'Servico de eletrica concluido!', 'Serviço de elétrica concluído!');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'Servico de ', 'Serviço de ');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, ' concluido!', ' concluído!');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'urgencia ', 'urgência ');
UPDATE notificacoes SET mensagem = REPLACE(mensagem, 'preco justo', 'faixa de referência');
