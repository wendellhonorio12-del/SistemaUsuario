package br.com.sistemausuario.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Centraliza a criação de conexões com o banco SQLite e habilita chaves estrangeiras.
 *
 * <p>O construtor sem argumentos usa o banco padrão da aplicação
 * ({@code jdbc:sqlite:data/sistemausuario.db}). O construtor com URL permite apontar para
 * um banco alternativo (por exemplo, um arquivo temporário em testes).
 */
public class DatabaseConnection {

    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:data/sistemausuario.db";

    private final String databaseUrl;

    public DatabaseConnection() {
        this(DEFAULT_DATABASE_URL);
    }

    public DatabaseConnection(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    /**
     * Abre e retorna uma nova conexão JDBC com o banco SQLite configurado.
     *
     * <p>Imediatamente após abrir a conexão, executa {@code PRAGMA foreign_keys = ON}
     * porque o SQLite desabilita a verificação de chaves estrangeiras por padrão;
     * sem esse PRAGMA, violações de FK passariam silenciosamente.
     * O chamador é responsável por fechar a conexão (use try-with-resources).
     *
     * @return conexão ativa com suporte a chaves estrangeiras habilitado
     * @throws IllegalStateException se a conexão JDBC falhar
     */
    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(databaseUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erro ao conectar ao banco SQLite.", exception);
        }
    }
}
