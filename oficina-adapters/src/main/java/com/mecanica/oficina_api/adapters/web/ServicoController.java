package com.mecanica.oficina_api.adapters.web;

import com.mecanica.oficina_api.adapters.web.dto.request.AlterarServicoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.CadastrarServicoRequest;
import com.mecanica.oficina_api.adapters.web.dto.response.ServicoResponse;
import com.mecanica.oficina_api.adapters.web.dto.response.ValidationErrorResponse;
import com.mecanica.oficina_api.adapters.web.presenter.ServicoPresenter;
import com.mecanica.oficina_api.application.servico.usecase.AlterarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.AtivarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.CadastrarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ConsultarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.InativarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ListarServicosUseCase;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("servico")
@Tag(name = "Serviço", description = "Catálogo de serviços oferecidos pela oficina")
@SecurityRequirement(name = "bearerAuth")
public class ServicoController {

    private final CadastrarServicoUseCase cadastrarServicoUseCase;
    private final ConsultarServicoUseCase consultarServicoUseCase;
    private final ListarServicosUseCase listarServicosUseCase;
    private final AlterarServicoUseCase alterarServicoUseCase;
    private final AtivarServicoUseCase ativarServicoUseCase;
    private final InativarServicoUseCase inativarServicoUseCase;
    private final ServicoPresenter presenter;

    public ServicoController(CadastrarServicoUseCase cadastrarServicoUseCase,
                             ConsultarServicoUseCase consultarServicoUseCase,
                             ListarServicosUseCase listarServicosUseCase,
                             AlterarServicoUseCase alterarServicoUseCase,
                             AtivarServicoUseCase ativarServicoUseCase,
                             InativarServicoUseCase inativarServicoUseCase,
                             ServicoPresenter presenter) {
        this.cadastrarServicoUseCase = cadastrarServicoUseCase;
        this.consultarServicoUseCase = consultarServicoUseCase;
        this.listarServicosUseCase = listarServicosUseCase;
        this.alterarServicoUseCase = alterarServicoUseCase;
        this.ativarServicoUseCase = ativarServicoUseCase;
        this.inativarServicoUseCase = inativarServicoUseCase;
        this.presenter = presenter;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastrar serviço", description = "Cadastra um novo serviço no catálogo da oficina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Serviço cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos de entrada",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    public ResponseEntity<ServicoResponse> cadastrar(@Valid @RequestBody CadastrarServicoRequest request) {
        var servico = cadastrarServicoUseCase.executar(
            request.getNome(), request.getDescricao(),
            request.getPreco(), request.getTempoEstimadoHoras()
        );
        return ResponseEntity.status(201).body(presenter.apresentar(servico));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar serviço por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço encontrado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<ServicoResponse> buscar(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentar(consultarServicoUseCase.executar(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar serviços ativos", description = "Retorna todos os serviços ativos do catálogo")
    @ApiResponse(responseCode = "200", description = "Lista de serviços ativos")
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(presenter.apresentar(listarServicosUseCase.executar()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar serviço", description = "Atualiza os dados de um serviço existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço atualizado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos de entrada",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<ServicoResponse> alterar(@PathVariable String id,
                                                   @Valid @RequestBody AlterarServicoRequest request) {
        var servico = alterarServicoUseCase.executar(
            id, request.getNome(), request.getDescricao(),
            request.getPreco(), request.getTempoEstimadoHoras()
        );
        return ResponseEntity.ok(presenter.apresentar(servico));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativar serviço", description = "Reativa um serviço previamente inativado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço ativado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<ServicoResponse> ativar(@PathVariable String id) {
        return ResponseEntity.ok(presenter.apresentar(ativarServicoUseCase.executar(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativar serviço", description = "Inativa um serviço do catálogo (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Serviço inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema()))
    })
    public ResponseEntity<Void> inativar(@PathVariable String id) {
        inativarServicoUseCase.executar(id);
        return ResponseEntity.noContent().build();
    }
}
