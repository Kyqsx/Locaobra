-- ============================================================================
--  MODELO FÍSICO DO BANCO DE DADOS — LOCAOBRA
--  Sistema: Aluguel de Equipamentos de Obra
--  Banco:   PostgreSQL (Supabase)
--  -------------------------------------------------------------
--  Gerado a partir das entidades JPA em /src/main/java/com/locaobra/entity
--  (schema mantido hoje via "spring.jpa.hibernate.ddl-auto=update").
--  Este script é o modelo físico de referência (DDL explícito).
-- ============================================================================


-- ============================================================================
-- 1) DEPARTAMENTOS
--    Entidade: DepartamentoEntity -> @Table(name = "departamentos")
-- ============================================================================
CREATE TABLE IF NOT EXISTS departamentos (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL UNIQUE,
    descricao      VARCHAR(500),
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP,
    atualizado_em  TIMESTAMP
);

-- ============================================================================
-- 1-B) DEPOSITOS
--    Entidade: Deposito -> @Table(name = "depositos")
--    Local físico (galpão/pátio) onde ficam as unidades de equipamento e onde
--    atuam os funcionários de logística. Vínculo é sempre com a UNIDADE
--    (unidades_equipamento.deposito_id) e com o FUNCIONÁRIO
--    (funcionarios.deposito_id) — nunca com o modelo de Equipamento.
--    Criada aqui, antes de FUNCIONARIOS e UNIDADES_EQUIPAMENTO, pra essas FKs
--    poderem referenciá-la.
-- ============================================================================
CREATE TABLE IF NOT EXISTS depositos (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL UNIQUE,
    endereco       VARCHAR(500),
    descricao      VARCHAR(500),
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP,
    atualizado_em  TIMESTAMP
);

-- ============================================================================
-- 2) CARGOS
--    Entidade: Cargo -> @Table(name = "cargos")
-- ============================================================================
CREATE TABLE IF NOT EXISTS cargos (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(80)  NOT NULL UNIQUE,
    descricao      VARCHAR(500),
    salario_padrao DOUBLE PRECISION,
    requisitos     VARCHAR(1000),
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    departamento_id BIGINT,
    criado_em      TIMESTAMP,
    atualizado_em  TIMESTAMP,
    CONSTRAINT fk_cargo_departamento FOREIGN KEY (departamento_id)
        REFERENCES departamentos (id)
);

CREATE INDEX IF NOT EXISTS idx_cargos_departamento ON cargos (departamento_id);

-- ============================================================================
-- 3) CLIENTES
--    Entidade: Cliente -> @Table(name = "clientes")
-- ============================================================================
CREATE TABLE IF NOT EXISTS clientes (
    id        BIGSERIAL PRIMARY KEY,
    nome      VARCHAR(150) NOT NULL,
    cpf_cnpj  VARCHAR(20)  NOT NULL,
    telefone  VARCHAR(20),
    ativo     BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_clientes_cpf_cnpj ON clientes (cpf_cnpj);

-- ============================================================================
-- 4) ENDERECO
--    Entidade: Endereco -> @Table(name = "Endereco")  (nome/colunas em caixa
--    alta direta na definição; exigem aspas duplas no PostgreSQL).
-- ============================================================================
CREATE TABLE IF NOT EXISTS "Endereco" (
    id_endereco BIGSERIAL PRIMARY KEY,
    "Cep"        VARCHAR(15),
    "Rua"        VARCHAR(100),
    "Bairro"     VARCHAR(50),
    "Cidade"     VARCHAR(50),
    "Estado"     VARCHAR(50),
    "Complemento" VARCHAR(20),
    "Numero"      VARCHAR(10)
);

-- ============================================================================
-- 5) FUNCIONARIOS
--    Entidade: Funcionario -> @Table(name = "funcionarios")
-- ============================================================================
CREATE TABLE IF NOT EXISTS funcionarios (
    id               BIGSERIAL PRIMARY KEY,
    matricula        VARCHAR(30)  NOT NULL UNIQUE,
    nome             VARCHAR(150) NOT NULL,
    cpf              VARCHAR(20)  NOT NULL UNIQUE,
    telefone         VARCHAR(20)  UNIQUE,
    data_nascimento  TIMESTAMP NOT NULL,
    cargo_id         BIGINT,
    departamento_id  BIGINT,
    deposito_id      BIGINT,
    salario          DOUBLE PRECISION NOT NULL,
    data_admissao    TIMESTAMP NOT NULL,
    data_demissao    TIMESTAMP,
    status           BOOLEAN NOT NULL,
    CONSTRAINT fk_funcionario_cargo FOREIGN KEY (cargo_id)
        REFERENCES cargos (id),
    CONSTRAINT fk_funcionario_departamento FOREIGN KEY (departamento_id)
        REFERENCES departamentos (id),
    CONSTRAINT fk_funcionario_deposito FOREIGN KEY (deposito_id)
        REFERENCES depositos (id)
);

