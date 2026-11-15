package br.edu.estoque.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;

public final class Veiculo {
    private final long modeloId;
    private final int ano;
    private final String cor;
    private final TipoVeiculo tipo;
    private BigDecimal preco;
    private int quilometragem;
    private StatusVeiculo status;

    public Veiculo(Long modeloId, Integer ano, String cor, TipoVeiculo tipo, BigDecimal preco,
                   Integer quilometragem, StatusVeiculo status) {
        if (modeloId == null || modeloId < 1) {
            throw new IllegalArgumentException("Selecione um modelo válido.");
        }
        validarAno(ano);
        if (tipo == null) {
            throw new IllegalArgumentException("Informe o tipo de veículo.");
        }
        this.modeloId = modeloId;
        this.ano = ano;
        this.cor = ItemCatalogo.validarTexto(cor, "Cor");
        this.tipo = tipo;
        this.preco = validarPreco(preco);
        this.quilometragem = validarQuilometragem(quilometragem);
        this.status = status == null ? StatusVeiculo.DISPONIVEL : status;
    }

    public static void validarAno(Integer ano) {
        if (ano == null || ano < 1886 || ano > Year.now().getValue()) {
            throw new IllegalArgumentException("O ano deve estar entre 1886 e o ano atual.");
        }
    }

    private static BigDecimal validarPreco(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.compareTo(new BigDecimal("9999999999.99")) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Informe um preço positivo, até 9.999.999.999,99, com até duas casas decimais.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static int validarQuilometragem(Integer km) {
        if (km == null || km < 0) {
            throw new IllegalArgumentException("A quilometragem deve ser um inteiro maior ou igual a zero.");
        }
        return km;
    }

    public void atualizar(BigDecimal novoPreco, Integer novaKm, StatusVeiculo novoStatus) {
        if (novoPreco == null && novaKm == null && novoStatus == null) {
            throw new IllegalArgumentException("Informe preço, quilometragem ou status para atualizar.");
        }
        BigDecimal precoValidado = novoPreco == null ? preco : validarPreco(novoPreco);
        int kmValidada = novaKm == null ? quilometragem : validarQuilometragem(novaKm);
        if (kmValidada < quilometragem) {
            throw new IllegalArgumentException("A quilometragem não pode diminuir.");
        }
        // Atribuir somente após validar todos os campos evita uma atualização parcial.
        preco = precoValidado;
        quilometragem = kmValidada;
        if (novoStatus != null) {
            status = novoStatus;
        }
    }

    public boolean podeRemover() {
        return status == StatusVeiculo.VENDIDO || status == StatusVeiculo.DESCONTINUADO;
    }

    public long getModeloId() { return modeloId; }
    public int getAno() { return ano; }
    public String getCor() { return cor; }
    public TipoVeiculo getTipo() { return tipo; }
    public BigDecimal getPreco() { return preco; }
    public int getQuilometragem() { return quilometragem; }
    public StatusVeiculo getStatus() { return status; }
}
