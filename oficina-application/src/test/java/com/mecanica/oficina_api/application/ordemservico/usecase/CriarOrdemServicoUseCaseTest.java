package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.cliente.gateway.ClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.veiculo.gateway.VeiculoGateway;
import com.mecanica.oficina_api.domain.cliente.Cliente;
import com.mecanica.oficina_api.domain.cliente.Cpf;
import com.mecanica.oficina_api.domain.cliente.Email;
import com.mecanica.oficina_api.domain.cliente.Telefone;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.veiculo.Veiculo;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CriarOrdemServicoUseCaseTest {

    @Mock
    private OrdemServicoGateway ordemServicoGateway;

    @Mock
    private VeiculoGateway veiculoGateway;

    @Mock
    private ClienteGateway clienteGateway;

    @Mock
    private NotificadorClienteGateway notificadorClienteGateway;

    @InjectMocks
    private CriarOrdemServicoUseCase useCase;

    @Test
    void deveCriarOrdemServicoComSucesso() {
        Veiculo veiculo = Veiculo.reconstituir(VEICULO_ID, CLIENTE_ID, "ABC1D23", "Fiat", "Uno", 2020, "Prata", true);
        Cliente cliente = Cliente.reconstituir(CLIENTE_ID, "João Silva", new Cpf("37518712091"),
            new Email("cliente@teste.com"), new Telefone("5437891237"), LocalDateTime.now(), null);
        when(veiculoGateway.buscar(VEICULO_ID)).thenReturn(Optional.of(veiculo));
        when(clienteGateway.findByIdAtivo(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(ordemServicoGateway.cadastrar(VEICULO_ID, CLIENTE_ID)).thenReturn(osSemOrcamento(OrdemServicoStatus.RECEBIDA));

        OrdemServico resultado = useCase.executar(VEICULO_ID, CLIENTE_ID);

        assertThat(resultado.getStatus()).isEqualTo(OrdemServicoStatus.RECEBIDA);
        verify(ordemServicoGateway).cadastrar(VEICULO_ID, CLIENTE_ID);
    }

    @Test
    void deveLancarExcecao_quandoVeiculoNaoEncontrado() {
        when(veiculoGateway.buscar("veic-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("veic-x", CLIENTE_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Veículo não encontrado");

        verify(ordemServicoGateway, never()).cadastrar(anyString(), anyString());
    }

    @Test
    void deveLancarExcecao_quandoClienteNaoEncontrado() {
        Veiculo veiculo = Veiculo.reconstituir(VEICULO_ID, CLIENTE_ID, "ABC1D23", "Fiat", "Uno", 2020, "Prata", true);
        when(veiculoGateway.buscar(VEICULO_ID)).thenReturn(Optional.of(veiculo));
        when(clienteGateway.findByIdAtivo("cli-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(VEICULO_ID, "cli-x"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cliente não encontrado");

        verify(ordemServicoGateway, never()).cadastrar(anyString(), anyString());
    }
}