CREATE INDEX IF NOT EXISTS idx_funcionarios_cargo ON funcionarios (cargo_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_departamento ON funcionarios (departamento_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_deposito ON funcionarios (deposito_id);

-- ============================================================================
-- 6) USUARIOS
--    Entidade: Usuario -> @Table(name = "usuarios")
--    OBS: id_cliente / id_funcionario / id_endereco são colunas "longas
--    soltas" (IDs brutos, sem relacionamento JPA); portanto NÃO há FK
--    física para elas no banco.
-- ============================================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    senha         VARCHAR(255) NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,       -- TipoUsuario: ADMIN | FUNCIONARIO | CLIENTE
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP NOT NULL,
    id_cliente    BIGINT,
    id_funcionario BIGINT,
    id_endereco   BIGINT
);
-- ============================================================================
-- 7) EQUIPAMENTOS
--    Entidade: Equipamento -> @Table(name = "equipamentos")
-- ============================================================================
CREATE TABLE IF NOT EXISTS equipamentos (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(200) NOT NULL,
    descricao     VARCHAR(500),
    categoria     VARCHAR(100) NOT NULL,
    valor_diaria  NUMERIC NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'ativo',
    criado_em     TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP
);

-- ============================================================================
-- 8) ESPECIFICACOES_EQUIPAMENTO
--    Entidade: EspecificacaoEquipamento (chave/valor)
-- ============================================================================
CREATE TABLE IF NOT EXISTS especificacoes_equipamento (
    id            BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL,
    chave         VARCHAR(100) NOT NULL,
    valor         VARCHAR(500) NOT NULL,
    CONSTRAINT fk_espec_equipamento FOREIGN KEY (equipamento_id)
        REFERENCES equipamentos (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_espec_equipamento ON especificacoes_equipamento (equipamento_id);

-- ============================================================================
-- 9) IMAGENS_EQUIPAMENTO
--    Entidade: ImagemEquipamento
-- ============================================================================
CREATE TABLE IF NOT EXISTS imagens_equipamento (
    id            BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL,
    url           VARCHAR(255) NOT NULL,
    ordem         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_imagem_equipamento FOREIGN KEY (equipamento_id)
        REFERENCES equipamentos (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_imagem_equipamento ON imagens_equipamento (equipamento_id);

-- ============================================================================
-- 10) UNIDADES_EQUIPAMENTO
--    Entidade: UnidadeEquipamento (patrimônio físico unitário)
--    Campos enums (VARCHAR): status = StatusUnidade
-- ============================================================================
CREATE TABLE IF NOT EXISTS unidades_equipamento (
    id                          BIGSERIAL PRIMARY KEY,
    equipamento_id              BIGINT NOT NULL,
    codigo_patrimonio           VARCHAR(100) UNIQUE,
    numero_serie                VARCHAR(100) UNIQUE,
    status                      VARCHAR(30) NOT NULL DEFAULT 'DISPONIVEL',
    horimetro_atual             DOUBLE PRECISION,
    horimetro_limite_manutencao DOUBLE PRECISION,
    -- Depósito físico onde essa unidade está guardada hoje. Vínculo é sempre
    -- na unidade (patrimônio), nunca no modelo de Equipamento — o mesmo
    -- modelo pode ter unidades espalhadas em depósitos diferentes.
    deposito_id                 BIGINT,
    criado_em                   TIMESTAMP NOT NULL,
    atualizado_em               TIMESTAMP,
    CONSTRAINT fk_unidade_equipamento FOREIGN KEY (equipamento_id)
        REFERENCES equipamentos (id) ON DELETE CASCADE,
    CONSTRAINT fk_unidade_deposito FOREIGN KEY (deposito_id)
        REFERENCES depositos (id),
    CONSTRAINT ck_unidade_status CHECK (status IN (
        'DISPONIVEL', 'ALUGADO', 'EM_LIMPEZA',
        'AGUARDANDO_MANUTENCAO', 'EM_MANUTENCAO'
    ))
);

CREATE INDEX IF NOT EXISTS idx_unidade_equipamento ON unidades_equipamento (equipamento_id);
CREATE INDEX IF NOT EXISTS idx_unidade_deposito ON unidades_equipamento (deposito_id);

-- ============================================================================
-- 11) PECAS_ESTOQUE
--    Entidade: PecaEstoque
-- ============================================================================
CREATE TABLE IF NOT EXISTS pecas_estoque (
    id                   BIGSERIAL PRIMARY KEY,
    codigo               VARCHAR(60) UNIQUE,
    nome                 VARCHAR(200) NOT NULL,
    quantidade_em_estoque INTEGER NOT NULL DEFAULT 0,
    unidade_medida       VARCHAR(20),
    estoque_minimo       INTEGER,
    criado_em            TIMESTAMP NOT NULL,
    atualizado_em        TIMESTAMP
);
-- ============================================================================
-- 11-B) PEDIDOS
--    Entidade: Pedido. Fluxo: cliente solicita (SOLICITADO) -> consultor
--    confirma (CONFIRMADO) ou recusa (RECUSADO) -> analista de credenciamento
--    aprova (APROVADO) ou reprova (REPROVADO) -> conferente gera a expedição
--    (ver seção 12, expedicoes.pedido_id). Cliente pode cancelar antes da
--    aprovação de crédito (CANCELADO).
--    Observação: essa tabela e a de itens_pedido não estavam neste script de
--    referência (adicionadas aqui junto com o campo expedicoes.pedido_id).
-- ============================================================================
CREATE TABLE IF NOT EXISTS pedidos (
    id                     BIGSERIAL PRIMARY KEY,
    codigo                 VARCHAR(50) NOT NULL UNIQUE,
    status                 VARCHAR(20) NOT NULL DEFAULT 'SOLICITADO',
    cliente_id             BIGINT NOT NULL,
    consultor_id           BIGINT,
    analista_credito_id    BIGINT,
    data_inicio            DATE NOT NULL,
    data_fim               DATE NOT NULL,
    endereco_entrega       VARCHAR(500) NOT NULL,
    observacoes_cliente    VARCHAR(1000),
    observacoes_consultor  VARCHAR(1000),
    motivo_recusa          VARCHAR(1000),
    valor_total_estimado   NUMERIC(12,2) NOT NULL DEFAULT 0,
    confirmado_em          TIMESTAMP,
    analisado_em           TIMESTAMP,
    cancelado_em           TIMESTAMP,
    criado_em              TIMESTAMP NOT NULL,
    atualizado_em          TIMESTAMP,
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id),
    CONSTRAINT fk_pedido_consultor FOREIGN KEY (consultor_id)
        REFERENCES funcionarios (id),
    CONSTRAINT fk_pedido_analista_credito FOREIGN KEY (analista_credito_id)
        REFERENCES funcionarios (id),
    CONSTRAINT ck_pedido_status CHECK (status IN (
        'SOLICITADO', 'CONFIRMADO', 'APROVADO', 'RECUSADO', 'REPROVADO', 'CANCELADO'
    ))
);

