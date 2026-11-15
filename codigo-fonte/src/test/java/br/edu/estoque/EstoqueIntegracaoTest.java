package br.edu.estoque;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:teste;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class EstoqueIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void cadastroFiltroCombinadoAtualizacaoERemocao() throws Exception {
        long marca = marca("Toyota");
        long modelo = modelo(marca, "Corolla");
        long primeiro = veiculo(modelo, 85000);
        long segundo = veiculo(modelo, 99000);
        long marcaExtra = marca("Outra");
        long modeloExtra = modelo(marcaExtra, "Corolla");
        veiculo(modeloExtra, 85000);

        // O segundo veículo excede o teto e o terceiro compartilha o nome, mas pertence a outra marca.
        JsonNode filtrados = resposta(get("/api/veiculos")
                .param("marcaId", String.valueOf(marca)).param("modeloId", String.valueOf(modelo))
                .param("ano", "2022").param("status", "DISPONIVEL").param("tipo", "CARRO")
                .param("precoMin", "85000").param("precoMax", "85000"), 200);
        assertThat(filtrados.size()).isEqualTo(1);
        assertThat(filtrados.get(0).get("id").asLong()).isEqualTo(primeiro);
        resposta(delete("/api/veiculos/" + primeiro), 409);
        resposta(corpo(patch("/api/veiculos/" + primeiro),
                "{\"quilometragem\":36000,\"preco\":84000.50,\"status\":\"VENDIDO\"}"), 200);
        JsonNode alterado = resposta(get("/api/veiculos/" + primeiro), 200);
        assertThat(alterado.get("status").asText()).isEqualTo("VENDIDO");
        assertThat(alterado.get("preco").decimalValue()).isEqualByComparingTo("84000.50");
        assertThat(alterado.get("quilometragem").asInt()).isEqualTo(36000);
        mvc.perform(delete("/api/veiculos/" + primeiro)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        resposta(get("/api/veiculos/" + primeiro), 404);
        assertThat(resposta(get("/api/veiculos/" + segundo), 200).get("status").asText()).isEqualTo("DISPONIVEL");
    }

    @Test
    void edicaoInvalidaPreservaOsDados() throws Exception {
        long id = veiculo(modelo(marca("Honda"), "Civic"), 78000);
        String url = "/api/veiculos/" + id;
        for (String proibido : new String[] {
                "{\"preco\":100,\"quilometragem\":34000}", "{\"preco\":-1}", "{\"preco\":0.001}",
                "{\"preco\":null,\"status\":\"VENDIDO\"}", "{}", "{\"cor\":\"Azul\"}",
                "{\"quilometragem\":35000.5}", "{\"status\":1}"
        }) {
            resposta(corpo(patch(url), proibido), 400);
        }
        JsonNode veiculo = resposta(get(url), 200);
        assertThat(veiculo.get("preco").decimalValue()).isEqualByComparingTo("78000.00");
        assertThat(veiculo.get("quilometragem").asInt()).isEqualTo(35000);
        assertThat(veiculo.get("status").asText()).isEqualTo("DISPONIVEL");

        resposta(corpo(patch(url), "{\"status\":\"DESCONTINUADO\"}"), 200);
        mvc.perform(delete(url)).andExpect(status().isNoContent());
    }

    @Test
    void validaCatalogoEParametrosInvalidos() throws Exception {
        long id = marca("Fiat");
        modelo(id, "Argo");
        resposta(corpo(post("/api/marcas"), "{\"nome\":\"  FIAT  \"}"), 409);
        resposta(corpo(post("/api/modelos"), "{\"marcaId\":" + id + ",\"nome\":\"argo\"}"), 409);
        resposta(corpo(post("/api/modelos"), "{\"marcaId\":999999,\"nome\":\"Novo\"}"), 404);
        resposta(corpo(post("/api/marcas"), "{\"nome\":\"   \"}"), 400);
        resposta(get("/api/veiculos").param("precoMin","100").param("precoMax","50"), 400);
        resposta(get("/api/veiculos").param("status","SEM_STATUS"), 400);
        resposta(get("/api/veiculos").param("ano","1800"), 400);
    }

    @Test
    void excluiMarcaLivreEProtegeOsVinculosDoCatalogo() throws Exception {
        long livre = marca("Marca sem modelos");
        String urlLivre = "/api/marcas/" + livre;
        mvc.perform(delete(urlLivre)).andExpect(status().isNoContent()).andExpect(content().string(""));
        resposta(delete(urlLivre), 404);
        resposta(get(urlLivre), 404);

        long vinculada = marca("Marca com modelos");
        long modelo = modelo(vinculada, "Modelo de teste");
        String urlVinculada = "/api/marcas/" + vinculada;
        JsonNode conflito = resposta(delete(urlVinculada), 409);
        assertThat(conflito.get("mensagem").asText()).contains("modelos vinculados");
        assertThat(resposta(get("/api/modelos/" + modelo), 200).get("marcaId").asLong()).isEqualTo(vinculada);

        long idVeiculo = veiculo(modelo, 85000);
        resposta(delete(urlVinculada), 409);
        assertThat(resposta(get(urlVinculada), 200).get("id").asLong()).isEqualTo(vinculada);
        JsonNode preservado = resposta(get("/api/veiculos/" + idVeiculo), 200);
        assertThat(preservado.get("modeloId").asLong()).isEqualTo(modelo);
        assertThat(preservado.get("marcaId").asLong()).isEqualTo(vinculada);
    }

    @Test
    void corrigeNomeDaMarcaEConservaModeloEVeiculo() throws Exception {
        long marca = marca("Renaut");
        long modelo = modelo(marca, "Clio");
        long veiculo = veiculo(modelo, 38000);
        marca("Peugeot");
        String url = "/api/marcas/" + marca;
        JsonNode alterada = resposta(corpo(patch(url), "{\"nome\":\"  Renault  \"}"), 200);
        assertThat(alterada.get("id").asLong()).isEqualTo(marca);
        assertThat(alterada.get("nome").asText()).isEqualTo("Renault");
        resposta(corpo(patch(url), "{\"nome\":\"Renault\"}"), 200);
        resposta(corpo(patch(url), "{\"nome\":\" peuGEOT \"}"), 409);
        resposta(corpo(patch(url), "{\"nome\":\"   \"}"), 400);
        resposta(corpo(patch("/api/marcas/999999"), "{\"nome\":\"Outra marca\"}"), 404);
        assertThat(resposta(get(url), 200).get("nome").asText()).isEqualTo("Renault");
        JsonNode mesmoModelo = resposta(get("/api/modelos/" + modelo), 200);
        assertThat(mesmoModelo.get("marcaId").asLong()).isEqualTo(marca);
        assertThat(mesmoModelo.get("marca").asText()).isEqualTo("Renault");
        JsonNode mesmoVeiculo = resposta(get("/api/veiculos/" + veiculo), 200);
        assertThat(mesmoVeiculo.get("modeloId").asLong()).isEqualTo(modelo);
        assertThat(mesmoVeiculo.get("marca").asText()).isEqualTo("Renault");
        resposta(corpo(post("/api/marcas"), "{\"nome\":\"RENAULT\"}"), 409);
    }

    @Test
    void corrigeModeloComVeiculosETrocaSuaMarcaAtomicamente() throws Exception {
        long antiga = marca("Fabricante antigo");
        long correta = marca("Fabricante correto");
        long modelo = modelo(antiga, "Sedam");
        long primeiro = veiculo(modelo, 50000);
        long segundo = veiculo(modelo, 61000);
        String url = "/api/modelos/" + modelo;
        modelo(correta, "Ocupado");
        JsonNode antes = resposta(get(url), 200);
        resposta(editar(url, correta, "  OCUPADO  "), 409);
        resposta(editar(url, 999999, "Sedan"), 404);
        resposta(editar(url, correta, "   "), 400);
        resposta(corpo(put(url), "{\"nome\":\"Sedan\"}"), 400);
        resposta(editar("/api/modelos/999999", correta, "Sedan"), 404);
        assertThat(resposta(get(url), 200)).isEqualTo(antes);
        assertThat(resposta(get("/api/veiculos/" + primeiro), 200).get("marcaId").asLong()).isEqualTo(antiga);

        JsonNode atualizado = resposta(editar(url, correta, "  Sedan  "), 200);
        assertThat(atualizado.get("id").asLong()).isEqualTo(modelo);
        assertThat(atualizado.get("nome").asText()).isEqualTo("Sedan");
        assertThat(atualizado.get("marcaId").asLong()).isEqualTo(correta);
        assertThat(atualizado.get("marca").asText()).isEqualTo("Fabricante correto");
        assertThat(resposta(editar(url, correta, "Sedan"), 200)).isEqualTo(atualizado);
        assertThat(resposta(get(url), 200)).isEqualTo(atualizado);
        for (long id : new long[]{primeiro, segundo}) {
            JsonNode unidade = resposta(get("/api/veiculos/" + id), 200);
            assertThat(unidade.get("id").asLong()).isEqualTo(id);
            assertThat(unidade.get("modeloId").asLong()).isEqualTo(modelo);
            assertThat(unidade.get("modelo").asText()).isEqualTo("Sedan");
            assertThat(unidade.get("marcaId").asLong()).isEqualTo(correta);
            assertThat(unidade.get("marca").asText()).isEqualTo("Fabricante correto");
        }
        assertThat(resposta(get("/api/veiculos").param("marcaId", String.valueOf(antiga)), 200).size()).isZero();
        assertThat(resposta(get("/api/veiculos").param("marcaId", String.valueOf(correta)), 200).size()).isEqualTo(2);
        // O mesmo nome é permitido em outra marca.
        modelo(antiga, "Sedan");
    }

    @Test
    void excluiModeloLivreEBloqueiaModeloVinculadoEmCadaStatus() throws Exception {
        long marca = marca("Fabricante de outra linha");
        long modelo = modelo(marca, "Modelo de estoque");
        String url = "/api/modelos/" + modelo;
        long id = veiculo(modelo, 54000);
        String urlVeiculo = "/api/veiculos/" + id;
        for (String situacao : new String[]{"DISPONIVEL", "VENDIDO", "DESCONTINUADO"}) {
            resposta(corpo(patch(urlVeiculo), json.createObjectNode().put("status", situacao).toString()), 200);
            JsonNode conflito = resposta(delete(url), 409);
            assertThat(conflito.get("mensagem").asText()).contains("veículos vinculados");
            assertThat(resposta(get(urlVeiculo), 200).get("modeloId").asLong()).isEqualTo(modelo);
        }
        mvc.perform(delete(urlVeiculo)).andExpect(status().isNoContent());
        mvc.perform(delete(url)).andExpect(status().isNoContent()).andExpect(content().string(""));
        resposta(get(url), 404);
        resposta(delete(url), 404);
        assertThat(resposta(get("/api/marcas/" + marca), 200).get("id").asLong()).isEqualTo(marca);
    }

    private MockHttpServletRequestBuilder editar(String url, long marcaId, String nome) {
        return corpo(put(url), json.createObjectNode().put("marcaId", marcaId).put("nome", nome).toString());
    }

    private long marca(String nome) throws Exception {
        return resposta(corpo(post("/api/marcas"), json.createObjectNode()
                .put("nome", nome).toString()), 201).get("id").asLong();
    }

    private long modelo(long marcaId, String nome) throws Exception {
        return resposta(corpo(post("/api/modelos"), json.createObjectNode()
                .put("nome", nome).put("marcaId", marcaId).toString()), 201).get("id").asLong();
    }

    private long veiculo(long modeloId, int preco) throws Exception {
        return resposta(corpo(post("/api/veiculos"), json.createObjectNode()
                .put("modeloId", modeloId).put("ano", 2022).put("cor", "Branco").put("preco", preco)
                .put("quilometragem", 35000).put("tipo", "CARRO").toString()), 201).get("id").asLong();
    }

    private MockHttpServletRequestBuilder corpo(MockHttpServletRequestBuilder requisicao, String conteudo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(conteudo);
    }

    private JsonNode resposta(MockHttpServletRequestBuilder requisicao, int codigo) throws Exception {
        byte[] resultado = mvc.perform(requisicao).andExpect(status().is(codigo)).andReturn()
                .getResponse().getContentAsByteArray();
        return json.readTree(resultado);
    }
}
