package com.mecanica.oficina_api.application.ordemservico.usecase;

import com.mecanica.oficina_api.application.evento.EventosSaga;
import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.input.ConcluirDiagnosticoInput;
import com.mecanica.oficina_api.application.servico.gateway.ServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.servico.Servico;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Início da Saga: publica {@code DiagnosticoConcluido} com os itens já precificados para o billing
 * gerar o orçamento. A OS continua EM_DIAGNOSTICO até chegar {@code OrcamentoGerado}.
 */
public class ConcluirDiagnosticoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final ServicoGateway servicoGateway;
    private final RegistroEventosOrdemServico eventos;

    public ConcluirDiagnosticoUseCase(OrdemServicoGateway ordemServicoGateway,
                                      ServicoGateway servicoGateway,
                                      RegistroEventosOrdemServico eventos) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.servicoGateway = servicoGateway;
        this.eventos = eventos;
    }

    public OrdemServico executar(String id, ConcluirDiagnosticoInput input) {
        OrdemServico os = ordemServicoGateway.encontrarOuLancar(id);
        os.concluirDiagnostico();

        List<ConcluirDiagnosticoInput.Servico> servicos = Optional.ofNullable(input.servicos()).orElse(List.of());
        List<ConcluirDiagnosticoInput.Insumo> insumos = Optional.ofNullable(input.insumos()).orElse(List.of());
        if (servicos.isEmpty() && insumos.isEmpty()) {
            throw new IllegalArgumentException("O diagnóstico deve ter ao menos um serviço ou insumo");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("ordemServicoId", os.getId());
        payload.put("clienteId", os.getClienteId());
        payload.put("veiculoId", os.getVeiculoId());
        payload.put("servicos", servicos.stream().map(this::precificar).toList());
        payload.put("insumos", insumos.stream().map(ConcluirDiagnosticoUseCase::validar).toList());
        if (input.observacoes() != null && !input.observacoes().isBlank()) {
            payload.put("observacoes", input.observacoes());
        }

        eventos.publicar(EventosSaga.DIAGNOSTICO_CONCLUIDO, os, os.getStatus(), payload, null);
        return os;
    }

    private Map<String, Object> precificar(ConcluirDiagnosticoInput.Servico item) {
        exigirQuantidade(item.quantidade());
        Servico servico = servicoGateway.buscarOuFalhar(item.servicoId());
        if (!servico.isAtivo()) {
            throw new IllegalArgumentException("Serviço inativo: " + servico.getNome());
        }
        return Map.of(
                "servicoId", servico.getId(),
                "quantidade", item.quantidade(),
                "descricao", servico.getNome(),
                "precoUnitario", servico.getPreco());
    }

    private static Map<String, Object> validar(ConcluirDiagnosticoInput.Insumo item) {
        exigirQuantidade(item.quantidade());
        if (item.insumoId() == null || item.insumoId().isBlank()) {
            throw new IllegalArgumentException("O ID do insumo é obrigatório");
        }
        if (item.descricao() == null || item.descricao().isBlank()
                || item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Descrição e preço unitário positivo são obrigatórios para o insumo " + item.insumoId());
        }
        return Map.of(
                "insumoId", item.insumoId(),
                "quantidade", item.quantidade(),
                "descricao", item.descricao(),
                "precoUnitario", item.precoUnitario());
    }

    private static void exigirQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
    }
}
