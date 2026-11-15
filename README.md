# Sistema Automotivo · Gestão de Estoque de Veículos

Aplicação acadêmica para cadastrar marcas, modelos e unidades do estoque de uma concessionária. O vendedor pesquisa os veículos por marca, modelo, tipo, ano de fabricação, disponibilidade e faixa de preços, edita preço e quilometragem e retira do estoque um veículo vendido ou descontinuado.

Feita em Java 17 com Spring Boot 3.5 (API REST e acesso ao banco com JDBC), interface web em HTML, CSS e JavaScript, e banco H2 ou MySQL.

## Conteúdo do repositório

| Pasta | O que tem |
| --- | --- |
| [`codigo-fonte`](codigo-fonte) | Projeto Maven com o código, os testes e a [documentação completa](codigo-fonte/README.md): como rodar, como usar o MySQL e o contrato da API. |
| [`entrega`](entrega) | [Relatório teórico](entrega/01-relatorio-teorico.pdf) e [relatório prático](entrega/02-relatorio-pratico.pdf). |

## Rodar sem instalar Java (Windows)

Baixe o arquivo **Sistema-Automotivo-1.2.0.zip** na [versão mais recente](https://github.com/RafaelNaunk/Sistema-Automotivo/releases/latest) e extraia o conteúdo. Na pasta `Sistema-Automotivo`, abra o `Iniciar-Estoque.cmd`, que usa o Java embutido no pacote. Quando aparecer `Started SistemaAutomotivo`, abra http://127.0.0.1:8080 no navegador.

## Rodar a partir do código

Com JDK 17 ou superior e Maven 3.6.3 ou superior instalados:

```powershell
cd codigo-fonte
mvn spring-boot:run
```

Quando aparecer `Started SistemaAutomotivo`, abra http://127.0.0.1:8080 no navegador. Por padrão a aplicação usa o banco H2, criado na pasta `dados`.
