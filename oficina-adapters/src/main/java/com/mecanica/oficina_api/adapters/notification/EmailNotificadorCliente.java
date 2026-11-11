package com.mecanica.oficina_api.adapters.notification;

import com.mecanica.oficina_api.adapters.persistence.repository.ClienteSpringDataRepository;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class EmailNotificadorCliente implements NotificadorClienteGateway {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificadorCliente.class);

    private final JavaMailSender mailSender;
    private final ClienteSpringDataRepository clienteRepo;
    private final String from;

    public EmailNotificadorCliente(JavaMailSender mailSender,
                                   ClienteSpringDataRepository clienteRepo,
                                   @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.clienteRepo = clienteRepo;
        this.from = from;
    }

    @Override
    public void notificar(NotificacaoCliente n) {
        String emailDestino = clienteRepo.findById(n.clienteId())
                .map(c -> c.getEmail())
                .orElse(null);

        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("[EMAIL_NOTIFICACAO_IGNORADA] cliente_id={} motivo=email_nao_encontrado", n.clienteId());
            return;
        }

        String assunto = resolverAssunto(n);
        String corpo = resolverCorpo(n);

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(from);
        mensagem.setTo(emailDestino);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);

        mailSender.send(mensagem);
        log.info("[EMAIL_ENVIADO] tipo_evento={} ordem_servico_id={} cliente_id={}",
                n.tipo().chaveLog(), n.ordemServicoId(), n.clienteId());
    }

    private String resolverAssunto(NotificacaoCliente n) {
        return switch (n.tipo()) {
            case RECEBIDA -> "Oficina — Ordem de Serviço criada (OS " + n.ordemServicoId() + ")";
            case EM_DIAGNOSTICO -> "Oficina — Ordem de Serviço em diagnostico (OS " + n.ordemServicoId() + ")";
            case ENVIO_ORCAMENTO -> "Oficina — Orçamento disponível para aprovação (OS " + n.ordemServicoId() + ")";
            case EM_EXECUCAO -> "Oficina — Ordem de Serviço em execução (OS " + n.ordemServicoId() + ")";
            case FINALIZACAO_OS  -> "Oficina — Seu veículo está pronto para retirada (OS " + n.ordemServicoId() + ")";
            case ENTREGUE -> "Oficina — Veículo entregue (OS " + n.ordemServicoId() + ")";
            case CANCELADA -> "Oficina — Ordem de Serviço cancelada (OS " + n.ordemServicoId() + ")";
        };
    }

    private String resolverCorpo(NotificacaoCliente n) {
        return switch (n.tipo()) {
            case RECEBIDA -> """
            Olá,

            Recebemos seu veículo e a Ordem de Serviço %s foi aberta com sucesso.
            Nossa equipe iniciará a avaliação para identificar os serviços necessários.

            Você será informado sobre cada nova etapa do atendimento.

            Atenciosamente,
            Equipe Oficina
            """.formatted(n.ordemServicoId());

            case EM_DIAGNOSTICO -> """
            Olá,

            A Ordem de Serviço %s entrou na etapa de diagnóstico.
            Nossa equipe técnica está realizando uma avaliação detalhada do veículo para identificar os reparos e serviços necessários.

            Assim que o diagnóstico for concluído, você receberá as próximas informações sobre o atendimento.

            Atenciosamente,
            Equipe Oficina
            """.formatted(n.ordemServicoId());

            case ENVIO_ORCAMENTO -> """
            Olá,

            O orçamento da sua ordem de serviço %s está disponível para aprovação.
            Para aprová-la acesse o sistema em:
            /ordem-servico/%s/orcamento/aprovar
            
            Para nega-la acesse o sistema em:
            /ordem-servico/%s/orcamento/negar
            
            Atenciosamente,
            Equipe Oficina
            """.formatted(
                    n.ordemServicoId(),
                    n.ordemServicoId(),
                    n.ordemServicoId()
            );

            case EM_EXECUCAO -> """
            Olá,

            Sua ordem de serviço %s foi aprovada e os serviços já estão em execução.
            Nossa equipe está trabalhando para concluir o atendimento com qualidade e no menor tempo possível.

            Entraremos em contato assim que o serviço for finalizado.

            Atenciosamente,
            Equipe Oficina
            """.formatted(n.ordemServicoId());

            case FINALIZACAO_OS -> """
            Olá,

            O serviço referente à ordem %s foi concluído e seu veículo está pronto para retirada.
            Entre em contato com a oficina para agendar a busca.

            Atenciosamente,
            Equipe Oficina
            """.formatted(n.ordemServicoId());

            case ENTREGUE -> """
            Olá,

            Registramos a entrega do seu veículo referente à ordem de serviço %s.
            Agradecemos a confiança em nossa oficina e esperamos atendê-lo novamente quando precisar.

            Tenha uma ótima viagem!

            Atenciosamente,
            Equipe Oficina
            """.formatted(n.ordemServicoId());

            case CANCELADA -> """
            Olá,

            A ordem de serviço %s foi cancelada. Se algum valor foi pago ou algum insumo foi
            reservado, o estorno é feito automaticamente.

            Em caso de dúvidas, entre em contato com a oficina.

            Atenciosamente,
            Equipe Oficina
            """.formatted(n.ordemServicoId());
        };
    }
}