CREATE INDEX IF NOT EXISTS idx_pedidos_cliente ON pedidos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_status ON pedidos (status);

-- ============================================================================
-- 11-C) ITENS_PEDIDO
--    Entidade: ItemPedido. valor_diaria_snapshot: preço da diária travado no
--    momento do pedido (não muda se o preço do equipamento mudar depois).
-- ============================================================================
CREATE TABLE IF NOT EXISTS itens_pedido (
    id                     BIGSERIAL PRIMARY KEY,
    pedido_id              BIGINT NOT NULL,
    equipamento_id         BIGINT NOT NULL,
    quantidade             INTEGER NOT NULL,
    valor_diaria_snapshot  NUMERIC(12,2) NOT NULL,
    observacao_item        VARCHAR(500),
    criado_em              TIMESTAMP NOT NULL,
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedidos (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_pedido_equipamento FOREIGN KEY (equipamento_id)
        REFERENCES equipamentos (id)
);

CREATE INDEX IF NOT EXISTS idx_itens_pedido_pedido ON itens_pedido (pedido_id);

-- ============================================================================
-- 12) EXPEDICOES
--    Entidade: Expedicao.
--    entrega_origem_id: auto-relacionamento (COLETA aponta para ENTREGA).
--    pedido_id: preenchido quando a expedição (ENTREGA) foi gerada pelo
--    Conferente a partir de um pedido APROVADO (ver seção 11-B).
--    Os enums tipo/status podem ser validados por CHECK (opcional).
-- ============================================================================
CREATE TABLE IF NOT EXISTS expedicoes (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(50) NOT NULL UNIQUE,
    tipo                VARCHAR(20) NOT NULL,          -- ENTREGA | COLETA
    status              VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
    cliente_id          BIGINT,
    motorista_id        BIGINT,
    entrega_origem_id   BIGINT,
    -- Preenchido quando a expedição (ENTREGA) foi gerada pelo Conferente a
    -- partir de um Pedido já APROVADO. Null pra expedições avulsas e pra COLETA.
    pedido_id           BIGINT,
    nome_autorizado_1   VARCHAR(150),
    nome_autorizado_2   VARCHAR(150),
    nome_autorizado_3   VARCHAR(150),
    placa_veiculo       VARCHAR(20),
    data_programada     DATE NOT NULL,
    horario_programado  VARCHAR(255),
    endereco_entrega    VARCHAR(500),
    observacoes         VARCHAR(1000),
    checkout_em         TIMESTAMP,
    checkin_em          TIMESTAMP,
    assinatura_cliente  VARCHAR(255),
    entrega_confirmada_em TIMESTAMP,
    assinatura_entrega  VARCHAR(255),
    foto_entrega        VARCHAR(500),
    criado_em           TIMESTAMP NOT NULL,
    atualizado_em       TIMESTAMP,
    CONSTRAINT fk_expedicao_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id),
    CONSTRAINT fk_expedicao_motorista FOREIGN KEY (motorista_id)
        REFERENCES funcionarios (id),
    CONSTRAINT fk_expedicao_origem FOREIGN KEY (entrega_origem_id)
        REFERENCES expedicoes (id),
    CONSTRAINT fk_expedicao_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedidos (id),
    CONSTRAINT ck_expedicao_status CHECK (status IN (
        'AGENDADO', 'EM_TRANSITO', 'ENTREGUE', 'CONCLUIDO', 'CANCELADO'
    )),
    CONSTRAINT ck_expedicao_tipo CHECK (tipo IN ('ENTREGA', 'COLETA'))
);

