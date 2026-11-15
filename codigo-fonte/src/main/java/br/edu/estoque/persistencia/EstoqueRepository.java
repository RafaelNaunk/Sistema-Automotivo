package br.edu.estoque.persistencia;

import br.edu.estoque.api.Dados.*;
import br.edu.estoque.dominio.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EstoqueRepository {
    private static final String LER_MODELOS = """
        SELECT mo.id, mo.nome, mo.marca_id, ma.nome AS marca
          FROM modelos mo JOIN marcas ma ON ma.id = mo.marca_id
        """;
    private static final String LER_VEICULOS = """
        SELECT v.id, v.modelo_id, mo.nome AS modelo, mo.marca_id, ma.nome AS marca,
               v.ano, v.cor, v.preco, v.quilometragem, v.tipo, v.status
          FROM veiculos v
          JOIN modelos mo ON mo.id = v.modelo_id
          JOIN marcas ma ON ma.id = mo.marca_id
        """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate comandos;

    public EstoqueRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate comandos) {
        this.jdbc = jdbc;
        this.comandos = comandos;
    }

    public List<MarcaDto> marcas() {
        return jdbc.query("SELECT id, nome FROM marcas ORDER BY nome, id",
                (rs, linha) -> new MarcaDto(rs.getLong("id"), rs.getString("nome")));
    }

    public MarcaDto marca(long id) {
        return consultarMarca(id, false);
    }

    private MarcaDto consultarMarca(long id, boolean bloquear) {
        String consulta = "SELECT id, nome FROM marcas WHERE id = ?" + (bloquear ? " FOR UPDATE" : "");
        return jdbc.query(consulta, (rs, linha) ->
                new MarcaDto(rs.getLong("id"), rs.getString("nome")), id)
                .stream().findFirst().orElseThrow(() -> ausente("Marca"));
    }

    @Transactional
    public MarcaDto cadastrar(Marca marca) {
        var valores = new MapSqlParameterSource("nome", marca.getNome()).addValue("chave", marca.chaveDoNome());
        try {
            long id = inserir("INSERT INTO marcas(nome, nome_chave) VALUES (:nome, :chave)", valores);
            return new MarcaDto(id, marca.getNome());
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Já existe uma marca com esse nome.", e);
        }
    }

    @Transactional
    public MarcaDto renomearMarca(long id, Marca marca) {
        consultarMarca(id, true);
        try {
            jdbc.update("UPDATE marcas SET nome = ?, nome_chave = ? WHERE id = ?",
                    marca.getNome(), marca.chaveDoNome(), id);
            return new MarcaDto(id, marca.getNome());
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Já existe uma marca com esse nome.", e);
        }
    }

    @Transactional
    public void removerMarca(long id) {
        // A chave estrangeira recusa o DELETE se houver um modelo, mesmo sem veículos.
        try {
            int removidas = jdbc.update("DELETE FROM marcas WHERE id = ?", id);
            if (removidas == 0) {
                throw ausente("Marca");
            }
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Não é possível excluir uma marca com modelos vinculados.", e);
        }
    }

    public List<ModeloDto> modelos(Long marcaId) {
        String ordenar = " ORDER BY ma.nome, mo.nome, mo.id";
        if (marcaId == null) {
            return jdbc.query(LER_MODELOS + ordenar, this::lerModelo);
        }
        return jdbc.query(LER_MODELOS + " WHERE mo.marca_id = ?" + ordenar, this::lerModelo, marcaId);
    }

    public ModeloDto modelo(long id) {
        return jdbc.query(LER_MODELOS + " WHERE mo.id = ?", this::lerModelo, id)
                .stream().findFirst().orElseThrow(() -> ausente("Modelo"));
    }

    private void bloquearModelo(long id) {
        if (jdbc.queryForList("SELECT id FROM modelos WHERE id = ? FOR UPDATE", Long.class, id).isEmpty()) {
            throw ausente("Modelo");
        }
    }

    @Transactional
    public ModeloDto cadastrar(Modelo modelo) {
        // Serializa o vínculo do novo modelo com uma possível exclusão simultânea da marca.
        MarcaDto marca = consultarMarca(modelo.getMarcaId(), true);
        var valores = new MapSqlParameterSource("nome", modelo.getNome())
                .addValue("chave", modelo.chaveDoNome()).addValue("marca", marca.id());
        try {
            long id = inserir("INSERT INTO modelos (nome, nome_chave, marca_id) VALUES (:nome, :chave, :marca)", valores);
            return new ModeloDto(id, modelo.getNome(), marca.id(), marca.nome());
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Já existe um modelo com esse nome nesta marca.", e);
        }
    }

    @Transactional
    public ModeloDto editarModelo(long id, Modelo modelo) {
        MarcaDto marca = consultarMarca(modelo.getMarcaId(), true);
        bloquearModelo(id);
        try {
            jdbc.update("UPDATE modelos SET nome = ?, nome_chave = ?, marca_id = ? WHERE id = ?",
                    modelo.getNome(), modelo.chaveDoNome(), marca.id(), id);
            return new ModeloDto(id, modelo.getNome(), marca.id(), marca.nome());
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Já existe um modelo com esse nome nesta marca.", e);
        }
    }

    @Transactional
    public void removerModelo(long id) {
        // A chave estrangeira protege o modelo enquanto houver um veículo de qualquer status.
        try {
            int removidos = jdbc.update("DELETE FROM modelos WHERE id = ?", id);
            if (removidos == 0) {
                throw ausente("Modelo");
            }
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Não é possível excluir um modelo com veículos vinculados.", e);
        }
    }

    public VeiculoDto veiculo(long id) {
        return jdbc.query(LER_VEICULOS + " WHERE v.id = ?", this::lerVeiculo, id)
                .stream().findFirst().orElseThrow(() -> ausente("Veículo"));
    }

    public List<VeiculoDto> veiculos(Filtro filtro) {
        List<String> condicoes = new ArrayList<>();
        var valores = new MapSqlParameterSource();
        adicionarFiltro(condicoes, valores, "mo.marca_id = :marca", "marca", filtro.marcaId());
        adicionarFiltro(condicoes, valores, "v.modelo_id = :modelo", "modelo", filtro.modeloId());
        adicionarFiltro(condicoes, valores, "v.preco >= :min", "min", filtro.precoMin());
        adicionarFiltro(condicoes, valores, "v.preco <= :max", "max", filtro.precoMax());
        adicionarFiltro(condicoes, valores, "v.ano = :ano", "ano", filtro.ano());
        if (filtro.status() != null) {
            adicionarFiltro(condicoes, valores, "v.status = :status", "status", filtro.status().name());
        }
        if (filtro.tipo() != null) {
            adicionarFiltro(condicoes, valores, "v.tipo = :tipo", "tipo", filtro.tipo().name());
        }
        String onde = condicoes.isEmpty() ? "" : " WHERE " + String.join(" AND ", condicoes);
        return comandos.query(LER_VEICULOS + onde + " ORDER BY v.id DESC", valores, this::lerVeiculo);
    }

    @Transactional
    public VeiculoDto cadastrar(Veiculo veiculo) {
        bloquearModelo(veiculo.getModeloId());
        var valores = new MapSqlParameterSource("modelo", veiculo.getModeloId())
                .addValue("ano", veiculo.getAno()).addValue("cor", veiculo.getCor())
                .addValue("preco", veiculo.getPreco()).addValue("km", veiculo.getQuilometragem())
                .addValue("tipo", veiculo.getTipo().name()).addValue("status", veiculo.getStatus().name());
        long id = inserir("""
            INSERT INTO veiculos (modelo_id, ano, cor, preco, quilometragem, tipo, status)
            VALUES (:modelo, :ano, :cor, :preco, :km, :tipo, :status)
            """, valores);
        return veiculo(id);
    }

    @Transactional
    public VeiculoDto atualizar(long id, Atualizacao dados) {
        Veiculo veiculo = bloquear(id);
        veiculo.atualizar(dados.preco, dados.quilometragem, dados.status);
        jdbc.update("UPDATE veiculos SET preco = ?, quilometragem = ?, status = ? WHERE id = ?",
                veiculo.getPreco(), veiculo.getQuilometragem(), veiculo.getStatus().name(), id);
        return veiculo(id);
    }

    @Transactional
    public void remover(long id) {
        Veiculo veiculo = bloquear(id);
        if (!veiculo.podeRemover()) {
            throw new IllegalStateException("Só é possível remover veículos vendidos ou descontinuados.");
        }
        jdbc.update("DELETE FROM veiculos WHERE id = ?", id);
    }

    // O bloqueio dura até o fim da transação. Uma venda e uma exclusão simultâneas leem estado consistente.
    private Veiculo bloquear(long id) {
        return jdbc.query("SELECT * FROM veiculos WHERE id = ? FOR UPDATE", (rs, linha) ->
                new Veiculo(rs.getLong("modelo_id"), rs.getInt("ano"), rs.getString("cor"),
                        TipoVeiculo.valueOf(rs.getString("tipo")), rs.getBigDecimal("preco"),
                        rs.getInt("quilometragem"), StatusVeiculo.valueOf(rs.getString("status"))), id)
                .stream().findFirst().orElseThrow(() -> ausente("Veículo"));
    }

    private long inserir(String sql, MapSqlParameterSource valores) {
        var chave = new GeneratedKeyHolder();
        comandos.update(sql, valores, chave, new String[] {"id"});
        return java.util.Objects.requireNonNull(chave.getKey()).longValue();
    }

    private static void adicionarFiltro(List<String> condicoes, MapSqlParameterSource valores,
                                        String condicao, String nome, Object valor) {
        if (valor != null) {
            condicoes.add(condicao);
            valores.addValue(nome, valor);
        }
    }

    private ModeloDto lerModelo(ResultSet rs, int linha) throws SQLException {
        return new ModeloDto(rs.getLong("id"), rs.getString("nome"),
                rs.getLong("marca_id"), rs.getString("marca"));
    }

    private VeiculoDto lerVeiculo(ResultSet rs, int linha) throws SQLException {
        return new VeiculoDto(rs.getLong("id"), rs.getLong("modelo_id"), rs.getString("modelo"),
                rs.getLong("marca_id"), rs.getString("marca"), rs.getInt("ano"), rs.getString("cor"),
                rs.getBigDecimal("preco"), rs.getInt("quilometragem"),
                TipoVeiculo.valueOf(rs.getString("tipo")), StatusVeiculo.valueOf(rs.getString("status")));
    }

    private static NoSuchElementException ausente(String entidade) {
        return new NoSuchElementException(entidade + " não encontrado(a).");
    }
}
