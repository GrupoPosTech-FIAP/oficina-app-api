package com.mecanica.oficina_api.adapters.config;

import com.mecanica.oficina_api.application.cliente.gateway.ClienteGateway;
import com.mecanica.oficina_api.application.cliente.usecase.AlterarClienteUseCase;
import com.mecanica.oficina_api.application.cliente.usecase.CadastrarClienteUseCase;
import com.mecanica.oficina_api.application.cliente.usecase.ConsultarClienteUseCase;
import com.mecanica.oficina_api.application.cliente.usecase.DeletarClienteUseCase;
import com.mecanica.oficina_api.application.cliente.usecase.ListarClientesUseCase;
import com.mecanica.oficina_api.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
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
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarPorUsuarioOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarPorVeiculoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarServicosPorOSUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ProcessarEventoSagaUseCase;
import com.mecanica.oficina_api.application.servico.gateway.ServicoGateway;
import com.mecanica.oficina_api.application.servico.gateway.StatusServicoGateway;
import com.mecanica.oficina_api.application.servico.usecase.AlterarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.AtivarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.CadastrarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ConsultarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ConsultarTempoMedioUseCase;
import com.mecanica.oficina_api.application.servico.usecase.InativarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ListarServicosUseCase;
import com.mecanica.oficina_api.application.usuario.gateway.PasswordEncoder;
import com.mecanica.oficina_api.application.usuario.gateway.UsuarioGateway;
import com.mecanica.oficina_api.application.usuario.usecase.AlterarUsuarioUseCase;
import com.mecanica.oficina_api.application.usuario.usecase.CadastrarUsuarioUseCase;
import com.mecanica.oficina_api.application.usuario.usecase.ConsultarUsuarioUseCase;
import com.mecanica.oficina_api.application.usuario.usecase.InativarUsuarioUseCase;
import com.mecanica.oficina_api.application.veiculo.gateway.VeiculoGateway;
import com.mecanica.oficina_api.application.veiculo.usecase.AlterarVeiculoUseCase;
import com.mecanica.oficina_api.application.veiculo.usecase.CadastrarVeiculoUseCase;
import com.mecanica.oficina_api.application.veiculo.usecase.ConsultarVeiculoUseCase;
import com.mecanica.oficina_api.application.veiculo.usecase.InativarVeiculoUseCase;
import com.mecanica.oficina_api.application.veiculo.usecase.ListarVeiculosPorClienteUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    /* Cliente */
    @Bean
    CadastrarClienteUseCase cadastrarClienteUseCase(ClienteGateway gateway) {
        return new CadastrarClienteUseCase(gateway);
    }
    @Bean
    ConsultarClienteUseCase consultarClienteUseCase(ClienteGateway gateway) {
        return new ConsultarClienteUseCase(gateway);
    }
    @Bean
    ListarClientesUseCase listarClientesUseCase(ClienteGateway gateway) {
        return new ListarClientesUseCase(gateway);
    }
    @Bean
    AlterarClienteUseCase alterarClienteUseCase(ClienteGateway gateway) {
        return new AlterarClienteUseCase(gateway);
    }
    @Bean
    DeletarClienteUseCase deletarClienteUseCase(ClienteGateway gateway) {
        return new DeletarClienteUseCase(gateway);
    }

    /* Serviço */
    @Bean
    CadastrarServicoUseCase cadastrarServicoUseCase(ServicoGateway servicoGateway) {
        return new CadastrarServicoUseCase(servicoGateway);
    }
    @Bean
    ConsultarServicoUseCase consultarServicoUseCase(ServicoGateway servicoGateway) {
        return new ConsultarServicoUseCase(servicoGateway);
    }
    @Bean
    ConsultarTempoMedioUseCase consultarTempoMedioUseCase(ServicoGateway servicoGateway, StatusServicoGateway statusServicoGateway) {
        return new ConsultarTempoMedioUseCase(servicoGateway, statusServicoGateway);
    }
    @Bean
    ListarServicosUseCase listarServicosUseCase(ServicoGateway servicoGateway) {
        return new ListarServicosUseCase(servicoGateway);
    }
    @Bean
    AlterarServicoUseCase alterarServicoUseCase(ServicoGateway servicoGateway) {
        return new AlterarServicoUseCase(servicoGateway);
    }
    @Bean
    AtivarServicoUseCase ativarServicoUseCase(ServicoGateway servicoGateway) {
        return new AtivarServicoUseCase(servicoGateway);
    }
    @Bean
    InativarServicoUseCase inativarServicoUseCase(ServicoGateway servicoGateway) {
        return new InativarServicoUseCase(servicoGateway);
    }

    /* Usuário */
    @Bean
    CadastrarUsuarioUseCase cadastrarUsuarioUseCase(UsuarioGateway usuarioGateway, PasswordEncoder passwordEncoder) {
        return new CadastrarUsuarioUseCase(usuarioGateway, passwordEncoder);
    }
    @Bean
    ConsultarUsuarioUseCase consultarUsuarioUseCase(UsuarioGateway usuarioGateway) {
        return new ConsultarUsuarioUseCase(usuarioGateway);
    }
    @Bean
    AlterarUsuarioUseCase alterarUsuarioUseCase(UsuarioGateway usuarioGateway) {
        return new AlterarUsuarioUseCase(usuarioGateway);
    }
    @Bean
    InativarUsuarioUseCase inativarUsuarioUseCase(UsuarioGateway usuarioGateway) {
        return new InativarUsuarioUseCase(usuarioGateway);
    }

    /* Veículo */
    @Bean
    CadastrarVeiculoUseCase cadastrarVeiculoUseCase(VeiculoGateway veiculoGateway, ClienteGateway clienteGateway) {
        return new CadastrarVeiculoUseCase(veiculoGateway, clienteGateway);
    }
    @Bean
    ConsultarVeiculoUseCase consultarVeiculoUseCase(VeiculoGateway veiculoGateway) {
        return new ConsultarVeiculoUseCase(veiculoGateway);
    }
    @Bean
    ListarVeiculosPorClienteUseCase listarVeiculosPorClienteUseCase(VeiculoGateway veiculoGateway) {
        return new ListarVeiculosPorClienteUseCase(veiculoGateway);
    }
    @Bean
    AlterarVeiculoUseCase alterarVeiculoUseCase(VeiculoGateway veiculoGateway) {
        return new AlterarVeiculoUseCase(veiculoGateway);
    }
    @Bean
    InativarVeiculoUseCase inativarVeiculoUseCase(VeiculoGateway veiculoGateway) {
        return new InativarVeiculoUseCase(veiculoGateway);
    }

    /* Ordem de Serviço */
    @Bean
    CriarOrdemServicoUseCase criarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorClienteGateway notificadorClienteGateway, VeiculoGateway veiculoGateway, ClienteGateway clienteGateway) {
        return new CriarOrdemServicoUseCase(ordemServicoGateway, notificadorClienteGateway, veiculoGateway, clienteGateway);
    }
    @Bean
    ConsultarOrdemServicoUseCase consultarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        return new ConsultarOrdemServicoUseCase(ordemServicoGateway);
    }
    @Bean
    ListarOrdensServicoUseCase listarOrdensServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        return new ListarOrdensServicoUseCase(ordemServicoGateway);
    }
    @Bean
    ListarPorVeiculoOrdemServicoUseCase listarPorVeiculoOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        return new ListarPorVeiculoOrdemServicoUseCase(ordemServicoGateway);
    }
    @Bean
    ListarPorUsuarioOrdemServicoUseCase listarPorUsuarioOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        return new ListarPorUsuarioOrdemServicoUseCase(ordemServicoGateway);
    }
    @Bean
    IniciarDiagnosticoOrdemServicoUseCase iniciarDiagnosticoOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorClienteGateway notificadorClienteGateway) {
        return new IniciarDiagnosticoOrdemServicoUseCase(ordemServicoGateway, notificadorClienteGateway);
    }
    @Bean
    IniciarExecucaoUseCase iniciarExecucaoUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorClienteGateway notificadorClienteGateway, StatusServicoGateway statusServicoGateway, RegistroEventosOrdemServico registroEventos, Clock clock) {
        return new IniciarExecucaoUseCase(ordemServicoGateway, notificadorClienteGateway, statusServicoGateway, registroEventos, clock);
    }
    @Bean
    FinalizarOrdemServicoUseCase finalizarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway, StatusServicoGateway statusServicoGateway, NotificadorClienteGateway notificadorClienteGateway, RegistroEventosOrdemServico registroEventos) {
        return new FinalizarOrdemServicoUseCase(ordemServicoGateway, statusServicoGateway, notificadorClienteGateway, registroEventos);
    }
    @Bean
    EntregarOrdemServicoUseCase entregarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorClienteGateway notificadorClienteGateway) {
        return new EntregarOrdemServicoUseCase(ordemServicoGateway, notificadorClienteGateway);
    }
    @Bean
    ListarServicosPorOSUseCase listarServicosPorOSUseCase(StatusServicoGateway statusServicoGateway) {
        return new ListarServicosPorOSUseCase(statusServicoGateway);
    }
    @Bean
    IniciarServicoUseCase iniciarServicoUseCase(StatusServicoGateway statusServicoGateway) {
        return new IniciarServicoUseCase(statusServicoGateway);
    }
    @Bean
    FinalizarServicoUseCase finalizarServicoUseCase(StatusServicoGateway statusServicoGateway) {
        return new FinalizarServicoUseCase(statusServicoGateway);
    }

    /* Saga (ADR 005) */
    @Bean
    RegistroEventosOrdemServico registroEventosOrdemServico(PublicadorEventosGateway publicador, HistoricoOrdemServicoGateway historicoGateway, Clock clock) {
        return new RegistroEventosOrdemServico(publicador, historicoGateway, clock);
    }
    @Bean
    ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase(OrdemServicoGateway ordemServicoGateway, ServicoGateway servicoGateway, RegistroEventosOrdemServico registroEventos) {
        return new ConcluirDiagnosticoUseCase(ordemServicoGateway, servicoGateway, registroEventos);
    }
    @Bean
    FalharExecucaoUseCase falharExecucaoUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorClienteGateway notificadorClienteGateway, RegistroEventosOrdemServico registroEventos) {
        return new FalharExecucaoUseCase(ordemServicoGateway, notificadorClienteGateway, registroEventos);
    }
    @Bean
    ListarHistoricoOrdemServicoUseCase listarHistoricoOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway, HistoricoOrdemServicoGateway historicoGateway) {
        return new ListarHistoricoOrdemServicoUseCase(ordemServicoGateway, historicoGateway);
    }
    @Bean
    ProcessarEventoSagaUseCase processarEventoSagaUseCase(OrdemServicoGateway ordemServicoGateway, HistoricoOrdemServicoGateway historicoGateway, NotificadorClienteGateway notificadorClienteGateway, Clock clock) {
        return new ProcessarEventoSagaUseCase(ordemServicoGateway, historicoGateway, notificadorClienteGateway, clock);
    }
}
