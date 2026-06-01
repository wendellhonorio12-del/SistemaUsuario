package br.com.sistemausuario.view;

import br.com.sistemausuario.controller.EquipeController;
import br.com.sistemausuario.controller.LoginController;
import br.com.sistemausuario.controller.ProjetoController;
import br.com.sistemausuario.controller.RelatorioController;
import br.com.sistemausuario.controller.TarefaController;
import br.com.sistemausuario.controller.UsuarioController;
import br.com.sistemausuario.dao.EquipeDao;
import br.com.sistemausuario.dao.EquipeUsuarioDao;
import br.com.sistemausuario.dao.ProjetoDao;
import br.com.sistemausuario.dao.ProjetoEquipeDao;
import br.com.sistemausuario.dao.RelatorioDao;
import br.com.sistemausuario.dao.TarefaDao;
import br.com.sistemausuario.dao.UsuarioDao;
import br.com.sistemausuario.database.DatabaseConnection;
import br.com.sistemausuario.database.DatabaseMigrator;
import br.com.sistemausuario.model.entity.Usuario;
import br.com.sistemausuario.model.enums.PerfilUsuario;
import br.com.sistemausuario.security.EscopoColaborador;

import javax.swing.SwingUtilities;

/**
 * Ponto de entrada da aplicação. Executa a migração inicial do banco, garante
 * um usuário administrador para o primeiro acesso, monta os DAOs e controllers
 * e abre a tela de login, que conduz à tela principal (hub de navegação).
 */
public class Main {

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_CPF = "52998224725";

    public static void main(String[] args) {
        new DatabaseMigrator().migrate();

        DatabaseConnection databaseConnection = new DatabaseConnection();
        UsuarioDao usuarioDao = new UsuarioDao(databaseConnection);
        EquipeDao equipeDao = new EquipeDao(databaseConnection);
        EquipeUsuarioDao equipeUsuarioDao = new EquipeUsuarioDao(databaseConnection);
        ProjetoDao projetoDao = new ProjetoDao(databaseConnection);
        ProjetoEquipeDao projetoEquipeDao = new ProjetoEquipeDao(databaseConnection);
        TarefaDao tarefaDao = new TarefaDao(databaseConnection);
        RelatorioDao relatorioDao = new RelatorioDao(databaseConnection);

        semearAdmin(usuarioDao);

        LoginController loginController = new LoginController(usuarioDao);
        UsuarioController usuarioController = new UsuarioController(usuarioDao);
        EquipeController equipeController =
                new EquipeController(equipeDao, equipeUsuarioDao, usuarioDao);
        ProjetoController projetoController =
                new ProjetoController(projetoDao, projetoEquipeDao, equipeDao);
        EscopoColaborador escopoColaborador =
                new EscopoColaborador(equipeUsuarioDao, projetoEquipeDao, projetoDao);
        TarefaController tarefaController =
                new TarefaController(tarefaDao, projetoDao, usuarioDao);
        RelatorioController relatorioController =
                new RelatorioController(projetoDao, relatorioDao, projetoEquipeDao, equipeDao, usuarioDao);

        SwingUtilities.invokeLater(() -> new LoginView(loginController,
                usuarioController,
                equipeController,
                projetoController,
                escopoColaborador,
                tarefaController,
                relatorioController).setVisible(true));
    }

    private static void semearAdmin(UsuarioDao usuarioDao) {
        if (usuarioDao.existsByLogin(ADMIN_LOGIN) || usuarioDao.existsByCpf(ADMIN_CPF)) {
            return;
        }
        usuarioDao.inserir(new Usuario("Administrador do Sistema", ADMIN_CPF,
                "admin@SistemaUsuario.local", "Administrador", ADMIN_LOGIN, "admin",
                PerfilUsuario.ADMINISTRADOR));
    }
}
