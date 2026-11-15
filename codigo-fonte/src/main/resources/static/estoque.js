"use strict";

const porId = (id) => document.getElementById(id);
const tipos = { CARRO: "Carro", MOTO: "Moto", CAMINHAO: "Caminhão", UTILITARIO: "Utilitário" };
const estados = { DISPONIVEL: "Disponível", VENDIDO: "Vendido", DESCONTINUADO: "Descontinuado" };
const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const inteiro = new Intl.NumberFormat("pt-BR");
let marcas = [];
let modelos = [];
let veiculos = [];
let consulta = "";
let numeroConsulta = 0;
let idEmEdicao;
let idEmExclusao;
let marcaEmExclusao;
let marcaEmEdicao;
let exclusaoMarcaPermitida = false;
let modeloSelecionado;
let verificacaoDeVinculos = 0;
let exclusaoModeloPermitida = false;

async function api(caminho, metodo = "GET", dados) {
  const opcoes = { method: metodo };
  if (dados !== undefined) {
    opcoes.headers = { "Content-Type": "application/json" };
    opcoes.body = JSON.stringify(dados);
  }
  let resposta;
  try {
    resposta = await fetch("/api/" + caminho, opcoes);
  } catch {
    throw new Error("Não foi possível conectar ao sistema. Verifique a conexão e tente novamente.");
  }
  if (resposta.status === 204) return;
  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    const erro = new Error(corpo?.mensagem || "Não foi possível concluir a operação (HTTP " + resposta.status + ").");
    erro.status = resposta.status;
    throw erro;
  }
  return corpo;
}

function aviso(texto, erro = false) {
  const area = porId("aviso");
  area.dataset.erro = erro ? "sim" : "nao";
  area.textContent = texto;
}

function mostrarTela(catalogo, limparAviso = true) {
  porId("tela-estoque").hidden = catalogo;
  porId("tela-catalogo").hidden = !catalogo;
  const atual = porId(catalogo ? "nav-catalogo" : "nav-estoque");
  const anterior = porId(catalogo ? "nav-estoque" : "nav-catalogo");
  atual.setAttribute("aria-current", "page");
  anterior.removeAttribute("aria-current");
  document.title = (catalogo ? "Marcas e modelos" : "Estoque de veículos") + " · Sistema Automotivo";
  if (limparAviso) aviso("");
}

function opcoes(id, itens, vazio) {
  const seletor = porId(id);
  const anterior = seletor.value;
  seletor.replaceChildren();
  if (vazio !== null) seletor.add(new Option(vazio, ""));
  for (const [valor, rotulo] of itens) seletor.add(new Option(rotulo, String(valor)));
  if (Array.from(seletor.options).some((opcao) => opcao.value === anterior)) seletor.value = anterior;
}

function opcoesModelos(marcaCampo, modeloCampo, filtro) {
  const marcaId = porId(marcaCampo).value;
  const disponiveis = modelos.filter((m) => marcaId ? String(m.marcaId) === marcaId : filtro);
  opcoes(modeloCampo, disponiveis.map((m) => [m.id, filtro && !marcaId ? m.marca + " · " + m.nome : m.nome]),
    filtro ? "Todos os modelos" : "Selecione o modelo");
}

function mostrarItensDoCatalogo(lista, itens, vazio, acoes, rotulo) {
  lista.replaceChildren();
  if (itens.length === 0) lista.append(elemento("li", vazio));
  for (const item of itens) {
    const linha = elemento("li", "", "item-catalogo");
    linha.dataset.id = item.id;
    const botoes = elemento("div", "", "acoes");
    const descricao = rotulo(item);
    for (const [acao, nome, nomeAcessivel] of acoes) {
      const botao = elemento("button", nome, acao.startsWith("excluir") ? "acao excluir" : "acao");
      botao.type = "button";
      botao.dataset.acao = acao;
      botao.setAttribute("aria-label", nomeAcessivel + " " + descricao);
      botoes.append(botao);
    }
    linha.append(elemento("span", descricao), botoes);
    lista.append(linha);
  }
}

async function carregarCatalogo() {
  [marcas, modelos] = await Promise.all([api("marcas"), api("modelos")]);
  mostrarCatalogo();
}

