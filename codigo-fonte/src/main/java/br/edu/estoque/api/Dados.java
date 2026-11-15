package br.edu.estoque.api;

import br.edu.estoque.dominio.StatusVeiculo;
import br.edu.estoque.dominio.TipoVeiculo;
import br.edu.estoque.dominio.Veiculo;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.math.BigDecimal;

public final class Dados {
    private Dados() {}

    public record NovaMarca(String nome) {}
    public record NovoModelo(Long marcaId, String nome) {}
    public record NovoVeiculo(Long modeloId, Integer ano, String cor, BigDecimal preco,
                              Integer quilometragem, TipoVeiculo tipo, StatusVeiculo status) {}

    // No PATCH, um campo omitido preserva o valor. Um campo enviado com null é inválido.
    public static final class Atualizacao {
        @JsonSetter(nulls = Nulls.FAIL) public BigDecimal preco;
        @JsonSetter(nulls = Nulls.FAIL) public Integer quilometragem;
        @JsonSetter(nulls = Nulls.FAIL) public StatusVeiculo status;
    }

    public record MarcaDto(long id, String nome) {}
    public record ModeloDto(long id, String nome, long marcaId, String marca) {}
    public record VeiculoDto(long id, long modeloId, String modelo, long marcaId, String marca,
                             int ano, String cor, BigDecimal preco, int quilometragem,
                             TipoVeiculo tipo, StatusVeiculo status) {}

    public record Filtro(Long marcaId, Long modeloId, BigDecimal precoMin, BigDecimal precoMax,
                         Integer ano, StatusVeiculo status, TipoVeiculo tipo) {
        public Filtro {
            if ((marcaId != null && marcaId < 1) || (modeloId != null && modeloId < 1)) {
                throw new IllegalArgumentException("Os identificadores de marca e modelo devem ser positivos.");
            }
            if ((precoMin != null && precoMin.signum() < 0) || (precoMax != null && precoMax.signum() < 0)) {
                throw new IllegalArgumentException("Os limites de preço devem ser maiores ou iguais a zero.");
            }
            if (precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0) {
                throw new IllegalArgumentException("O preço mínimo deve ser menor ou igual ao preço máximo.");
            }
            if (ano != null) {
                Veiculo.validarAno(ano);
            }
        }
    }
}
