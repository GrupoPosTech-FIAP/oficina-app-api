package com.mecanica.oficina_api.adapters.web.dto.request;

import java.util.List;

public class IniciarExecucaoRequest {

    private List<String> servicoIds;

    public IniciarExecucaoRequest() {}

    public IniciarExecucaoRequest(List<String> servicoIds) {
        this.servicoIds = servicoIds;
    }

    public List<String> getServicoIds() { return servicoIds; }
    public void setServicoIds(List<String> servicoIds) { this.servicoIds = servicoIds; }
}

