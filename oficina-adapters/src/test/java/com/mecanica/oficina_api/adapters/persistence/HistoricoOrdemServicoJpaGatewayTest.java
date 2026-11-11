package com.mecanica.oficina_api.adapters.persistence;

import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(HistoricoOrdemServicoJpaGateway.class)
@Transactional
class HistoricoOrdemServicoJpaGatewayTest {

    @Autowired
    private HistoricoOrdemServicoJpaGateway gateway;

    @Test
    void registraEListaEmOrdemDeChegada() {
        Instant t = Instant.parse("2026-11-08T10:00:00Z");
        var publicado = new HistoricoOrdemServico("os-1", "DiagnosticoConcluido", HistoricoOrdemServico.Origem.PUBLICADO,
                null, OrdemServicoStatus.EM_DIAGNOSTICO, false, null, t);
        var recebido = new HistoricoOrdemServico("os-1", "OrcamentoGerado", HistoricoOrdemServico.Origem.RECEBIDO,
                OrdemServicoStatus.EM_DIAGNOSTICO, OrdemServicoStatus.AGUARDANDO_APROVACAO, false, "{\"valorTotal\":260}", t);
        gateway.registrar(publicado);
        gateway.registrar(recebido);
        gateway.registrar(new HistoricoOrdemServico("os-2", "OrcamentoGerado", HistoricoOrdemServico.Origem.RECEBIDO,
                OrdemServicoStatus.RECEBIDA, OrdemServicoStatus.RECEBIDA, true, null, t));

        assertThat(gateway.listarPorOrdemServico("os-1")).containsExactly(publicado, recebido);
    }
}
