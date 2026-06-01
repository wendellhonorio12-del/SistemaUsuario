package br.com.sistemausuario.dao;

import br.com.sistemausuario.database.DatabaseConnection;
import br.com.sistemausuario.model.entity.Projeto;
import br.com.sistemausuario.model.enums.StatusProjeto;
import br.com.sistemausuario.model.filter.ProjetoFiltro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso a dados da tabela {@code projetos}. Recebe e devolve entidades
 * {@link Projeto}; converte datas {@link LocalDate} para TEXT ISO e o status
 * para TEXT (via {@code name()}). Abre uma conexão por operação.
 */
public class ProjetoDao {

    private static final String COLUNAS =
        "id, nome, descricao, data_inicio, data_termino_prevista, status";

    private final DatabaseConnection databaseConnection;

    public ProjetoDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Persiste um novo projeto e atualiza o campo {@code id} da entidade.
     *
     * @param projeto entidade a inserir (id ignorado; será preenchido após inserção)
     * @return id gerado pelo banco de dados
     * @throws IllegalStateException em falha de SQL ou se o id não for retornado
     */
    public int inserir(Projeto projeto) {
        String sql = "INSERT INTO projetos (nome, descricao, data_inicio, data_termino_prevista, status) "
            + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             // RETURN_GENERATED_KEYS: recupera o id autoincrement gerado pelo SQLite
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(statement, projeto);
            statement.executeUpdate();
            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    int id = chaves.getInt(1);
                    projeto.setId(id);
                    return id;
                }
                throw new IllegalStateException("Falha ao obter o id gerado do projeto.");
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao inserir projeto.", excecao);
        }
    }

    /**
     * Atualiza todos os campos do projeto identificado por {@code projeto.getId()}.
     *
     * @param projeto entidade com os novos valores e id existente
     * @throws IllegalStateException em falha de SQL
     */
    public void atualizar(Projeto projeto) {
        String sql = "UPDATE projetos SET nome = ?, descricao = ?, data_inicio = ?, "
            + "data_termino_prevista = ?, status = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, projeto);
            statement.setInt(6, projeto.getId());
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao atualizar projeto.", excecao);
        }
    }

    /**
     * Remove o projeto pelo id. Verifique {@link #isReferenciado} antes de
     * chamar para evitar dependências órfãs em tarefas e equipes vinculadas.
     *
     * @param id identificador do projeto a remover
     * @throws IllegalStateException em falha de SQL
     */
    public void excluir(int id) {
        String sql = "DELETE FROM projetos WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao excluir projeto.", excecao);
        }
    }

    /**
     * Retorna todos os projetos cadastrados, ordenados por id.
     *
     * @return lista (possivelmente vazia) de projetos
     * @throws IllegalStateException em falha de SQL
     */
    public List<Projeto> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM projetos ORDER BY id";
        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                projetos.add(mapear(resultSet));
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao listar projetos.", excecao);
        }
        return projetos;
    }

    /**
     * Busca um projeto pelo id.
     *
     * @param id identificador do projeto
     * @return {@link Optional} contendo a entidade, ou vazio se não encontrado
     * @throws IllegalStateException em falha de SQL
     */
    public Optional<Projeto> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM projetos WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapear(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao buscar projeto por id.", excecao);
        }
    }

    /**
     * Lista projetos filtrando pelo status informado. O enum é persistido como
     * TEXT via {@code name()}.
     *
     * @param status status desejado (ex.: {@code ATIVO}, {@code CONCLUIDO})
     * @return lista (possivelmente vazia) de projetos com esse status
     * @throws IllegalStateException em falha de SQL
     */
    public List<Projeto> findByStatus(StatusProjeto status) {
        String sql = "SELECT " + COLUNAS + " FROM projetos WHERE status = ? ORDER BY id";
        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projetos.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao listar projetos por status.", excecao);
        }
        return projetos;
    }

    /**
     * Executa consulta dinâmica com filtros opcionais: nome (LIKE case-insensitive),
     * status, intervalo de data de início e equipe associada via subquery em
     * {@code projeto_equipe}. Parâmetros nulos são ignorados na construção da query.
     *
     * @param filtro critérios de busca; {@code null} equivale a {@link #listarTodos()}
     * @return lista de projetos que satisfazem todos os filtros informados
     * @throws IllegalStateException em falha de SQL
     */
    public List<Projeto> consultar(ProjetoFiltro filtro) {
        if (filtro == null) {
            return listarTodos();
        }
        // SQL construído dinamicamente com cláusulas AND condicionais por filtro ativo
        StringBuilder sql = new StringBuilder("SELECT " + COLUNAS + " FROM projetos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filtro.getNome() != null && !filtro.getNome().isBlank()) {
            sql.append(" AND LOWER(nome) LIKE LOWER(?)");
            params.add("%" + filtro.getNome().trim() + "%");
        }
        if (filtro.getStatus() != null) {
            sql.append(" AND status = ?");
            params.add(filtro.getStatus().name());
        }
        if (filtro.getDataInicioDe() != null && filtro.getDataInicioAte() != null) {
            sql.append(" AND data_inicio BETWEEN ? AND ?");
            params.add(filtro.getDataInicioDe().toString());
            params.add(filtro.getDataInicioAte().toString());
        }
        if (filtro.getEquipeId() != null) {
            sql.append(" AND id IN (SELECT projeto_id FROM projeto_equipe WHERE equipe_id = ?)");
            params.add(filtro.getEquipeId());
        }
        sql.append(" ORDER BY id");

        List<Projeto> projetos = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projetos.add(mapear(resultSet));
                }
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao consultar projetos.", excecao);
        }
        return projetos;
    }

    /**
     * Verifica se o projeto possui tarefas ou equipes vinculadas, consultando
     * {@code tarefas} e a tabela de junção {@code projeto_equipe} em uma única
     * query com dois subselects somados.
     *
     * @param projetoId id do projeto a verificar
     * @return {@code true} se houver ao menos uma dependência
     * @throws IllegalStateException em falha de SQL
     */
    public boolean isReferenciado(int projetoId) {
        // Soma contagens de tarefas e vínculos de equipe para detectar dependências
        String sql = "SELECT "
            + "(SELECT COUNT(*) FROM tarefas WHERE projeto_id = ?) + "
            + "(SELECT COUNT(*) FROM projeto_equipe WHERE projeto_id = ?) AS total";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projetoId);
            statement.setInt(2, projetoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("total") > 0;
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao verificar referências do projeto.", excecao);
        }
    }

    private void preencher(PreparedStatement statement, Projeto projeto) throws SQLException {
        statement.setString(1, projeto.getNome());
        statement.setString(2, projeto.getDescricao());
        statement.setString(3, projeto.getDataInicio() != null ? projeto.getDataInicio().toString() : null);
        statement.setString(4, projeto.getDataTerminoPrevista() != null
            ? projeto.getDataTerminoPrevista().toString() : null);
        statement.setString(5, projeto.getStatus() != null ? projeto.getStatus().name() : null);
    }

    private Projeto mapear(ResultSet resultSet) throws SQLException {
        return new Projeto(
            resultSet.getInt("id"),
            resultSet.getString("nome"),
            resultSet.getString("descricao"),
            LocalDate.parse(resultSet.getString("data_inicio")),
            LocalDate.parse(resultSet.getString("data_termino_prevista")),
            StatusProjeto.valueOf(resultSet.getString("status"))
        );
    }
}
