package br.com.sistemausuario.view;

import br.com.sistemausuario.config.AppConfig;
import br.com.sistemausuario.controller.EquipeController;
import br.com.sistemausuario.controller.LoginController;
import br.com.sistemausuario.controller.ProjetoController;
import br.com.sistemausuario.controller.RelatorioController;
import br.com.sistemausuario.controller.TarefaController;
import br.com.sistemausuario.controller.UsuarioController;
import br.com.sistemausuario.exception.AutenticacaoException;
import br.com.sistemausuario.model.entity.Usuario;
import br.com.sistemausuario.security.EscopoColaborador;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.*;

public class LoginView extends JFrame {

    private final transient LoginController controller;
    private final transient UsuarioController usuarioController;
    private final transient EquipeController equipeController;
    private final transient ProjetoController projetoController;
    private final transient EscopoColaborador escopoColaborador;
    private final transient TarefaController tarefaController;
    private final transient RelatorioController relatorioController;
    private final JTextField loginField = new JTextField(18);
    private final JPasswordField senhaField = new JPasswordField(18);

    public LoginView(LoginController controller,
                     UsuarioController usuarioController,
                     EquipeController equipeController,
                     ProjetoController projetoController,
                     EscopoColaborador escopoColaborador,
                     TarefaController tarefaController,
                     RelatorioController relatorioController) {
        super("SistemaUsuario — Login");
        this.controller = controller;
        this.usuarioController = usuarioController;
        this.equipeController = equipeController;
        this.projetoController = projetoController;
        this.escopoColaborador = escopoColaborador;
        this.tarefaController = tarefaController;
        this.relatorioController = relatorioController;
        configurarJanela();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(360, 200);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Login:"), gbc);
        gbc.gridx = 1;
        add(loginField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        add(senhaField, gbc);

        JButton entrar = new JButton("Entrar");
        entrar.addActionListener(evento -> autenticar());
        senhaField.addActionListener(evento -> autenticar());

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(entrar, gbc);
    }

    private void autenticar() {
        String login = loginField.getText();
        char[] senhaChars = senhaField.getPassword();
        String senha = new String(senhaChars);
        try {
            Usuario usuario = controller.autenticar(login, senha);
            senhaField.setText("");
            abrirPrincipal(usuario);
        } catch (AutenticacaoException excecao) {
            JOptionPane.showMessageDialog(this, excecao.getMessage(),
                    "Falha na autenticação", JOptionPane.ERROR_MESSAGE);
            senhaField.setText("");
            senhaField.requestFocusInWindow();
        } catch (RuntimeException excecao) {
            JOptionPane.showMessageDialog(this,
                    "Erro inesperado ao autenticar. Tente novamente.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            senhaField.setText("");
            senhaField.requestFocusInWindow();
        } finally {
            Arrays.fill(senhaChars, '\0');
        }
    }

    private void abrirPrincipal(Usuario usuario) {
        setVisible(false);
        new MainView(usuario, this::voltarParaLogin,
                usuarioController,
                equipeController,
                projetoController,
                escopoColaborador,
                tarefaController,
                relatorioController)
                .setVisible(true);
    }

    private void voltarParaLogin() {
        AppConfig.getInstance().limpar();
        loginField.setText("");
        senhaField.setText("");
        setVisible(true);
        loginField.requestFocusInWindow();
    }
}