CREATE INDEX IF NOT EXISTS idx_expedicoes_cliente ON expedicoes (cliente_id);
CREATE INDEX IF NOT EXISTS idx_expedicoes_motorista ON expedicoes (motorista_id);
CREATE INDEX IF NOT EXISTS idx_expedicoes_origem ON expedicoes (entrega_origem_id);
CREATE INDEX IF NOT EXISTS idx_expedicoes_pedido ON expedicoes (pedido_id);

-- ============================================================================
-- 13) ITENS_EXPEDICAO
--    Entidade: ItemExpedicao
-- ============================================================================
CREATE TABLE IF NOT EXISTS itens_expedicao (
    id             BIGSERIAL PRIMARY KEY,
    expedicao_id   BIGINT NOT NULL,
    unidade_id     BIGINT,
    equipamento_id BIGINT,
    quantidade     INTEGER NOT NULL,
    observacao_item VARCHAR(500),
    criado_em      TIMESTAMP NOT NULL,
    CONSTRAINT fk_itemexp_expedicao FOREIGN KEY (expedicao_id)
        REFERENCES expedicoes (id) ON DELETE CASCADE,
    CONSTRAINT fk_itemexp_unidade FOREIGN KEY (unidade_id)
        REFERENCES unidades_equipamento (id),
    CONSTRAINT fk_itemexp_equipamento FOREIGN KEY (equipamento_id)
        REFERENCES equipamentos (id)
);

CREATE INDEX IF NOT EXISTS idx_itens_expedicao_expedicao ON itens_expedicao (expedicao_id);
CREATE INDEX IF NOT EXISTS idx_itens_expedicao_unidade ON itens_expedicao (unidade_id);
CREATE INDEX IF NOT EXISTS idx_itens_expedicao_equipamento ON itens_expedicao (equipamento_id);

-- ============================================================================
-- 14) VISTORIAS
--    Entidade: Vistoria. tipo: TipoVistoria
-- ============================================================================
CREATE TABLE IF NOT EXISTS vistorias (
    id                BIGSERIAL PRIMARY KEY,
    expedicao_id      BIGINT NOT NULL,
    unidade_id        BIGINT,
    tipo              VARCHAR(20) NOT NULL,             -- ENTREGA | DEVOLUCAO
    condicao_geral    VARCHAR(20),
    avarias_existentes VARCHAR(2000),
    danos_causados    VARCHAR(2000),
    observacoes       VARCHAR(1000),
    realizada_em      TIMESTAMP,
    criado_em         TIMESTAMP NOT NULL,
    CONSTRAINT fk_vistoria_expedicao FOREIGN KEY (expedicao_id)
        REFERENCES expedicoes (id) ON DELETE CASCADE,
    CONSTRAINT fk_vistoria_unidade FOREIGN KEY (unidade_id)
        REFERENCES unidades_equipamento (id),
    CONSTRAINT ck_vistoria_tipo CHECK (tipo IN ('ENTREGA', 'DEVOLUCAO'))
);

