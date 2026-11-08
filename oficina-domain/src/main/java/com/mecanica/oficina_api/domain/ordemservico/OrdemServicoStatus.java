package com.mecanica.oficina_api.domain.ordemservico;

public enum OrdemServicoStatus {
    RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, AGUARDANDO_PAGAMENTO, AGUARDANDO_EXECUCAO,
    EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA
}
