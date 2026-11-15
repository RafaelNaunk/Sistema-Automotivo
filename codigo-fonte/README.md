# Sistema Automotivo · Gestão de Estoque de Veículos

Aplicação acadêmica para cadastrar marcas, modelos e unidades do estoque de uma concessionária. O vendedor pesquisa os veículos por marca, modelo, tipo, ano de fabricação, disponibilidade e faixa de preços. Pode editar o preço, registrar uma nova quilometragem e retirar do estoque um veículo vendido ou descontinuado.

## Corrigir uma marca pelo sistema

Em **Marcas e modelos**, localize o cadastro e escolha **Renomear** para corrigir a grafia. Os modelos e veículos associados passam a mostrar o novo nome. A aplicação mantém o identificador e impede usar o nome de outra marca. Para apagar um cadastro feito por engano, escolha **Excluir** junto ao nome e confira a confirmação. A operação exige que a marca tenha zero modelos associados, mesmo que os modelos estejam sem veículos.

## Editar ou excluir um modelo

Abra **Marcas e modelos → Modelos cadastrados**. **Editar** permite corrigir o nome, a marca, ou os dois campos. A alteração vale para todas as unidades associadas ao modelo, mantendo seus identificadores, preços e quilometragens. Nomes de modelo precisam ser únicos dentro de cada marca. Para remover um cadastro sem unidades associadas, escolha **Excluir modelo**, confira a marca e o modelo e confirme. Qualquer veículo vinculado impede a exclusão, inclusive um vendido que ainda consta do estoque.

## Abrir a aplicação

Use Java 17 ou superior. O projeto foi compilado com JDK 21. Se você recebeu o arquivo `sistema-automotivo-1.2.0.jar`, abra um terminal na pasta do arquivo e execute:

```powershell
java -jar sistema-automotivo-1.2.0.jar
```

Quando o terminal exibir `Started SistemaAutomotivo`, abra **http://127.0.0.1:8080** no navegador. O botão de cadastrar veículo passa a ter modelos disponíveis depois de cadastrar pelo menos uma marca e um modelo. Para encerrar o serviço, pressione `Ctrl+C` no terminal.

A execução padrão usa o perfil **demonstracao** e cria o banco H2 na pasta `dados` do diretório de execução. Os registros persistem ao fechar a aplicação. Reinicie no mesmo diretório para reencontrá-los. O perfil MySQL está descrito abaixo.

## Abrir o código no VS Code

1. Instale o JDK 21 e Maven 3.6.3 ou superior. No terminal, confira as instalações com `java -version`, `javac -version` e `mvn -version`.
2. No VS Code, instale a extensão **Extension Pack for Java**, da Microsoft. Use **Arquivo → Abrir Pasta** e selecione a pasta que contém este `pom.xml`.
3. Abra **Terminal → Novo Terminal** nessa pasta e execute `mvn clean package`. A primeira compilação baixa dependências do Maven Central. O Maven compila o código, executa os testes de integração e cria o JAR com as dependências.
4. Execute o arquivo gerado:

```powershell
java -jar target/sistema-automotivo-1.2.0.jar
```

Para desenvolver, também é possível iniciar com `mvn spring-boot:run`. Para executar somente os testes, use `mvn test`. Os testes usam uma base H2 em memória criada para a suíte. As operações realizadas na interface usam o banco do perfil escolhido.

