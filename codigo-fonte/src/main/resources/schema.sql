CREATE TABLE IF NOT EXISTS marcas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    nome_chave VARCHAR(160) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS modelos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    marca_id BIGINT NOT NULL,
    nome VARCHAR(80) NOT NULL,
    nome_chave VARCHAR(160) NOT NULL,
    CONSTRAINT fk_modelo_marca FOREIGN KEY (marca_id) REFERENCES marcas(id),
    CONSTRAINT uq_modelo_marca UNIQUE (marca_id, nome_chave)
);

CREATE TABLE IF NOT EXISTS veiculos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    modelo_id BIGINT NOT NULL,
    ano INT NOT NULL CHECK (ano >= 1886),
    cor VARCHAR(80) NOT NULL,
    preco DECIMAL(12,2) NOT NULL CHECK (preco > 0),
    quilometragem INT NOT NULL CHECK (quilometragem >= 0),
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('CARRO','MOTO','CAMINHAO','UTILITARIO')),
    status VARCHAR(15) NOT NULL CHECK (status IN ('DISPONIVEL','VENDIDO','DESCONTINUADO')),
    CONSTRAINT fk_veiculo_modelo FOREIGN KEY (modelo_id) REFERENCES modelos(id),
    INDEX idx_veiculo_modelo (modelo_id),
    INDEX idx_veiculo_status_preco (status, preco),
    INDEX idx_veiculo_ano (ano),
    INDEX idx_veiculo_preco (preco)
);