function mostrarCatalogo() {
  const nomes = marcas.map((m) => [m.id, m.nome]);
  opcoes("f-marca", nomes, "Todas as marcas");
  opcoes("modelo-marca", nomes, "Selecione a marca");
  opcoes("cad-marca", nomes, "Selecione a marca");
  opcoesModelos("f-marca", "f-modelo", true);
  opcoesModelos("cad-marca", "cad-modelo", false);
  mostrarItensDoCatalogo(porId("lista-marcas"), marcas, "Nenhuma marca cadastrada.",
    [["renomear-marca", "Renomear", "Renomear marca"], ["excluir-marca", "Excluir marca", "Excluir marca"]], (m) => m.nome);
  mostrarItensDoCatalogo(porId("lista-modelos"), modelos, "Nenhum modelo cadastrado.",
    [["editar-modelo", "Editar", "Editar modelo"], ["excluir-modelo", "Excluir modelo", "Excluir modelo"]], (m) => m.marca + " · " + m.nome);
  porId("salvar-modelo").disabled = marcas.length === 0;
}

function elemento(tag, texto, classe) {
  const el = document.createElement(tag);
  el.textContent = texto;
  if (classe) el.className = classe;
  return el;
}

function identificar(v) {
  return v.marca + " " + v.modelo + " · #" + v.id;
}

function mostrarVeiculos() {
  const corpo = porId("linhas-veiculos");
  corpo.replaceChildren();
  for (const v of veiculos) {
    const linha = corpo.insertRow();
    linha.dataset.id = v.id;
    linha.insertCell().append(elemento("strong", v.marca + " " + v.modelo),
      elemento("span", "#" + v.id + " · " + tipos[v.tipo], "secao-dado"));
    linha.insertCell().append(elemento("span", v.ano), elemento("span", v.cor, "secao-dado"));
    linha.insertCell().textContent = inteiro.format(v.quilometragem) + " km";
    linha.insertCell().textContent = moeda.format(v.preco);
    linha.insertCell().append(elemento("span", estados[v.status], "status " + v.status.toLowerCase()));
    const acoes = elemento("div", "", "acoes");
    for (const [acao, nome] of [["editar", "Editar"], ["excluir", "Excluir"]]) {
      const botao = elemento("button", nome, "acao " + acao);
      botao.type = "button";
      botao.dataset.acao = acao;
      botao.setAttribute("aria-label", nome + " " + identificar(v));
      if (acao === "excluir" && v.status === "DISPONIVEL") {
        botao.disabled = true;
        botao.title = "A exclusão é permitida para veículos vendidos ou descontinuados.";
      }
      acoes.append(botao);
    }
    linha.insertCell().append(acoes);
  }
  porId("resultado-contagem").textContent = veiculos.length === 1 ? "1 resultado" : veiculos.length + " resultados";
  porId("vazio").hidden = veiculos.length > 0;
  porId("vazio").textContent = consulta ? "Nenhum veículo corresponde aos filtros selecionados." : "Nenhum veículo cadastrado.";
}

async function carregarVeiculos() {
  const pedido = ++numeroConsulta;
  try {
    const lista = await api("veiculos" + (consulta ? "?" + consulta : ""));
    if (pedido !== numeroConsulta) return;
    veiculos = lista;
    mostrarVeiculos();
  } catch (erro) {
    if (pedido !== numeroConsulta) return;
    porId("linhas-veiculos").replaceChildren();
    porId("resultado-contagem").textContent = "";
    porId("vazio").hidden = false;
    porId("vazio").textContent = "Não foi possível consultar o estoque.";
    throw erro;
  }
}

function abrirDialogo(id) {
  const dialogo = porId(id);
  const alerta = dialogo.querySelector(".erro-local");
  alerta.textContent = "";
  alerta.hidden = true;
  dialogo.showModal();
}

function abrirCadastro() {
  porId("form-veiculo").reset();
  opcoesModelos("cad-marca", "cad-modelo", false);
  porId("ajuda-catalogo").hidden = modelos.length > 0;
  porId("salvar-veiculo").disabled = modelos.length === 0;
  abrirDialogo("dialogo-cadastro");
}

function escolherAcao(evento) {
  const botao = evento.target.closest("button[data-acao]");
  if (!botao) return;
  const id = Number(botao.closest("tr").dataset.id);
  const v = veiculos.find((item) => item.id === id);
  if (!v) return;
  if (botao.dataset.acao === "editar") {
    idEmEdicao = id;
    porId("descricao-edicao").textContent = identificar(v);
    porId("ed-preco").value = v.preco;
    porId("ed-km").min = v.quilometragem;
    porId("ed-km").value = v.quilometragem;
    porId("ed-status").value = v.status;
    abrirDialogo("dialogo-edicao");
  } else {
    idEmExclusao = id;
    porId("descricao-exclusao").textContent = identificar(v) + " · " + estados[v.status];
    abrirDialogo("dialogo-exclusao");
  }
}

