# language: pt
Funcionalidade: Saga da ordem de serviço
  Como oficina
  Quero que a OS acompanhe o orçamento, o estoque e o pagamento tratados nos outros serviços
  Para só executar o serviço depois de aprovado, com as peças baixadas e pago

  Os eventos de billing e inventory são entregues direto ao consumidor da fila, como chegariam
  pelo SQS; os eventos que a OS publica são conferidos na outbox.

  Contexto:
    Dado uma ordem de serviço em diagnóstico para o serviço "Troca de óleo" de R$ 80,00

  Cenário: Caminho feliz até a finalização
    Quando o mecânico conclui o diagnóstico com 2 unidades do insumo "Óleo 5W30" a R$ 45,00
    Então o evento "DiagnosticoConcluido" é publicado com o serviço e o insumo precificados
    Quando o billing publica "OrcamentoGerado"
    Então a ordem de serviço fica "AGUARDANDO_APROVACAO"
    Quando o billing publica "OrcamentoAprovado"
    E o inventory publica "EstoqueBaixado"
    E o billing publica "CobrancaCriada"
    Então a ordem de serviço fica "AGUARDANDO_PAGAMENTO"
    Quando o billing publica "PagamentoConfirmado"
    Então a ordem de serviço fica "AGUARDANDO_EXECUCAO"
    Quando o mecânico inicia a execução
    E conclui todos os serviços da ordem
    E finaliza a ordem de serviço
    Então a ordem de serviço fica "FINALIZADA"
    E os eventos publicados são "DiagnosticoConcluido, ExecucaoIniciada, ExecucaoConcluida"

  Esquema do Cenário: <evento> cancela a OS antes da execução
    Dado que a saga já recebeu "<eventos anteriores>"
    Quando o <servico> publica "<evento>"
    Então a ordem de serviço fica "CANCELADA"
    E o histórico registra "<evento>" levando de "<status anterior>" para "CANCELADA"

    Exemplos:
      | eventos anteriores                                         | servico   | evento              | status anterior      |
      | OrcamentoGerado                                            | billing   | OrcamentoNegado     | AGUARDANDO_APROVACAO |
      | OrcamentoGerado                                            | billing   | OrcamentoExpirado   | AGUARDANDO_APROVACAO |
      | OrcamentoGerado, OrcamentoAprovado                         | inventory | EstoqueInsuficiente | AGUARDANDO_APROVACAO |
      | OrcamentoGerado, OrcamentoAprovado, EstoqueBaixado, CobrancaCriada | billing   | PagamentoRecusado   | AGUARDANDO_PAGAMENTO |
      | OrcamentoGerado, OrcamentoAprovado, EstoqueBaixado, CobrancaCriada | billing   | PagamentoExpirado   | AGUARDANDO_PAGAMENTO |

  Cenário: Falha na execução dispara os estornos de estoque e pagamento
    Dado que a saga já recebeu "OrcamentoGerado, OrcamentoAprovado, EstoqueBaixado, CobrancaCriada, PagamentoConfirmado"
    Quando o mecânico inicia a execução
    E a execução falha com o motivo "Bloco do motor trincado"
    Então a ordem de serviço fica "CANCELADA"
    E o evento "ExecucaoFalhou" é publicado com o motivo "Bloco do motor trincado"
    Quando o inventory publica "EstoqueEstornado"
    E o billing publica "PagamentoEstornado"
    Então a ordem de serviço fica "CANCELADA"
    E o histórico termina com "ExecucaoFalhou, EstoqueEstornado, PagamentoEstornado"

  Cenário: A execução não começa sem pagamento confirmado
    Dado que a saga já recebeu "OrcamentoGerado"
    Quando o mecânico inicia a execução
    Então a operação é recusada com conflito
    E a ordem de serviço fica "AGUARDANDO_APROVACAO"

  Cenário: Evento fora de ordem é registrado e descartado
    Quando o billing publica "PagamentoConfirmado"
    Então a ordem de serviço fica "EM_DIAGNOSTICO"
    E o histórico marca "PagamentoConfirmado" como descartado

  Cenário: Evento entregue duas vezes é processado uma vez só
    Dado que o diagnóstico foi concluído
    Quando o billing entrega duas vezes o mesmo "OrcamentoGerado"
    Então a segunda entrega é ignorada como duplicada
    E o histórico tem 1 registro de "OrcamentoGerado"
