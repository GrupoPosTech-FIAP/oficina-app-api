package com.mecanica.oficina_api.adapters.web;

import com.mecanica.oficina_api.application.ordemservico.input.ConcluirDiagnosticoInput;
import com.mecanica.oficina_api.application.ordemservico.usecase.ConcluirDiagnosticoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ConsultarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.CriarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.EntregarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.FalharExecucaoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.FinalizarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.FinalizarServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarDiagnosticoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarExecucaoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarHistoricoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarOrdensServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarPorVeiculoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarServicosPorOSUseCase;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.adapters.web.dto.request.ConcluirDiagnosticoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.CriarOrdemServicoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.FalharExecucaoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.IniciarExecucaoRequest;
import com.mecanica.oficina_api.adapters.web.dto.response.HistoricoOrdemServicoResponse;
import com.mecanica.oficina_api.adapters.web.dto.response.OrdemServicoResponse;
import com.mecanica.oficina_api.adapters.web.dto.response.ValidationErrorResponse;
import com.mecanica.oficina_api.adapters.web.dto.response.ServicoStatusResponse;
import com.mecanica.oficina_api.adapters.web.presenter.OrdemServicoPresenter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("ordem-servico")
@Tag(name = "Ordem de Serviço", description = "Gerenciamento de ordens de serviço. Fluxo (Saga, ADR 005): RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → AGUARDANDO_PAGAMENTO → AGUARDANDO_EXECUCAO → EM_EXECUCAO → FINALIZADA → ENTREGUE; falhas levam a CANCELADA")
@SecurityRequirement(name = "bearerAuth")
public class OrdemServicoController {

    private final CriarOrdemServicoUseCase criarOrdemServicoUseCase;
    private final ConsultarOrdemServicoUseCase consultarOrdemServicoUseCase;
    private final ListarOrdensServicoUseCase listarOrdensServicoUseCase;
    private final ListarPorVeiculoOrdemServicoUseCase listarPorVeiculoOrdemServicoUseCase;
    private final IniciarDiagnosticoOrdemServicoUseCase iniciarDiagnosticoOrdemServicoUseCase;
    private final IniciarExecucaoUseCase iniciarExecucaoUseCase;
    private final FinalizarOrdemServicoUseCase finalizarOrdemServicoUseCase;
    private final EntregarOrdemServicoUseCase entregarOrdemServicoUseCase;
    private final ListarServicosPorOSUseCase listarServicosPorOSUseCase;
    private final IniciarServicoUseCase iniciarServicoUseCase;
    private final FinalizarServicoUseCase finalizarServicoUseCase;
    private final ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;
    private final FalharExecucaoUseCase falharExecucaoUseCase;
    private final ListarHistoricoOrdemServicoUseCase listarHistoricoOrdemServicoUseCase;
    private final OrdemServicoPresenter presenter;

