package br.edu.estoque.dominio;

public final class Modelo extends ItemCatalogo {
    private final long marcaId;

    public Modelo(String nome, Long marcaId) {
        super(nome);
        if (marcaId == null || marcaId < 1) {
            throw new IllegalArgumentException("Selecione uma marca válida.");
        }
        this.marcaId = marcaId;
    }

    public long getMarcaId() {
        return marcaId;
    }
}