function descreverVinculos(marcaId) {
  const associados = modelos.filter((modelo) => modelo.marcaId === marcaId);
  const quantidade = associados.length === 1 ? "1 modelo vinculado" : associados.length + " modelos vinculados";
  porId("vinculos-exclusao-marca").textContent = associados.length
    ? quantidade + ". A marca precisa permanecer no catálogo. Para corrigir o nome, use Renomear."
    : "Nenhum modelo vinculado. A confirmação remove a marca do catálogo permanentemente.";
  exclusaoMarcaPermitida = associados.length === 0;
  porId("confirmar-exclusao-marca").disabled = !exclusaoMarcaPermitida;
}

porId("lista-marcas").addEventListener("click", (evento) => {
  const botao = evento.target.closest("button[data-acao]");
  if (!botao) return;
  const id = Number(botao.closest("li").dataset.id);
  const marca = marcas.find((item) => item.id === id);
  if (!marca) return;
  if (botao.dataset.acao === "renomear-marca") {
    marcaEmEdicao = marca;
    porId("descricao-renomear-marca").textContent = "Nome atual: " + marca.nome + ". Os modelos e veículos vinculados passam a exibir o novo nome.";
    porId("novo-nome-marca").value = marca.nome;
    abrirDialogo("dialogo-renomear-marca");
    porId("novo-nome-marca").focus();
    porId("novo-nome-marca").select();
    return;
  }
  marcaEmExclusao = marca;
  porId("descricao-exclusao-marca").textContent = marcaEmExclusao.nome + " · #" + id;
  descreverVinculos(id);
  abrirDialogo("dialogo-exclusao-marca");
  porId("cancelar-exclusao-marca").focus();
});

porId("dialogo-exclusao-marca").addEventListener("cancel", (evento) => {
  if (porId("cancelar-exclusao-marca").disabled) evento.preventDefault();
});

porId("dialogo-exclusao-marca").addEventListener("close", () => {
  // A lista também pode ter sido reconstruída após um conflito.
  const origem = porId("lista-marcas").querySelector('li[data-id="' + marcaEmExclusao.id + '"] [data-acao=excluir-marca]');
  (origem || porId("marca-nome")).focus();
});
porId("dialogo-renomear-marca").addEventListener("cancel", (evento) => {
  if (porId("salvar-nome-marca").disabled) evento.preventDefault();
});
porId("dialogo-renomear-marca").addEventListener("close", () => {
  const origem = porId("lista-marcas").querySelector('li[data-id="' + marcaEmEdicao.id + '"] [data-acao=renomear-marca]');
  (origem || porId("marca-nome")).focus();
});

async function verificarVeiculosDoModelo(modelo) {
  const pedido = ++verificacaoDeVinculos;
  const texto = porId("vinculos-excluir-modelo");
  const dialogo = porId("dialogo-excluir-modelo");
  const confirmar = porId("confirmar-exclusao-modelo");
  exclusaoModeloPermitida = false;
  confirmar.disabled = true;
  texto.textContent = "Consultando veículos vinculados ao modelo…";
  try {
    // Consulta independente dos filtros que o usuário aplicou à tabela de estoque.
    const vinculados = await api("veiculos?modeloId=" + modelo.id);
    if (pedido !== verificacaoDeVinculos || !dialogo.open) return;
    if (!Array.isArray(vinculados)) throw new Error("Resposta inesperada ao consultar os veículos.");
    exclusaoModeloPermitida = vinculados.length === 0;
    const quantidade = vinculados.length === 1 ? "1 veículo vinculado" : vinculados.length + " veículos vinculados";
    texto.textContent = exclusaoModeloPermitida
      ? "Nenhum veículo vinculado. A confirmação remove o modelo do catálogo permanentemente."
      : quantidade + ". O modelo em uso pode ser editado.";
    confirmar.disabled = !exclusaoModeloPermitida;
  } catch (erro) {
    if (pedido !== verificacaoDeVinculos || !dialogo.open) return;
    texto.textContent = "Não foi possível verificar os vínculos. Feche e reabra para tentar de novo.";
    const alerta = porId("erro-excluir-modelo");
    alerta.textContent = erro.message;
    alerta.hidden = false;
  }
}

