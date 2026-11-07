package com.mecanica.oficina_api.infrastructure.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_api.adapters.persistence.ClienteJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.UsuarioJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.ClienteSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.OrdemServicoSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.ServicoSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.UsuarioSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.VeiculoSpringDataRepository;
import com.mecanica.oficina_api.adapters.web.dto.request.LoginRequest;
import com.mecanica.oficina_api.adapters.web.dto.response.LoginResponse;
import com.mecanica.oficina_api.domain.usuario.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest extends PostgresTestcontainersSupport {

    @Autowired private WebApplicationContext context;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired protected UsuarioSpringDataRepository usuarioRepository;
    @Autowired protected ClienteSpringDataRepository clienteRepository;
    @Autowired protected VeiculoSpringDataRepository veiculoRepository;
    @Autowired protected OrdemServicoSpringDataRepository ordemServicoRepository;
    @Autowired protected ServicoSpringDataRepository servicoRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected MockMvc mockMvc;
    protected String adminToken;

    @BeforeEach
    void limparEConfigurar() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        ordemServicoRepository.deleteAll();
        servicoRepository.deleteAllInBatch();
        veiculoRepository.deleteAllInBatch();
        clienteRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();

        usuarioRepository.save(usuario("Admin Testes", "admin@integration.test", "admin123", Perfil.ADMIN, null));

        adminToken = login("admin@integration.test", "admin123");
    }

    protected String login(String email, String senha) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new LoginRequest(email, senha))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, LoginResponse.class).token();
    }

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    protected MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + adminToken);
    }

    protected MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }

    protected UsuarioJpaEntity usuario(String nome, String email, String senha, Perfil perfil, String clienteId) {
        UsuarioJpaEntity u = new UsuarioJpaEntity();
        u.setId(UUID.randomUUID().toString());
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(passwordEncoder.encode(senha));
        u.setPerfil(perfil);
        u.setClienteId(clienteId);
        u.setDataCadastro(LocalDateTime.now());
        u.setDataAtualizacao(LocalDateTime.now());
        u.setAtivo(true);
        return u;
    }

    protected ClienteJpaEntity cliente(String nome, String cpf, String email) {
        ClienteJpaEntity c = new ClienteJpaEntity();
        c.setId(UUID.randomUUID().toString());
        c.setNome(nome);
        c.setDocumento(cpf);
        c.setEmail(email);
        c.setTelefone("11999990000");
        c.setDataCadastro(LocalDateTime.now());
        c.setAtivo(true);
        return c;
    }
}
