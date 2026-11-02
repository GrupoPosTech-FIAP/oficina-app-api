package com.mecanica.oficina_api.application.ordemservico.usecase;

import com.mecanica.oficina_api.application.cliente.gateway.ClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.application.veiculo.gateway.VeiculoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;

public class CriarOrdemServicoUseCase {
    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final VeiculoGateway veiculoGateway;
    private final ClienteGateway clienteGateway;

    public CriarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway,
            NotificadorClienteGateway notificadorClienteGateway, VeiculoGateway veiculoGateway,
            ClienteGateway clienteGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.veiculoGateway = veiculoGateway;
        this.clienteGateway = clienteGateway;
    }

    public OrdemServico executar(String veiculoId, String clienteId) {
        veiculoGateway.buscar(veiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado: " + veiculoId));
        clienteGateway.findByIdAtivo(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clienteId));

        OrdemServico ordemServico = ordemServicoGateway.cadastrar(veiculoId, clienteId);

        this.notificadorClienteGateway.notificar(NotificacaoCliente.recebido(ordemServico));

        return ordemServico;
    }
}
