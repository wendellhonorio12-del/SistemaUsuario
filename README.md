# SistemaUsuario — Sistema de Gestão de Projetos e Equipes

Autor: Wendell

Aplicação **desktop Java Swing** para gestão de usuários, equipes, projetos, tarefas e relatórios, com **controle de acesso por perfil (RBAC)** e persistência local em **SQLite**.  
Arquitetura baseada em **MVC clássico** (View → Controller → DAO → banco), sem frameworks externos além do driver JDBC.

---

## 🚀 Funcionalidades

- Usuário: cadastro com validação de CPF e autenticação por login/senha.
- Equipe: criação de equipes e vínculo de usuários.
- Projeto: cadastro de projetos e associação de equipes.
- Escopo: filtragem de dados conforme participação do colaborador.
- Tarefa: criação de tarefas vinculadas a projetos e responsáveis.
- Relatório: geração de relatórios de desempenho e escopo.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|------------|--------|
| Linguagem | Java (JDK) | 21 |
| Build | Apache Maven | 3.6+ |
| Interface gráfica | Java Swing | nativo do JDK |
| Banco de dados | SQLite (`sqlite-jdbc`) | 3.53.1.0 |
| Testes | JUnit Jupiter (JUnit 5) | 5.14.4 |
| Empacotamento | `maven-shade-plugin` | 3.6.2 |

---

## 📐 Arquitetura

### Diagrama de componentes

```mermaid
flowchart LR
    subgraph Apresentacao["Apresentação (Swing)"]
        LoginView
        MainView["MainView (hub)"]
        Telas["Telas de recurso<br>(Usuário/Equipe/Projeto/Escopo/Tarefa/Relatório)"]
    end

    subgraph SessaoSeg["Sessão e Segurança"]
        AppConfig["AppConfig<br>(Singleton de sessão)"]
        PermissoesPerfil["PermissoesPerfil<br>(perfil → recursos)"]
        EscopoColaborador["EscopoColaborador<br>(dados filtrados)"]
    end

    subgraph Negocio["Negócio"]
        Controllers["Controllers"]
    end

    subgraph Persistencia["Persistência"]
        DAOs["DAOs (JDBC)"]
        DBConn["DatabaseConnection"]
    end

    DB[("SQLite<br>data/sistemausuario.db")]

    LoginView --> Controllers
    Controllers --> AppConfig
    MainView --> PermissoesPerfil
    MainView --> Telas
    Telas --> Controllers
    Telas --> EscopoColaborador
    Controllers --> DAOs
    EscopoColaborador --> DAOs
    DAOs --> DBConn
    DBConn --> DB
```

---

## Modelo de dados

Seis tabelas no SQLite (DDL em `src/main/resources/db/schema.sql`). As duas
tabelas de junção (`equipe_usuario`, `projeto_equipe`) modelam relações
muitos-para-muitos. Datas são persistidas como `TEXT` (ISO) e enums via o
`name()` da constante.

```mermaid
erDiagram
    usuarios {
        INTEGER id PK
        TEXT cpf UK
        TEXT nome_completo
        TEXT email
        TEXT login UK
        TEXT senha
        TEXT perfil
    }
    equipes {
        INTEGER id PK
        TEXT nome
        TEXT descricao
    }
    projetos {
        INTEGER id PK
        TEXT nome
        TEXT descricao
        TEXT data_inicio
        TEXT data_termino_prevista
        TEXT status
    }
    tarefas {
        INTEGER id PK
        TEXT titulo
        TEXT descricao
        INTEGER projeto_id FK
        INTEGER responsavel_id FK
        TEXT data_inicio
        TEXT data_termino_prevista
        TEXT status
    }

    usuarios  ||--o{ tarefas        : "responsável por"
    projetos  ||--o{ tarefas        : "contém"
    usuarios  ||--o{ equipe_usuario : "participa"
    equipes   ||--o{ equipe_usuario : "tem membro"
    projetos  ||--o{ projeto_equipe : "atendido por"
    equipes   ||--o{ projeto_equipe : "atua em"
```

---

## Fluxos

### Autenticação e navegação

```mermaid
sequenceDiagram
    actor U as Usuário
    participant LV as LoginView
    participant LC as LoginController
    participant AC as AppConfig
    participant MV as MainView
    participant PP as PermissoesPerfil
    participant T as Tela do recurso

    U->>LV: informa login e senha
    LV->>LC: autenticar(login, senha)
    LC->>AC: setUsuarioAutenticado(usuário)
    LC-->>LV: usuário autenticado
    LV->>MV: abre o hub (injeta controllers + escopo)
    MV->>PP: recursosDe(perfil)
    PP-->>MV: recursos permitidos
    MV-->>U: exibe apenas os botões permitidos
    U->>MV: clica num recurso
    MV->>PP: podeAcessar(perfil, recurso)?
    PP-->>MV: sim
    MV->>T: abre a tela (contexto: usuário, modo leitura/escopo)
    U->>MV: clica em "Sair"
    MV->>AC: limpar()
    MV->>LV: retorna ao login
```

### Estados de projeto e tarefa

```mermaid
stateDiagram-v2
    direction LR
    state "StatusProjeto" as SP {
        [*] --> PLANEJADO
        PLANEJADO --> EM_ANDAMENTO
        PLANEJADO --> CANCELADO
        EM_ANDAMENTO --> CONCLUIDO
        EM_ANDAMENTO --> CANCELADO
        CONCLUIDO --> [*]
        CANCELADO --> [*]
    }
```

