package br.com.sistemausuario.view;
import br.com.sistemausuario.controller.UsuarioController;
import br.com.sistemausuario.exception.ExclusaoBloqueadaException;
import br.com.sistemausuario.exception.RegistroDuplicadoException;
import br.com.sistemausuario.exception.ValidacaoException;
import br.com.sistemausuario.model.entity.Usuario;
import br.com.sistemausuario.model.enums.PerfilUsuario;

import java.awt.BorderLayout;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;

/**
 * Tela de cadastro de usuários do SistemaUsuario.
 */
public class UsuarioView extends JFrame {

    private static final Font FONTE_BASE = new Font("Verdana Bold", Font.PLAIN, 15);

    private final transient UsuarioController controller;

    private final JFormattedTextField campoCpf;
    private final JTextField campoNome = new JTextField(23);
    private final JTextField campoEmail = new JTextField(24);
    private final JTextField campoCargo = new JTextField(24);
    private final JTextField campoLogin = new JTextField(24);
    private final JPasswordField campoSenha = new JPasswordField(24);
    private final JComboBox<PerfilUsuario> campoPerfil = new JComboBox<>(PerfilUsuario.values());

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"ID", "CPF", "Nome", "E-mail", "Cargo", "Login", "Perfil"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(tableModel);

    private Integer usuarioSelecionadoId;