Guias oficiais: [Java no VS Code](https://code.visualstudio.com/docs/java/java-tutorial) · [Instalação do Maven](https://maven.apache.org/install.html).

## Usar MySQL 8.4

1. Inicie o servidor MySQL e abra o Workbench como administrador. Crie o banco executando o SQL abaixo.

```sql
CREATE DATABASE IF NOT EXISTS estoque_veiculos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. No Workbench, em **Administration → Users and Privileges → Add Account**, crie o usuário `estoque_app` com host `localhost`. Digite uma senha sua nos campos de senha e aplique a mudança. Mantenha a conta restrita à máquina local.
3. Como administrador, autorize essa conta a criar e usar as tabelas do projeto:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX, REFERENCES
ON estoque_veiculos.* TO 'estoque_app'@'localhost';
```

4. No PowerShell da pasta do projeto, preencha as variáveis da sessão. A senha será solicitada de forma oculta. A conexão abaixo utiliza a porta padrão do MySQL.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/estoque_veiculos'
$env:DB_USER = 'estoque_app'
$credencial = Get-Credential -UserName $env:DB_USER -Message 'Digite a senha do usuário do estoque no MySQL'
$env:DB_PASSWORD = $credencial.GetNetworkCredential().Password
java -jar target/sistema-automotivo-1.2.0.jar --spring.profiles.active=mysql
```

Se recebeu o JAR avulso, rode o último comando a partir da pasta dele, removendo o prefixo `target/`. O Spring executa `src/main/resources/schema.sql` no banco indicado. A página abre no mesmo endereço do perfil anterior. Para limpar a senha da sessão depois de encerrar o servidor, execute:

```powershell
Remove-Item Env:DB_PASSWORD
Remove-Variable credencial
```

## Fazer o primeiro cadastro e testar o CRUD

| Passo | Ação | O que observar |
| --- | --- | --- |
| 1 | Cadastre a marca Toyota e o modelo Corolla nessa marca. | O modelo aparece associado à Toyota. |
| 2 | Cadastre um carro Corolla, ano 2022, branco, R$ 85.000,00, 35.000 km, disponível. Depois cadastre um segundo Corolla por R$ 99.000,00. | Os dois recebem identificadores diferentes. |
| 3 | Filtre por Toyota, Corolla, ano 2022, disponível e preço máximo de R$ 85.000,00. | Apenas o primeiro aparece. O valor no limite da faixa é incluído. |
| 4 | Edite o primeiro para R$ 84.500,00 e 36.000 km. | Os novos dados aparecem na consulta. O segundo carro preserva o preço anterior. |
| 5 | Tente reduzir a quilometragem e tente remover o veículo disponível. | A alteração inválida e a exclusão fora da regra são impedidas. |
| 6 | Marque o primeiro como vendido. Limpe o filtro de disponibilidade, encontre o veículo e remova-o. | Ele desaparece depois da confirmação. O segundo carro continua no estoque. |

Para comprovar persistência, encerre e reinicie o servidor com o mesmo perfil e a mesma base. Consulte o segundo veículo.

## Contrato da API

Todas as rotas usam o prefixo `/api`. As listas retornam arrays JSON. Na criação a API responde com HTTP 201 e o cabeçalho `Location` do recurso. Erros de entrada retornam 400; recursos ausentes, 404; conflitos com as regras do catálogo ou de remoção, 409. O campo `mensagem` explica a falha.

| Método e rota | Dados ou resultado |
| --- | --- |
| `POST /api/marcas` | Corpo com `nome`. |
| `GET /api/marcas`, `GET /api/marcas/{id}` | Lista ordenada ou uma marca. |
| `PATCH /api/marcas/{id}` | Envie `{"nome":"Renault"}` para renomear. Retorna 200 e mantém os vínculos. |
| `DELETE /api/marcas/{id}` | Remove uma marca sem modelos (204). Com modelo associado, retorna 409. |
| `POST /api/modelos` | Corpo com `nome` e `marcaId`. |
| `GET /api/modelos`, `GET /api/modelos/{id}` | Lista ou modelo. A lista aceita `marcaId` opcional. |
| `PUT /api/modelos/{id}` | Corpo completo com `nome` e `marcaId`. Retorna 200, com o mesmo ID do modelo. |
| `DELETE /api/modelos/{id}` | Retorna 204 se não houver veículos associados, 409 se estiver em uso e 404 se já não existir. |
| `POST /api/veiculos` | Corpo com `modeloId`, `ano`, `cor`, `preco`, `quilometragem`, `tipo` e `status`. O status inicial padrão é `DISPONIVEL`. |
| `GET /api/veiculos`, `GET /api/veiculos/{id}` | Lista completa, lista filtrada ou uma unidade. |
| `PATCH /api/veiculos/{id}` | Envie um ou mais entre `preco`, `quilometragem` e `status`. Cada valor omitido é preservado. |
| `DELETE /api/veiculos/{id}` | Remove a unidade se estiver vendida ou descontinuada. Retorna 204 sem corpo. |

Filtros cumulativos: `marcaId`, `modeloId`, `tipo`, `ano`, `status`, `precoMin` e `precoMax`. Por exemplo, `/api/veiculos?ano=2022&status=DISPONIVEL&precoMax=85000`. Uma consulta sem filtros inclui todos os estados. Uma busca válida sem correspondência devolve `[]`.

Status permitidos: `DISPONIVEL`, `VENDIDO`, `DESCONTINUADO`. Tipos permitidos: `CARRO`, `MOTO`, `CAMINHAO`, `UTILITARIO`. No JSON, o preço é numérico, usa ponto decimal, aceita até duas casas e deve ser maior que zero. A quilometragem usa inteiros e pode ser zero. O ano de fabricação deve estar entre 1886 e o ano atual.