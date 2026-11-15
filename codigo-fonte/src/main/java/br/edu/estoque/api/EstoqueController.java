package br.edu.estoque.api;

import br.edu.estoque.api.Dados.*;
import br.edu.estoque.dominio.*;
import br.edu.estoque.persistencia.EstoqueRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EstoqueController {
    private final EstoqueRepository estoque;

    public EstoqueController(EstoqueRepository estoque) {
        this.estoque = estoque;
    }

    @GetMapping("/marcas")
    public List<MarcaDto> listarMarcas() { return estoque.marcas(); }

    @GetMapping("/marcas/{id}")
    public MarcaDto buscarMarca(@PathVariable long id) { return estoque.marca(id); }

    @PatchMapping("/marcas/{id}")
    public MarcaDto renomearMarca(@PathVariable long id, @RequestBody NovaMarca dados) {
        return estoque.renomearMarca(id, new Marca(dados.nome()));
    }

    @DeleteMapping("/marcas/{id}")
    public ResponseEntity<Void> removerMarca(@PathVariable long id) {
        estoque.removerMarca(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marcas")
    public ResponseEntity<MarcaDto> criarMarca(@RequestBody NovaMarca dados) {
        MarcaDto marca = estoque.cadastrar(new Marca(dados.nome()));
        return ResponseEntity.created(URI.create("/api/marcas/" + marca.id())).body(marca);
    }

    @GetMapping("/modelos")
    public List<ModeloDto> listarModelos(@RequestParam(required = false) Long marcaId) {
        return estoque.modelos(marcaId);
    }

    @GetMapping("/modelos/{id}")
    public ModeloDto buscarModelo(@PathVariable long id) { return estoque.modelo(id); }

    @PutMapping("/modelos/{id}")
    public ModeloDto editarModelo(@PathVariable long id, @RequestBody NovoModelo dados) {
        return estoque.editarModelo(id, new Modelo(dados.nome(), dados.marcaId()));
    }

    @DeleteMapping("/modelos/{id}")
    public ResponseEntity<Void> removerModelo(@PathVariable long id) {
        estoque.removerModelo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modelos")
    public ResponseEntity<ModeloDto> criarModelo(@RequestBody NovoModelo dados) {
        ModeloDto modelo = estoque.cadastrar(new Modelo(dados.nome(), dados.marcaId()));
        return ResponseEntity.created(URI.create("/api/modelos/" + modelo.id())).body(modelo);
    }

    @GetMapping("/veiculos")
    public List<VeiculoDto> listarVeiculos(@RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Long modeloId, @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax, @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) StatusVeiculo status, @RequestParam(required = false) TipoVeiculo tipo) {
        return estoque.veiculos(new Filtro(marcaId, modeloId, precoMin, precoMax, ano, status, tipo));
    }

    @GetMapping("/veiculos/{id}")
    public VeiculoDto buscarVeiculo(@PathVariable long id) { return estoque.veiculo(id); }

    @PostMapping("/veiculos")
    public ResponseEntity<VeiculoDto> criarVeiculo(@RequestBody NovoVeiculo dados) {
        Veiculo novo = new Veiculo(dados.modeloId(), dados.ano(), dados.cor(), dados.tipo(),
                dados.preco(), dados.quilometragem(), dados.status());
        VeiculoDto salvo = estoque.cadastrar(novo);
        return ResponseEntity.created(URI.create("/api/veiculos/" + salvo.id())).body(salvo);
    }

    @PatchMapping("/veiculos/{id}")
    public VeiculoDto atualizar(@PathVariable long id, @RequestBody Atualizacao dados) {
        return estoque.atualizar(id, dados);
    }

    @DeleteMapping("/veiculos/{id}")
    public ResponseEntity<Void> remover(@PathVariable long id) {
        estoque.remover(id);
        return ResponseEntity.noContent().build();
    }
}