```mermaid
stateDiagram-v2
    direction LR
    state "StatusTarefa" as ST {
        [*] --> PENDENTE
        PENDENTE --> EM_ANDAMENTO
        PENDENTE --> CANCELADA
        EM_ANDAMENTO --> CONCLUIDA
        EM_ANDAMENTO --> CANCELADA
        CONCLUIDA --> [*]
        CANCELADA --> [*]
    }
```

---

## Perfis e permissões

O acesso aos recursos é resolvido de forma declarativa em
`security/PermissoesPerfil` (mapa `PerfilUsuario → conjunto de recursos`). A
`MainView` renderiza apenas os botões permitidos e revalida o acesso antes de
abrir cada tela (defesa em profundidade).

| Recurso | Administrador | Gerente | Colaborador |
| --- | --- | --- | --- |
| Usuários | ✅ | — | — |
| Equipes | ✅ | ✅ | — |
| Projetos | ✅ | ✅ | — |
| Escopo | ✅ | ✅ | ✅ |
| Tarefas | ✅ | ✅ | ✅ (modo leitura) |
| Relatórios | ✅ | ✅ | ✅ |

O **Colaborador** visualiza apenas dados das equipes de que participa
(`EscopoColaborador`) e não pode criar, editar nem excluir registros.

---

## Pré-requisitos

| Ferramenta | Versão mínima |
| --- | --- |
| JDK | 21 |
| Apache Maven | 3.6 |
| Git | qualquer |
| Ambiente gráfico | A interface Swing exige um display (não roda em servidor headless) |

Verifique as versões com:

```bash
java -version
mvn -version
```

---

## Instalação e configuração

### Linux

**Opção A — gerenciador de pacotes (Debian/Ubuntu):**

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git
git clone <URL-REPOSITORIO>
cd SistemaUsuario
mvn clean package
java -jar target/SistemaUsuario.jar
```

**Opção B — SDKMAN (qualquer distribuição):**

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
sdk install maven
git clone <URL-DO-REPOSITORIO>
cd SistemaUsuario
mvn clean package
java -jar target/SistemaUsuario.jar
```

### Windows

**Opção A — winget (PowerShell):**

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
winget install Git.Git
git clone <URL-REPOSITORIO>
cd SistemaUsuario
mvn clean package
java -jar target\SistemaUsuario.jar
```

Feche e reabra o terminal para recarregar o `PATH`. Se o Maven não definir o
`JAVA_HOME` automaticamente, configure-o:

```powershell
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21"
```

**Opção B — instalação manual:**

1. Baixe e instale o JDK 21 (Temurin/Oracle).
2. Baixe o Apache Maven, extraia e adicione a pasta `bin` ao `PATH`.
3. Defina `JAVA_HOME` apontando para a pasta do JDK.

---

## Comandos principais

Todos executados na raiz do projeto (onde está o `pom.xml`):

| Comando | Descrição |
| --- | --- |
| `mvn clean` | Remove artefatos antigos |
| `mvn compile` | Compila o código |
| `mvn test` | Executa testes JUnit |
| `mvn package` | Gera o JAR executável |
| `java -jar target/SistemaUsuario.jar` | Executa o sistema |

---

## Como executar

Após `mvn package`, execute o fat jar:

```bash
java -jar target/SistemaUsuario.jar
```

No primeiro acesso, use as credenciais do administrador criadas automaticamente:

| Login | Senha |
|-------|-------|
| `admin` | `admin` |

> A senha é armazenada em texto simples nesta versão (uso acadêmico). Não use credenciais reais.

---

## Banco de dados

- Engine: **SQLite**, arquivo único em `data/sistemausuario.db`, criado relativo ao diretório de execução.
- O schema é aplicado automaticamente na inicialização por `database/DatabaseMigrator` (a partir de `src/main/resources/db/schema.sql`, com `CREATE TABLE IF NOT EXISTS` — idempotente).
- As chaves estrangeiras são habilitadas por conexão (`PRAGMA foreign_keys = ON` em `DatabaseConnection`), pois o SQLite as desativa por padrão.
- Para reiniciar do zero, basta apagar o arquivo `.db` — ele será recriado e o admin inicial semeado novamente.

---

## Estrutura do projeto

```text
SistemaUsuario/
├── pom.xml
├── README.md
├── data/                          # banco SQLite gerado em runtime (não versionado)
└── src/
    ├── main/
    │   ├── java/br/com/sistemausuario/
    │   │   ├── view/Main.java     # ponto de entrada + wiring de dependências
    │   │   ├── config/            # AppConfig (sessão Singleton)
    │   │   ├── controller/        # regras de negócio
    │   │   ├── dao/               # acesso a dados (JDBC)
    │   │   ├── database/          # conexão e migração
    │   │   ├── exception/         # exceções de domínio
    │   │   ├── model/             # entity, enums, dto, filter
    │   │   ├── security/          # RBAC (PermissoesPerfil) e escopo (EscopoColaborador)
    │   │   └── view/              # telas Swing
    │   └── resources/db/
    │       └── schema.sql         # DDL das tabelas
    └── test/
        └── java/br/com/sistemausuario/  # testes JUnit 5
```
