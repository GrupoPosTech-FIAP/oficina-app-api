package com.mecanica.oficina_api.application.ordemservico;

import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

/** Fixtures de domínio compartilhados pelos testes de use case de ordem de serviço. */
public final class OrdemServicoFixture {

    public static final String OS_ID = "os-1";
    public static final String VEICULO_ID = "veic-1";
    public static final String CLIENTE_ID = "cli-1";

    private OrdemServicoFixture() {}

    public static OrdemServico osSemOrcamento(OrdemServicoStatus status) {
        return OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID, status, null, null);
    }
}