    public UsuarioView(UsuarioController controller) {
        super("Cadastro de Usuários — SistemaUsuario");
        this.controller = controller;
        try {
            MaskFormatter cpfMask = new MaskFormatter("###.###.###-##");
            cpfMask.setPlaceholderCharacter('_');
            campoCpf = new JFormattedTextField(cpfMask);
            campoCpf.setColumns(14);
        } catch (ParseException e) {
            throw new RuntimeException("Erro ao criar máscara de CPF", e);
        }
        configurarJanela();
        recarregarTabela();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirFormulario(), BorderLayout.NORTH);
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);
        conteudo.add(construirBotoes(), BorderLayout.SOUTH);

        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                carregarSelecionado();
            }
        });

        aplicarFonte(conteudo, FONTE_BASE);
        tabela.setRowHeight(26);
        tabela.getTableHeader().setFont(new Font("Verdana", Font.BOLD, 14));
    }

    private void aplicarFonte(JPanel conteudo, Font fonteBase) {

    }

    private JPanel construirFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());

        GridBagConstraints rotulo = new GridBagConstraints();
        rotulo.gridx = 0;
        rotulo.anchor = GridBagConstraints.WEST;
        rotulo.insets = new Insets(6, 0, 6, 12);

        GridBagConstraints campo = new GridBagConstraints();
        campo.gridx = 1;
        campo.weightx = 1.0;
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(6, 0, 6, 0);

        int linha = 0;
        adicionarLinha(painel, rotulo, campo, linha++, "CPF:", campoCpf);
        adicionarLinha(painel, rotulo, campo, linha++, "Nome", campoNome);
        adicionarLinha(painel, rotulo, campo, linha++, "E-mail:", campoEmail);
        adicionarLinha(painel, rotulo, campo, linha++, "Cargo:", campoCargo);
        adicionarLinha(painel, rotulo, campo, linha++, "Login:", campoLogin);
        adicionarLinha(painel, rotulo, campo, linha++, "Senha:", campoSenha);
        adicionarLinha(painel, rotulo, campo, linha, "Perfil:", campoPerfil);
        return painel;
    }

    private void adicionarLinha(JPanel painel, GridBagConstraints rotulo, GridBagConstraints campo,
                                int linha, String texto, JComponent componente) {
        rotulo.gridy = linha;
        campo.gridy = linha;
        painel.add(new JLabel(texto), rotulo);
        painel.add(componente, campo);
    }

    private JPanel construirBotoes() {
        JPanel painel = new JPanel();
        JButton novo = new JButton("Novo");
        JButton salvar = new JButton("Salvar");
        JButton excluir = new JButton("Excluir");
        JButton editar = new JButton("Editar");
        novo.addActionListener(evento -> limparFormulario());
        salvar.addActionListener(evento -> salvar());
        excluir.addActionListener(evento -> excluir());
        editar.addActionListener(evento -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) {
                exibirErro("Selecione um usuário na tabela para editar.");
                return;
            }
            carregarSelecionado();
            exibirInformacao("Altere os dados e clique em Salvar para confirmar.");
        });
        painel.add(novo);
        painel.add(salvar);
        painel.add(excluir);
        painel.add(editar);
        return painel;
    }

    private void salvar() {
        try {
            String cpfDigitado = campoCpf.getText();
            cpfDigitado = cpfDigitado.replaceAll("[^\\d]", ""); // remove tudo que não for número
            if (!ValidadorCPF.validar(cpfDigitado)) {
                exibirErro("CPF inválido! Verifique e tente novamente.");
                return;
            }



            Usuario usuario = lerFormulario();
            if (usuarioSelecionadoId == null) {
                controller.salvar(usuario);
                exibirInformacao("Usuário cadastrado com sucesso.");
            } else {
                usuario.setId(usuarioSelecionadoId);
                controller.atualizar(usuario);
                exibirInformacao("Usuário atualizado com sucesso.");
            }
            limparFormulario();
            recarregarTabela();
        } catch (ValidacaoException | RegistroDuplicadoException excecao) {
            exibirErro(excecao.getMessage());
        }
    }

    private void excluir() {
        if (usuarioSelecionadoId == null) {
            exibirErro("Selecione um usuário na tabela para excluir.");
            return;
        }
        int opcao = JOptionPane.showConfirmDialog(this,
                "Confirma a exclusão do usuário selecionado?", "Excluir", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.excluir(usuarioSelecionadoId);
            exibirInformacao("Usuário excluído com sucesso.");
            limparFormulario();
            recarregarTabela();
        } catch (ExclusaoBloqueadaException excecao) {
            exibirErro(excecao.getMessage());
        }
    }

    private Usuario lerFormulario() {
        return new Usuario(
                campoCpf.getText().trim(),
                campoNome.getText().trim(),
                campoEmail.getText().trim(),
                campoCargo.getText().trim(),
                campoLogin.getText().trim(),
                new String(campoSenha.getPassword()),
                (PerfilUsuario) campoPerfil.getSelectedItem()
        );
    }

    private void carregarSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(linha, 0);
        controller.buscarPorId(id).ifPresent(usuario -> {
            usuarioSelecionadoId = usuario.getId();
            campoCpf.setText(ValidadorCPF.formatar(usuario.getCpf()));
            campoNome.setText(usuario.getNomeCompleto());
            campoEmail.setText(usuario.getEmail());
            campoCargo.setText(usuario.getCargo());
            campoLogin.setText(usuario.getLogin());
            campoSenha.setText(usuario.getSenha());
            campoPerfil.setSelectedItem(usuario.getPerfil());
        });
    }

    private void recarregarTabela() {
        tableModel.setRowCount(0);
        List<Usuario> usuarios = controller.listarTodos();
        for (Usuario usuario : usuarios) {
            tableModel.addRow(new Object[] {
                    usuario.getId(),
                    usuario.getCpf(),
                    usuario.getNomeCompleto(),
                    usuario.getEmail(),
                    usuario.getCargo(),
                    usuario.getLogin(),
                    usuario.getPerfil()
            });
        }
    }

    private void limparFormulario() {
        usuarioSelecionadoId = null;
        tabela.clearSelection();
        campoNome.setText("");
        campoCpf.setText("");
        campoEmail.setText("");
        campoCargo.setText("");
        campoLogin.setText("");
        campoSenha.setText("");
        campoPerfil.setSelectedIndex(0);
    }

    private void exibirInformacao(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Informação", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exibirErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}   // ← fecha a classe UsuarioView