porId("lista-modelos").addEventListener("click", (evento) => {
  const botao = evento.target.closest("button[data-acao]");
  if (!botao) return;
  const id = Number(botao.closest("li").dataset.id);
  modeloSelecionado = modelos.find((m) => m.id === id);
  if (!modeloSelecionado) return;
  const identificacao = modeloSelecionado.marca + " · " + modeloSelecionado.nome + " · #" + id;
  if (botao.dataset.acao === "editar-modelo") {
    porId("descricao-editar-modelo").textContent = identificacao + ". A mudança de marca ou nome aparece em todos os veículos associados.";
    opcoes("editar-modelo-marca", marcas.map((m) => [m.id, m.nome]), "Selecione a marca");
    porId("editar-modelo-marca").value = modeloSelecionado.marcaId;
    porId("editar-modelo-nome").value = modeloSelecionado.nome;
    abrirDialogo("dialogo-editar-modelo");
    porId("editar-modelo-nome").focus();
    porId("editar-modelo-nome").select();
    return;
  }
  porId("descricao-excluir-modelo").textContent = identificacao;
  abrirDialogo("dialogo-excluir-modelo");
  porId("cancelar-exclusao-modelo").focus();
  verificarVeiculosDoModelo(modeloSelecionado);
});

for (const acao of ["editar-modelo", "excluir-modelo"]) {
  const dialogo = porId("dialogo-" + acao);
  dialogo.addEventListener("cancel", (evento) => {
    if (dialogo.querySelector(".cancelar").disabled) evento.preventDefault();
  });
  dialogo.addEventListener("close", () => {
    const origem = porId("lista-modelos").querySelector('li[data-id="' + modeloSelecionado.id + '"] [data-acao="' + acao + '"]');
    (origem || porId("modelo-nome")).focus();
  });
}

async function salvarModelo(itens) {
  const anterior = modeloSelecionado;
  const atual = await api("modelos/" + anterior.id, "PUT", itens);
  const filtros = new URLSearchParams(consulta);
  if (filtros.get("modeloId") === String(atual.id) && filtros.get("marcaId") === String(anterior.marcaId)) {
    // Quem escolheu o modelo e a marca juntos continua vendo a mesma unidade após a correção.
    filtros.set("marcaId", String(atual.marcaId));
    consulta = filtros.toString();
  }
  for (const [campoMarca, campoModelo] of [["f-marca", "f-modelo"], ["cad-marca", "cad-modelo"]]) {
    if (porId(campoModelo).value === String(atual.id) && porId(campoMarca).value === String(anterior.marcaId)) {
      porId(campoMarca).value = atual.marcaId;
    }
  }
  modelos = modelos.map((m) => m.id === atual.id ? atual : m);
  mostrarCatalogo();
  await concluirCatalogo("Modelo atualizado para " + atual.marca + " · " + atual.nome + ".", true);
}

async function excluirModelo() {
  const modelo = modeloSelecionado;
  let mensagem = "Modelo " + modelo.marca + " · " + modelo.nome + " excluído do catálogo.";
  try {
    await api("modelos/" + modelo.id, "DELETE");
  } catch (erro) {
    if (erro.status === 409) {
      exclusaoModeloPermitida = false;
      porId("vinculos-excluir-modelo").textContent = "O servidor encontrou um veículo vinculado. O modelo em uso pode ser editado.";
      throw erro;
    }
    if (erro.status !== 404) throw erro;
    mensagem = "O modelo " + modelo.nome + " já havia sido excluído do catálogo.";
  }
  const filtros = new URLSearchParams(consulta);
  if (filtros.get("modeloId") === String(modelo.id)) filtros.delete("modeloId");
  consulta = filtros.toString();
  modelos = modelos.filter((m) => m.id !== modelo.id);
  mostrarCatalogo();
  await concluirCatalogo(mensagem, true);
}

async function concluirDialogoModelo(id, operacao) {
  const dialogo = porId(id);
  const saidas = dialogo.querySelectorAll(".cancelar,.fechar");
  for (const botao of saidas) botao.disabled = true;
  try {
    await operacao();
    dialogo.close();
  } finally {
    for (const botao of saidas) botao.disabled = false;
    if (dialogo.open && id === "dialogo-excluir-modelo") porId("cancelar-exclusao-modelo").focus();
  }
}

