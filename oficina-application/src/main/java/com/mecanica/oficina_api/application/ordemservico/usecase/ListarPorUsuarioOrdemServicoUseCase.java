package com.mecanica.oficina_api.application.ordemservico.usecase;

import java.util.Comparator;
import java.util.List;

import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.MinhaOrdemServicoOutput;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

public class ListarPorUsuarioOrdemServicoUseCase {
    private final OrdemServicoGateway ordemServicoGateway;

    public ListarPorUsuarioOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
    }

    public List<MinhaOrdemServicoOutput> executar(String clienteId, OrdemServicoStatus status, String placa) {

        List<MinhaOrdemServicoOutput> osList = ordemServicoGateway.buscarPorCliente(clienteId);
        List<String> prioridadeStauts = List.of("EM_EXECUCAO", "AGUARDANDO_EXECUCAO", "AGUARDANDO_PAGAMENTO", "AGUARDANDO_APROVACAO", "EM_DIAGNOSTICO", "RECEBIDA");

        List<MinhaOrdemServicoOutput> openOsList =
            osList.stream()
                .filter(o -> !"FINALIZADA".equals(o.getStatus()) && !"ENTREGUE".equals(o.getStatus()) && !"CANCELADA".equals(o.getStatus()))
                .sorted(
                    Comparator.comparing((MinhaOrdemServicoOutput o) -> prioridadeStauts.indexOf(o.getStatus()))
                    .thenComparing(MinhaOrdemServicoOutput::dataCriacao)
                )
            .toList();


        if (status != null) {
            openOsList = openOsList.stream()
                .filter(os -> os.getStatus().equals(status.name()))
                .toList();
        }

        if (placa != null && !placa.isBlank()) {
            openOsList = openOsList.stream().filter(os -> os.getPlaca().equals(placa)).toList();
        }

        return openOsList;
    }

    
}
