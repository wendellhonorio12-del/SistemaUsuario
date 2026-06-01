package br.com.sistemausuario.view;

import br.com.sistemausuario.controller.ProjetoController;
import br.com.sistemausuario.model.entity.Equipe;
import br.com.sistemausuario.model.entity.Projeto;
import br.com.sistemausuario.model.entity.Usuario;
import br.com.sistemausuario.model.enums.PerfilUsuario;
import br.com.sistemausuario.model.enums.StatusProjeto;
import br.com.sistemausuario.model.filter.ProjetoFiltro;
import br.com.sistemausuario.security.EscopoColaborador;
import br.com.sistemausuario.util.DateUtils;
import br.com.sistemausuario.util.ValidationUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Tela de consulta de projetos com filtros por nome, status, período de início e equipe.
 * Perfis {@link PerfilUsuario#COLABORADOR} visualizam apenas os projetos cujas equipes
 * possuem vínculo com o usuário autenticado — restrição aplicada via
 * {@link EscopoColaborador} após a consulta ao controller, sem alterar a query do DAO.
 * Integra ao fluxo de navegação como tela de leitura; não permite cadastro nem edição.
 */
public class ConsultaProjetoview extends JFrame {

    private static final Font FONTE_BASE = new Font("SansSerif", Font.PLAIN, 15);
    private static final String FILTRO_TODOS = "Todos";
    private static final String FILTRO_TODAS = "Todas";

    private final transient ProjetoController controller;
    private final transient Usuario usuarioSessao;
    private final transient EscopoColaborador escopo;
    private final boolean colaborador;

    private final JTextField campoNome = new JTextField(20);
    private final JComboBox<Object> comboStatus = new JComboBox<>();
    private final JTextField campoDataDe = new JTextField(10);
    private final JTextField campoDataAte = new JTextField(10);
    private final JComboBox<Object> comboEquipe = new JComboBox<>();

    // DefaultTableModel anônimo: sobrescreve isCellEditable para garantir read-only na tabela.
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[] {"ID", "Nome", "Descrição", "Início", "Término", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(tableModel);

    /**
     * Cria a janela, carrega combos de status e equipes, aplica a fonte base à hierarquia
     * de componentes e dispara uma pesquisa inicial para preencher a tabela.
     *
     * @param controller     controller de projetos; fornece dados de equipes e resultados de consulta
     * @param usuarioSessao  usuário autenticado; determina se o filtro de escopo será ativado
     * @param escopo         serviço que mapeia um colaborador aos seus projetos permitidos
     */
    public ConsultaProjetoview(ProjetoController controller, Usuario usuarioSessao,
                               EscopoColaborador escopo) {
        super("Consulta de Projetos — SistemaUsuario");
        this.controller = controller;
        this.usuarioSessao = usuarioSessao;
        this.escopo = escopo;
        this.colaborador = usuarioSessao.getPerfil() == PerfilUsuario.COLABORADOR;
        carregarCombos();
        configurarJanela();
        pesquisar();
    }

    private void carregarCombos() {
        DefaultComboBoxModel<Object> modeloStatus = new DefaultComboBoxModel<>();
        modeloStatus.addElement(FILTRO_TODOS);
        for (StatusProjeto status : StatusProjeto.values()) {
            modeloStatus.addElement(status);
        }
        comboStatus.setModel(modeloStatus);

        DefaultComboBoxModel<Object> modeloEquipe = new DefaultComboBoxModel<>();
        modeloEquipe.addElement(FILTRO_TODAS);
        for (Equipe equipe : controller.listarEquipes()) {
            modeloEquipe.addElement(equipe);
        }
        comboEquipe.setModel(modeloEquipe);
        // Renderer inline: usa pattern matching (instanceof com binding, Java 16+) para
        // extrair o nome da Equipe sem cast explícito, evitando ClassCastException
        // quando o combo exibe o item "Todas" (String), que não entra no ramo if.
        comboEquipe.setRenderer(new DefaultListCellRenderer() {
            @Override // Usa o recurso de pattern matching do Java 16 para simplificar a renderização
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Equipe equipe) {
                    setText(equipe.getNome());
                }
                return this;
            }
        });
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirFiltros(), BorderLayout.NORTH);
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);

        aplicarFonte(conteudo, FONTE_BASE);
        tabela.setRowHeight(26);
        tabela.getTableHeader().setFont(FONTE_BASE.deriveFont(Font.BOLD));
    }

    /**
     * Monta o painel de filtros com {@link GridBagLayout}: dois GBC reutilizáveis
     * (um para rótulos, outro para campos) têm {@code gridx}/{@code gridy} ajustados
     * a cada célula, evitando instanciar objetos novos por linha.
     */
    private JPanel construirFiltros() {
        JPanel painel = new JPanel(new GridBagLayout());

        // GBC de rótulo: âncora à esquerda, margem direita para separar do campo.
        GridBagConstraints rotulo = new GridBagConstraints();
        rotulo.anchor = GridBagConstraints.WEST;
        rotulo.insets = new Insets(4, 0, 4, 8);

        // GBC de campo: expansão horizontal; weightx variável conforme a coluna.
        GridBagConstraints campo = new GridBagConstraints();
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.insets = new Insets(4, 0, 4, 16);

        int col = 0;
        rotulo.gridx = col;
        rotulo.gridy = 0;
        painel.add(new JLabel("Nome:"), rotulo);
        campo.gridx = ++col;
        campo.gridy = 0;
        campo.weightx = 1.0;
        painel.add(campoNome, campo);

        rotulo.gridx = ++col;
        campo.weightx = 0;
        painel.add(new JLabel("Status:"), rotulo);
        campo.gridx = ++col;
        painel.add(comboStatus, campo);

        rotulo.gridx = 0;
        rotulo.gridy = 1;
        painel.add(new JLabel("Início de:"), rotulo);
        campo.gridx = 1;
        campo.gridy = 1;
        campo.weightx = 1.0;
        painel.add(campoDataDe, campo);

        rotulo.gridx = 2;
        campo.weightx = 0;
        painel.add(new JLabel("até:"), rotulo);
        campo.gridx = 3;
        painel.add(campoDataAte, campo);

        rotulo.gridx = 0;
        rotulo.gridy = 2;
        painel.add(new JLabel("Equipe:"), rotulo);
        campo.gridx = 1;
        campo.gridy = 2;
        painel.add(comboEquipe, campo);

        JButton pesquisar = new JButton("Pesquisar");
        JButton limpar = new JButton("Limpar");
        pesquisar.addActionListener(evento -> pesquisar());
        limpar.addActionListener(evento -> limparFiltros());

        campo.gridx = 2;
        campo.gridy = 2;
        painel.add(pesquisar, campo);
        campo.gridx = 3;
        painel.add(limpar, campo);

        return painel;
    }

    private void pesquisar() {
        try {
            ProjetoFiltro filtro = montarFiltro();
            List<Projeto> resultado = controller.consultar(filtro);
            if (colaborador) {
                // Filtro de escopo pós-consulta: restringe o resultado aos projetos
                // onde o colaborador pertence a pelo menos uma equipe vinculada.
                Set<Integer> permitidos = escopo.projetoIdsDoColaborador(usuarioSessao.getId());
                resultado = resultado.stream()
                    .filter(projeto -> permitidos.contains(projeto.getId()))
                    .toList();
            }
            tableModel.setRowCount(0);
            for (Projeto projeto : resultado) {
                tableModel.addRow(new Object[] {
                    projeto.getId(),
                    projeto.getNome(),
                    projeto.getDescricao(),
                    DateUtils.format(projeto.getDataInicio()),
                    DateUtils.format(projeto.getDataTerminoPrevista()),
                    projeto.getStatus()
                });
            }
        } catch (Exception excecao) {
            JOptionPane.showMessageDialog(this, excecao.getMessage(), "Erro",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Constrói o {@link ProjetoFiltro} a partir dos campos da UI.
     * Campos vazios ou com o sentinela "Todos/Todas" são ignorados (filtro parcial):
     * somente propriedades explicitamente preenchidas são repassadas ao DAO.
     * Pattern matching nos combos evita cast e garante que o item sentinela (String)
     * não poluia o filtro como {@link StatusProjeto} ou {@link Equipe}.
     */
    private ProjetoFiltro montarFiltro() {
        ProjetoFiltro filtro = new ProjetoFiltro();

        String nome = campoNome.getText().trim();
        if (!nome.isEmpty()) {
            filtro.setNome(nome);
        }

        // Pattern matching: item sentinela "Todos" (String) não satisfaz StatusProjeto.
        Object statusSelecionado = comboStatus.getSelectedItem();
        if (statusSelecionado instanceof StatusProjeto status) {
            filtro.setStatus(status);
        }

        // Intervalo de datas só é aplicado quando ambos os extremos estiverem preenchidos.
        String dataDe = campoDataDe.getText().trim();
        String dataAte = campoDataAte.getText().trim();
        if (!ValidationUtils.isBlank(dataDe) && !ValidationUtils.isBlank(dataAte)) {
            filtro.setDataInicioDe(DateUtils.parse(dataDe));
            filtro.setDataInicioAte(DateUtils.parse(dataAte));
        }

        // Item sentinela "Todas" (String) não satisfaz Equipe; equipeId permanece nulo.
        Object equipeSelecionada = comboEquipe.getSelectedItem();
        if (equipeSelecionada instanceof Equipe equipe) {
            filtro.setEquipeId(equipe.getId());
        }

        return filtro;
    }

    private void limparFiltros() {
        campoNome.setText("");
        comboStatus.setSelectedIndex(0);
        campoDataDe.setText("");
        campoDataAte.setText("");
        comboEquipe.setSelectedIndex(0);
        pesquisar();
    }

    /**
     * Aplica {@code fonte} recursivamente a toda a hierarquia de componentes Swing.
     * O ramo {@code instanceof Container} usa pattern matching (Java 16+) para iterar
     * os filhos sem cast explícito; componentes folha (ex.: {@link JLabel}) encerram
     * a recursão naturalmente.
     */
    private void aplicarFonte(Component componente, Font fonte) {
        componente.setFont(fonte);
        if (componente instanceof Container container) {
            for (Component filho : container.getComponents()) {
                aplicarFonte(filho, fonte);
            }
        }
    }
}
