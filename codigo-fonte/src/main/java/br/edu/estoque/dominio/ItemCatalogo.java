package br.edu.estoque.dominio;

import java.util.Locale;

public abstract class ItemCatalogo {
    private final String nome;

    protected ItemCatalogo(String nome) {
        this.nome = validarTexto(nome, "Nome");
    }

    public String getNome() {
        return nome;
    }

    // Uma chave normalizada mantém o nome de exibição e impede duplicidades de caixa.
    public String chaveDoNome() {
        return nome.toLowerCase(Locale.ROOT);
    }

    static String validarTexto(String texto, String campo) {
        if (texto == null || texto.isBlank() || texto.strip().length() > 80) {
            throw new IllegalArgumentException(campo + " deve ter entre 1 e 80 caracteres.");
        }
        return texto.strip();
    }
}