    public OrdemServicoController(CriarOrdemServicoUseCase criarOrdemServicoUseCase,
            ConsultarOrdemServicoUseCase consultarOrdemServicoUseCase,
            ListarOrdensServicoUseCase listarOrdensServicoUseCase,
            ListarPorVeiculoOrdemServicoUseCase listarPorVeiculoOrdemServicoUseCase,
            IniciarDiagnosticoOrdemServicoUseCase iniciarDiagnosticoOrdemServicoUseCase,
            IniciarExecucaoUseCase iniciarExecucaoUseCase,
            FinalizarOrdemServicoUseCase finalizarOrdemServicoUseCase,
            EntregarOrdemServicoUseCase entregarOrdemServicoUseCase,
            ListarServicosPorOSUseCase listarServicosPorOSUseCase,
            IniciarServicoUseCase iniciarServicoUseCase,
            FinalizarServicoUseCase finalizarServicoUseCase,
            ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase,
            FalharExecucaoUseCase falharExecucaoUseCase,
            ListarHistoricoOrdemServicoUseCase listarHistoricoOrdemServicoUseCase,
            OrdemServicoPresenter presenter) {
        this.criarOrdemServicoUseCase = criarOrdemServicoUseCase;
        this.consultarOrdemServicoUseCase = consultarOrdemServicoUseCase;
        this.listarOrdensServicoUseCase = listarOrdensServicoUseCase;
        this.listarPorVeiculoOrdemServicoUseCase = listarPorVeiculoOrdemServicoUseCase;
        this.iniciarDiagnosticoOrdemServicoUseCase = iniciarDiagnosticoOrdemServicoUseCase;
        this.iniciarExecucaoUseCase = iniciarExecucaoUseCase;
        this.finalizarOrdemServicoUseCase = finalizarOrdemServicoUseCase;
        this.entregarOrdemServicoUseCase = entregarOrdemServicoUseCase;
        this.listarServicosPorOSUseCase = listarServicosPorOSUseCase;
        this.iniciarServicoUseCase = iniciarServicoUseCase;
        this.finalizarServicoUseCase = finalizarServicoUseCase;
        this.concluirDiagnosticoUseCase = concluirDiagnosticoUseCase;
        this.falharExecucaoUseCase = falharExecucaoUseCase;
        this.listarHistoricoOrdemServicoUseCase = listarHistoricoOrdemServicoUseCase;
        this.presenter = presenter;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar todas as ordens de serviço",
        description = "Retorna todas as OSs. Use o parâmetro `status` para filtrar por situação.")
    @ApiResponse(responseCode = "200", description = "Lista de OSs")
    public ResponseEntity<List<OrdemServicoResponse>> listar(
        @RequestParam(required = false) OrdemServicoStatus status) {
        return ResponseEntity.ok(presenter.apresentar(listarOrdensServicoUseCase.executar(status)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Criar ordem de serviço", description = "Cria uma nova OS com status RECEBIDA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "OS criada com sucesso",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos de entrada",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Veículo ou cliente não encontrado",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody CriarOrdemServicoRequest request) {
        OrdemServico os = criarOrdemServicoUseCase.executar(request.getVeiculoId(), request.getClienteId());
        return ResponseEntity.status(201).body(presenter.apresentar(os));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar OS por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OS encontrada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "OS não encontrada",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentar(consultarOrdemServicoUseCase.executar(id)));
    }

    @GetMapping("/veiculo/{veiculoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar OSs por veículo")
    @ApiResponse(responseCode = "200", description = "Lista de OSs do veículo")
    public ResponseEntity<List<OrdemServicoResponse>> listarPorVeiculo(@PathVariable String veiculoId) {
        return ResponseEntity.ok(presenter.apresentar(listarPorVeiculoOrdemServicoUseCase.executar(veiculoId)));
    }

    // --- Transições de status da OS ---

    @PatchMapping("/{id}/iniciar-diagnostico")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Operation(summary = "Iniciar diagnóstico", description = "Transição: RECEBIDA → EM_DIAGNOSTICO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnóstico iniciado",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "409", description = "OS não está RECEBIDA",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentar(iniciarDiagnosticoOrdemServicoUseCase.executar(id)));
    }

    @PatchMapping("/{id}/concluir-diagnostico")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Transactional // o evento da Saga vai para a outbox na mesma transação
    @Operation(summary = "Concluir diagnóstico",
        description = "Publica DiagnosticoConcluido para o billing gerar o orçamento. A OS segue EM_DIAGNOSTICO até o orçamento ser gerado (AGUARDANDO_APROVACAO).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnóstico enviado para orçamento",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Sem itens, serviço inexistente/inativo ou insumo sem preço",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "OS não está EM_DIAGNOSTICO",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> concluirDiagnostico(
            @PathVariable String id,
            @Valid @RequestBody ConcluirDiagnosticoRequest request) {
        var input = new ConcluirDiagnosticoInput(
                request.servicos() == null ? List.of() : request.servicos().stream()
                        .map(s -> new ConcluirDiagnosticoInput.Servico(s.servicoId(), s.quantidade())).toList(),
                request.insumos() == null ? List.of() : request.insumos().stream()
                        .map(i -> new ConcluirDiagnosticoInput.Insumo(i.insumoId(), i.quantidade(), i.descricao(), i.precoUnitario())).toList(),
                request.observacoes());
        return ResponseEntity.ok(presenter.apresentar(concluirDiagnosticoUseCase.executar(id, input)));
    }

