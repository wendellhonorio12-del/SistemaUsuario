package br.com.sistemausuario.dao;

import br.com.sistemausuario.database.DatabaseConnection;
import br.com.sistemausuario.model.entity.Equipe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de persistência para a tabela {@code equipes}. Fornece operações CRUD
 * completas e verificação de referência cruzada com {@code projeto_equipe},
 * isolando o JDBC das demais camadas da arquitetura MVC.
 */
public class EquipeDao {

    private static final String COLUNAS = "id, nome, descricao";

    private final DatabaseConnection databaseConnection;

    public EquipeDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Persiste uma nova equipe e atualiza o campo {@code id} da entidade.
     *
     * @param equipe entidade a inserir (id ignorado; será preenchido após inserção)
     * @return id gerado pelo banco de dados
     * @throws IllegalStateException em falha de SQL ou se o id não for retornado
     */
    public int inserir(Equipe equipe) {
        String sql = "INSERT INTO equipes (nome, descricao) VALUES (?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             // RETURN_GENERATED_KEYS: recupera o id autoincrement gerado pelo SQLite
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(statement, equipe);
            statement.executeUpdate();
            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    int id = chaves.getInt(1);
                    equipe.setId(id);
                    return id;
                }
                throw new IllegalStateException("Falha ao obter o id gerado da equipe.");
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao inserir equipe.", excecao);
        }
    }

    /**
     * Atualiza nome e descrição da equipe identificada por {@code equipe.getId()}.
     *
     * @param equipe entidade com os novos valores e id existente
     * @throws IllegalStateException em falha de SQL
     */
    public void atualizar(Equipe equipe) {
        String sql = "UPDATE equipes SET nome = ?, descricao = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencher(statement, equipe);
            statement.setInt(3, equipe.getId());
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao atualizar equipe.", excecao);
        }
    }

    /**
     * Remove a equipe pelo id. Não verifica vínculos — use {@link #isReferenciado}
     * antes de chamar este método para evitar violações de integridade referencial.
     *
     * @param id identificador da equipe a remover
     * @throws IllegalStateException em falha de SQL
     */
    public void excluir(int id) {
        String sql = "DELETE FROM equipes WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao excluir equipe.", excecao);
        }
    }

    /**
     * Busca uma equipe pelo id.
     *
     * @param id identificador da equipe
     * @return {@link Optional} contendo a entidade, ou vazio se não encontrada
     * @throws IllegalStateException em falha de SQL
     */
    public Optional<Equipe> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM equipes WHERE id = ?";
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
            throw new IllegalStateException("Erro ao buscar equipe por id.", excecao);
        }
    }

    /**
     * Retorna todas as equipes cadastradas, ordenadas por id.
     *
     * @return lista (possivelmente vazia) de equipes
     * @throws IllegalStateException em falha de SQL
     */
    public List<Equipe> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM equipes ORDER BY id";
        List<Equipe> equipes = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                equipes.add(mapear(resultSet));
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao listar equipes.", excecao);
        }
        return equipes;
    }

    /**
     * Verifica se a equipe possui vínculo ativo com algum projeto na tabela
     * de junção {@code projeto_equipe}. Deve ser consultado antes de excluir
     * a equipe para respeitar a integridade referencial.
     *
     * @param equipeId id da equipe a verificar
     * @return {@code true} se houver ao menos um projeto associado
     * @throws IllegalStateException em falha de SQL
     */
    public boolean isReferenciado(int equipeId) {
        // Consulta a tabela de junção projeto_equipe para detectar dependências
        String sql = "SELECT COUNT(*) FROM projeto_equipe WHERE equipe_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException excecao) {
            throw new IllegalStateException("Erro ao verificar referências da equipe.", excecao);
        }
    }

    private void preencher(PreparedStatement statement, Equipe equipe) throws SQLException {
        statement.setString(1, equipe.getNome());
        statement.setString(2, equipe.getDescricao());
    }

    private Equipe mapear(ResultSet resultSet) throws SQLException {
        return new Equipe(
            resultSet.getInt("id"),
            resultSet.getString("nome"),
            resultSet.getString("descricao")
        );
    }
}
