package com.mecanica.oficina_api.application.ordemservico.output;

import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;

/**
 * Carga útil da notificação — campos pensados para template de e-mail futuro.
 */
public record NotificacaoCliente(
        TipoNotificacaoCliente tipo,
        String ordemServicoId,
        String clienteId,
        String veiculoId) {

    public static NotificacaoCliente recebido(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.RECEBIDA,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }

    public static NotificacaoCliente emDiagnostico(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.EM_DIAGNOSTICO,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }

    public static NotificacaoCliente envioOrcamento(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.ENVIO_ORCAMENTO,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }

    public static NotificacaoCliente emExecucao(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.EM_EXECUCAO,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }

    public static NotificacaoCliente finalizacao(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.FINALIZACAO_OS,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }

    public static NotificacaoCliente entrega(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.ENTREGUE,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }

    public static NotificacaoCliente cancelada(OrdemServico os) {
        return new NotificacaoCliente(
                TipoNotificacaoCliente.CANCELADA,
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId());
    }
}