    @PatchMapping("/{id}/iniciar-execucao")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Transactional
    @Operation(summary = "Iniciar execução", description = "Transição: AGUARDANDO_EXECUCAO (pagamento confirmado) → EM_EXECUCAO. Publica ExecucaoIniciada. Opcionalmente recebe os IDs dos serviços para acompanhamento.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Execução iniciada",
            content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
        @ApiResponse(responseCode = "409", description = "OS não está AGUARDANDO_EXECUCAO",
            content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> iniciarExecucao(
            @PathVariable String id,
            @RequestBody(required = false) IniciarExecucaoRequest request) {
        List<String> servicoIds = request != null && request.getServicoIds() != null
                ? request.getServicoIds()
                : List.of();
        return ResponseEntity.ok(presenter.apresentar(iniciarExecucaoUseCase.executar(id, servicoIds)));
    }

    @PatchMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Transactional
    @Operation(summary = "Finalizar execução", description = "Transição: EM_EXECUCAO → FINALIZADA. Registra dataFinal e publica ExecucaoConcluida.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OS finalizada",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Serviços atrelados à OS não estão FINALIZADOS",
                content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "409", description = "OS não está EM_EXECUCAO",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> finalizar(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentar(finalizarOrdemServicoUseCase.executar(id)));
    }

    @PatchMapping("/{id}/falhar-execucao")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Transactional
    @Operation(summary = "Registrar falha na execução",
        description = "Transição: EM_EXECUCAO → CANCELADA. Publica ExecucaoFalhou: o inventory estorna os insumos e o billing reembolsa o pagamento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OS cancelada e compensações disparadas",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Motivo não informado",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "OS não está EM_EXECUCAO",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> falharExecucao(@PathVariable String id,
                                                               @Valid @RequestBody FalharExecucaoRequest request) {
        return ResponseEntity.ok(presenter.apresentar(falharExecucaoUseCase.executar(id, request.motivo())));
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Histórico da OS",
        description = "Eventos da Saga publicados e recebidos pela OS, em ordem, com o status antes e depois de cada um.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico da OS"),
            @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema()))
    })
    public ResponseEntity<List<HistoricoOrdemServicoResponse>> historico(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentarHistorico(listarHistoricoOrdemServicoUseCase.executar(id)));
    }

    @PatchMapping("/{id}/entregar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE')")
    @Operation(summary = "Registrar entrega do veículo", description = "Transição: FINALIZADA → ENTREGUE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Veículo entregue ao cliente",
                    content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class))),
            @ApiResponse(responseCode = "409", description = "OS não está FINALIZADA",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentar(entregarOrdemServicoUseCase.executar(id)));
    }

    // --- Serviços ---
    @GetMapping("/{id}/servico/listar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Operation(summary = "Listar todos os serviços atrelados à OS", description = "Mostra todos os serviços atrelados à uma OS e seus status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado com sucesso", content = @Content(schema = @Schema()))
    })
    public ResponseEntity<List<ServicoStatusResponse>> listarServicos(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentarServicos(listarServicosPorOSUseCase.executar(id)));
    }

    @PatchMapping("/servico/{servico_id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Operation(summary = "Iniciar um serviço por ID", description = "Altera o status do serviço específicado para: INICIADO")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Serviço iniciado com sucesso", content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(schema = @Schema()))
    })
    public ResponseEntity<ServicoStatusResponse> iniciarServico(@PathVariable String servico_id) {
        return ResponseEntity.ok(presenter.apresentarServico(iniciarServicoUseCase.executar(servico_id)));
    }

    @PatchMapping("/servico/{servico_id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICO')")
    @Operation(summary = "Finalizar um serviço por ID", description = "Altera o status do serviço específicado para: FINALIZADO")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Serviço finalizado com sucesso", content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(schema = @Schema()))
    })
    public ResponseEntity<ServicoStatusResponse> finalizarServico(@PathVariable String servico_id) {
        return ResponseEntity.ok(presenter.apresentarServico(finalizarServicoUseCase.executar(servico_id)));
    }
}