// Mantém o formulário aberto com o erro da API e impede envios repetidos.
function aoEnviar(id, executar, podeEnviar = () => true) {
  const formulario = porId(id);
  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const botao = evento.submitter || formulario.querySelector("button:not([type=button])");
    if (botao.disabled) return;
    botao.disabled = true;
    const alerta = formulario.querySelector(".erro-local");
    if (alerta) alerta.hidden = true;
    try {
      await executar();
    } catch (erro) {
      if (alerta && formulario.closest("dialog")?.open) {
        alerta.textContent = erro.message;
        alerta.hidden = false;
      } else {
        aviso(erro.message, true);
      }
    } finally {
      botao.disabled = !podeEnviar();
    }
  });
}

async function concluirVeiculo(idDialogo, mensagem) {
  porId(idDialogo).close();
  aviso(mensagem);
  try {
    await carregarVeiculos();
  } catch (erro) {
    aviso(mensagem + " A lista precisa ser consultada novamente. " + erro.message, true);
  }
}

async function concluirCatalogo(mensagem, recarregarEstoque = false) {
  aviso(mensagem);
  try {
    const leituras = [carregarCatalogo()];
    if (recarregarEstoque) leituras.push(carregarVeiculos());
    await Promise.all(leituras);
  } catch (erro) {
    aviso(mensagem + " Recarregue a página. " + erro.message, true);
  }
}

porId("nav-estoque").addEventListener("click", () => mostrarTela(false));
porId("nav-catalogo").addEventListener("click", () => mostrarTela(true));
porId("novo-veiculo").addEventListener("click", abrirCadastro);
porId("ir-catalogo").addEventListener("click", () => {
  porId("dialogo-cadastro").close();
  mostrarTela(true);
  porId("marca-nome").focus();
});
for (const b of document.querySelectorAll(".fechar,.cancelar")) {
  b.addEventListener("click", () => b.closest("dialog").close());
}
porId("linhas-veiculos").addEventListener("click", escolherAcao);
porId("cad-marca").addEventListener("change", () => opcoesModelos("cad-marca", "cad-modelo", false));
porId("f-marca").addEventListener("change", () => opcoesModelos("f-marca", "f-modelo", true));

aoEnviar("form-marca", async () => {
  const nova = await api("marcas", "POST", { nome: porId("marca-nome").value.trim() });
  porId("form-marca").reset();
  await concluirCatalogo("Marca " + nova.nome + " cadastrada.");
  porId("modelo-marca").value = nova.id;
});
aoEnviar("form-modelo", async () => {
  const novo = await api("modelos", "POST", {
    marcaId: Number(porId("modelo-marca").value), nome: porId("modelo-nome").value.trim()
  });
  porId("modelo-nome").value = "";
  await concluirCatalogo("Modelo " + novo.nome + " cadastrado.");
});
aoEnviar("form-editar-modelo", () => concluirDialogoModelo("dialogo-editar-modelo", () => salvarModelo({
  marcaId: Number(porId("editar-modelo-marca").value), nome: porId("editar-modelo-nome").value.trim()
})));
aoEnviar("form-excluir-modelo", () => concluirDialogoModelo("dialogo-excluir-modelo", excluirModelo),
  () => exclusaoModeloPermitida);
