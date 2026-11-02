package com.mecanica.oficina_api.adapters.persistence;

import com.mecanica.oficina_api.adapters.persistence.repository.ClienteSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.VeiculoSpringDataRepository;
import com.mecanica.oficina_api.application.ordemservico.output.MinhaOrdemServicoOutput;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(OrdemServicoJpaGateway.class)
@Transactional
class OrdemServicoJpaGatewayTest {

    @Autowired
    private OrdemServicoJpaGateway gateway;

    @Autowired
    private ClienteSpringDataRepository clienteRepo;

    @Autowired
    private VeiculoSpringDataRepository veiculoRepo;

    private String seedClienteEVeiculo() {
        ClienteJpaEntity cliente = new ClienteJpaEntity();
        cliente.setId("cliente-1");
        cliente.setNome("Maria");
        cliente.setDocumento("52998224725");
        cliente.setEmail("maria@email.com");
        cliente.setTelefone("11999999999");
        cliente.setDataCadastro(LocalDateTime.now());
        cliente.setAtivo(true);
        // saveAndFlush garante a linha persistida e uma instância gerenciada para o relacionamento
        ClienteJpaEntity clienteSalvo = clienteRepo.saveAndFlush(cliente);

        VeiculoJpaEntity veiculo = new VeiculoJpaEntity();
        veiculo.setPlaca("ABC1D23");
        veiculo.setMarca("Ford");
        veiculo.setModelo("Ka");
        veiculo.setAno(2020);
        veiculo.setCor("Prata");
        veiculo.setAtivo(true);
        veiculo.setCliente(clienteSalvo);
        return veiculoRepo.saveAndFlush(veiculo).getId();
    }

    @Test
    void deveCadastrarOrdemComStatusRecebida() {
        OrdemServico salvo = gateway.cadastrar("veiculo-1", "cliente-1");

        assertThat(salvo.getId()).isNotBlank();
        assertThat(salvo.getVeiculoId()).isEqualTo("veiculo-1");
        assertThat(salvo.getClienteId()).isEqualTo("cliente-1");
        assertThat(salvo.getStatus()).isEqualTo(OrdemServicoStatus.RECEBIDA);
    }

    @Test
    void deveAtualizarStatusDaOrdem() {
        OrdemServico salvo = gateway.cadastrar("veiculo-1", "cliente-1");

        OrdemServico emDiagnostico = gateway.buscar(salvo.getId()).orElseThrow();
        emDiagnostico.iniciarDiagnostico();

        gateway.atualizar(emDiagnostico);

        OrdemServico lido = gateway.buscar(salvo.getId()).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(OrdemServicoStatus.EM_DIAGNOSTICO);
    }

    @Test
    void deveLancarExcecao_quandoAtualizarOrdemInexistente() {
        OrdemServico inexistente = OrdemServico.reconstituir("inexistente", "veiculo-1",
                "cliente-1", OrdemServicoStatus.RECEBIDA, null, null);

        // @Repository: a IllegalArgumentException interna é traduzida pelo Spring, mantendo a causa raiz
        assertThatThrownBy(() -> gateway.atualizar(inexistente))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    void deveBuscarOrdemExistente() {
        OrdemServico salvo = gateway.cadastrar("veiculo-1", "cliente-1");

        Optional<OrdemServico> encontrado = gateway.buscar(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getVeiculoId()).isEqualTo("veiculo-1");
    }

    @Test
    void deveRetornarVazio_quandoOrdemNaoExiste() {
        assertThat(gateway.buscar("inexistente")).isEmpty();
    }

    @Test
    void deveListarPorStatus() {
        gateway.cadastrar("veiculo-1", "cliente-1");
        gateway.cadastrar("veiculo-2", "cliente-1");

        List<OrdemServico> recebidas = gateway.listar(OrdemServicoStatus.RECEBIDA);
        assertThat(recebidas).hasSize(2);

        List<OrdemServico> finalizadas = gateway.listar(OrdemServicoStatus.FINALIZADA);
        assertThat(finalizadas).isEmpty();
    }

    @Test
    void deveListarTodas_quandoStatusNulo() {
        gateway.cadastrar("veiculo-1", "cliente-1");
        gateway.cadastrar("veiculo-2", "cliente-1");

        assertThat(gateway.listar(null)).hasSize(2);
    }

    @Test
    void deveListarPorVeiculo() {
        gateway.cadastrar("veiculo-1", "cliente-1");
        gateway.cadastrar("veiculo-1", "cliente-1");
        gateway.cadastrar("veiculo-2", "cliente-1");

        List<OrdemServico> doVeiculo = gateway.listarPorVeiculo("veiculo-1");

        assertThat(doVeiculo).hasSize(2);
        assertThat(doVeiculo).allMatch(os -> os.getVeiculoId().equals("veiculo-1"));
    }

    @Test
    void deveBuscarPorCliente_projetandoVeiculo() {
        String veiculoId = seedClienteEVeiculo();
        gateway.cadastrar(veiculoId, "cliente-1");

        List<MinhaOrdemServicoOutput> minhas = gateway.buscarPorCliente("cliente-1");

        assertThat(minhas).hasSize(1);
        assertThat(minhas.get(0).status()).isEqualTo("RECEBIDA");
        assertThat(minhas.get(0).veiculo()).isNotNull();
        assertThat(minhas.get(0).veiculo().placa()).isEqualTo("ABC1D23");
        assertThat(minhas.get(0).veiculo().marca()).isEqualTo("Ford");
    }

    @Test
    void deveBuscarPorCliente_comVeiculoNulo_quandoVeiculoNaoExiste() {
        gateway.cadastrar("veiculo-inexistente", "cliente-1");

        List<MinhaOrdemServicoOutput> minhas = gateway.buscarPorCliente("cliente-1");

        assertThat(minhas).hasSize(1);
        assertThat(minhas.get(0).veiculo()).isNull();
    }
}