CREATE INDEX IF NOT EXISTS idx_vistorias_expedicao ON vistorias (expedicao_id);
CREATE INDEX IF NOT EXISTS idx_vistorias_unidade ON vistorias (unidade_id);

-- ============================================================================
-- 15) FOTOS_VISTORIA
--    Entidade: FotoVistoria
-- ============================================================================
CREATE TABLE IF NOT EXISTS fotos_vistoria (
    id         BIGSERIAL PRIMARY KEY,
    vistoria_id BIGINT NOT NULL,
    url        VARCHAR(500) NOT NULL,
    legenda    VARCHAR(300),
    criado_em  TIMESTAMP NOT NULL,
    CONSTRAINT fk_foto_vistoria FOREIGN KEY (vistoria_id)
        REFERENCES vistorias (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_fotos_vistoria_vistoria ON fotos_vistoria (vistoria_id);
-- ============================================================================
-- 16) ORDENS_SERVICO
--    Entidade: OrdemServico. status: StatusOrdemServico
-- ============================================================================
CREATE TABLE IF NOT EXISTS ordens_servico (
    id                  BIGSERIAL PRIMARY KEY,
    unidade_id          BIGINT NOT NULL,
    tecnico_id          BIGINT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    diagnostico         VARCHAR(2000),
    observacoes         VARCHAR(1000),
    horimetro_registrado DOUBLE PRECISION,
    aberta_em           TIMESTAMP NOT NULL,
    iniciada_em         TIMESTAMP,
    concluida_em        TIMESTAMP,
    criado_em           TIMESTAMP NOT NULL,
    atualizado_em       TIMESTAMP,
    CONSTRAINT fk_os_unidade FOREIGN KEY (unidade_id)
        REFERENCES unidades_equipamento (id),
    CONSTRAINT fk_os_tecnico FOREIGN KEY (tecnico_id)
        REFERENCES funcionarios (id),
    CONSTRAINT ck_os_status CHECK (status IN (
        'ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'
    ))
);

CREATE INDEX IF NOT EXISTS idx_os_unidade ON ordens_servico (unidade_id);
CREATE INDEX IF NOT EXISTS idx_os_tecnico ON ordens_servico (tecnico_id);

-- ============================================================================
-- 17) ITENS_ORDEM_SERVICA
--    Entidade: ItemOrdemServico
-- ============================================================================
CREATE TABLE IF NOT EXISTS itens_ordem_servico (
    id              BIGSERIAL PRIMARY KEY,
    ordem_servico_id BIGINT NOT NULL,
    peca_id         BIGINT NOT NULL,
    quantidade      INTEGER NOT NULL,
    criado_em       TIMESTAMP NOT NULL,
    CONSTRAINT fk_item_os_ordem FOREIGN KEY (ordem_servico_id)
        REFERENCES ordens_servico (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_os_peca FOREIGN KEY (peca_id)
        REFERENCES pecas_estoque (id)
);

CREATE INDEX IF NOT EXISTS idx_itens_os_ordem ON itens_ordem_servico (ordem_servico_id);
CREATE INDEX IF NOT EXISTS idx_itens_os_peca ON itens_ordem_servico (peca_id);

-- ============================================================================
-- 18) NOTIFICACOES
--    Entidade: Notificacao (destinatário/ref genéricos, sem FK física)
-- ============================================================================
CREATE TABLE IF NOT EXISTS notificacoes (
    id               BIGSERIAL PRIMARY KEY,
    tipo             VARCHAR(50) NOT NULL,
    titulo           VARCHAR(200) NOT NULL,
    mensagem         VARCHAR(2000),
    destinatario_tipo VARCHAR(30) NOT NULL,
    destinatario_id  BIGINT,
    referencia_tipo  VARCHAR(30),
    referencia_id    BIGINT,
    lida             BOOLEAN NOT NULL DEFAULT FALSE,
    criada_em        TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notificacoes_destinatario
    ON notificacoes (destinatario_tipo, destinatario_id);
CREATE INDEX IF NOT EXISTS idx_notificacoes_referencia
    ON notificacoes (referencia_tipo, referencia_id);