aoEnviar("form-renomear-marca", async () => {
  const dialogo = porId("dialogo-renomear-marca");
  const saidas = dialogo.querySelectorAll(".cancelar,.fechar");
  for (const botao of saidas) botao.disabled = true;
  try {
    const atualizada = await api("marcas/" + marcaEmEdicao.id, "PATCH", {
      nome: porId("novo-nome-marca").value.trim()
    });
    // O identificador dos registros e dos filtros continua sendo o mesmo.
    marcas = marcas.map((m) => m.id === atualizada.id ? atualizada : m);
    modelos = modelos.map((m) => m.marcaId === atualizada.id ? { ...m, marca: atualizada.nome } : m);
    veiculos = veiculos.map((v) => v.marcaId === atualizada.id ? { ...v, marca: atualizada.nome } : v);
    mostrarCatalogo();
    mostrarVeiculos();
    await concluirCatalogo("Nome da marca atualizado para " + atualizada.nome + ".", true);
    dialogo.close();
  } finally {
    for (const botao of saidas) botao.disabled = false;
  }
});
aoEnviar("form-exclusao-marca", async () => {
  const marca = marcaEmExclusao;
  const dialogo = porId("dialogo-exclusao-marca");
  const saidas = dialogo.querySelectorAll(".cancelar,.fechar");
  for (const botao of saidas) botao.disabled = true;
  let mensagem = "Marca " + marca.nome + " excluída do catálogo.";
  try {
    await api("marcas/" + marca.id, "DELETE");
  } catch (erro) {
    if (erro.status === 409) {
      // Outro usuário pode ter cadastrado um modelo depois que o diálogo foi aberto.
      try {
        await carregarCatalogo();
        descreverVinculos(marca.id);
      } catch {
        exclusaoMarcaPermitida = false;
        porId("vinculos-exclusao-marca").textContent = "O servidor encontrou modelos vinculados. Para corrigir o nome, use Renomear.";
      }
      porId("cancelar-exclusao-marca").disabled = false;
      porId("cancelar-exclusao-marca").focus();
      throw erro;
    }
    if (erro.status !== 404) throw erro;
    mensagem = "A marca " + marca.nome + " já havia sido excluída do catálogo.";
  } finally {
    for (const botao of saidas) botao.disabled = false;
  }

  const filtros = new URLSearchParams(consulta);
  const modelosRemovidos = modelos.filter((m) => m.marcaId === marca.id);
  if (filtros.get("marcaId") === String(marca.id)) filtros.delete("marcaId");
  if (modelosRemovidos.some((m) => String(m.id) === filtros.get("modeloId"))) filtros.delete("modeloId");
  const novaConsulta = filtros.toString();
  marcas = marcas.filter((m) => m.id !== marca.id);
  modelos = modelos.filter((m) => m.marcaId !== marca.id);
  mostrarCatalogo();
  dialogo.close();
  aviso(mensagem);
  if (consulta !== novaConsulta || veiculos.some((v) => v.marcaId === marca.id)) {
    consulta = novaConsulta;
    try { await carregarVeiculos(); } catch (erro) { aviso(mensagem + " " + erro.message, true); }
  }
}, () => exclusaoMarcaPermitida);
aoEnviar("form-veiculo", async () => {
  const v = await api("veiculos", "POST", {
    modeloId: Number(porId("cad-modelo").value), ano: Number(porId("cad-ano").value),
    cor: porId("cad-cor").value.trim(), preco: Number(porId("cad-preco").value),
    quilometragem: Number(porId("cad-km").value), tipo: porId("cad-tipo").value, status: porId("cad-status").value
  });
  await concluirVeiculo("dialogo-cadastro", "Veículo #" + v.id + " cadastrado.");
});
aoEnviar("form-edicao", async () => {
  const v = await api("veiculos/" + idEmEdicao, "PATCH", {
    preco: Number(porId("ed-preco").value), quilometragem: Number(porId("ed-km").value), status: porId("ed-status").value
  });
  await concluirVeiculo("dialogo-edicao", "Veículo #" + v.id + " atualizado.");
});
aoEnviar("form-exclusao", async () => {
  await api("veiculos/" + idEmExclusao, "DELETE");
  await concluirVeiculo("dialogo-exclusao", "Veículo #" + idEmExclusao + " excluído do estoque.");
});
aoEnviar("form-filtros", async () => {
  const minimo = porId("f-preco-min").value;
  const maximo = porId("f-preco-max").value;
  if (minimo !== "" && maximo !== "" && Number(minimo) > Number(maximo)) {
    throw new Error("O preço mínimo deve ser menor ou igual ao preço máximo.");
  }
  const valores = new URLSearchParams();
  for (const [nome, valor] of new FormData(porId("form-filtros"))) {
    if (valor !== "") valores.set(nome, valor);
  }
  consulta = valores.toString();
  aviso("");
  await carregarVeiculos();
});
porId("limpar-filtros").addEventListener("click", async () => {
  porId("form-filtros").reset();
  opcoesModelos("f-marca", "f-modelo", true);
  consulta = "";
  aviso("");
  try { await carregarVeiculos(); } catch (erro) { aviso(erro.message, true); }
});

opcoes("f-tipo", Object.entries(tipos), "Todos os tipos");
opcoes("f-status", Object.entries(estados), "Todos os status");
opcoes("cad-tipo", Object.entries(tipos), null);
opcoes("cad-status", Object.entries(estados), null);
opcoes("ed-status", Object.entries(estados), null);
porId("cad-ano").max = new Date().getFullYear();
porId("f-ano").max = new Date().getFullYear();
Promise.all([carregarCatalogo(), carregarVeiculos()]).catch((erro) => aviso(erro.message, true));
