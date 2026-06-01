package br.com.sistemausuario.view;

import br.com.sistemausuario.controller.RelatorioController;
import br.com.sistemausuario.model.dto.RelatorioProjeto;
import br.com.sistemausuario.model.entity.Equipe;
import br.com.sistemausuario.model.entity.Projeto;
import br.com.sistemausuario.model.entity.Usuario;
import br.com.sistemausuario.model.enums.PerfilUsuario;
import br.com.sistemausuario.security.EscopoColaborador;
import br.com.sistemausuario.util.DateUtils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
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
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class RelatorioView extends JFrame {

    private static final Font FONTE_BASE = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font FONTE_KPI = new Font("SansSerif", Font.BOLD, 36);

    private final transient RelatorioController controller;
    private final transient Usuario usuarioSessao;
    private final transient EscopoColaborador escopo;
    private final boolean colaborador;

    private final JComboBox<Projeto> comboProjeto = new JComboBox<>();
    private final JLabel labelNome = new JLabel("-");
    private final JLabel labelStatus = new JLabel("-");
    private final JLabel labelInicio = new JLabel("-");
    private final JLabel labelTermino = new JLabel("-");
    private final JLabel labelPercentual = new JLabel("0,0%");
    private final JLabel labelContagens = new JLabel("Total: 0 | Concluídas: 0 | Pendentes: 0");
    private final JLabel labelMensagem = new JLabel(" ");
    private final JTextArea areaEquipes = new JTextArea(4, 30);
    private final JTextArea areaResponsaveis = new JTextArea(4, 30);

    public RelatorioView(RelatorioController controller, Usuario usuarioSessao,
                         EscopoColaborador escopo) {
        super("Relatório de Desempenho — SistemaUsuario");
        this.controller = controller;
        this.usuarioSessao = usuarioSessao;
        this.escopo = escopo;
        this.colaborador = usuarioSessao.getPerfil() == PerfilUsuario.COLABORADOR;
        carregarComboProjetos();
        configurarJanela();
    }

    private void carregarComboProjetos() {
        DefaultComboBoxModel<Projeto> modelo = new DefaultComboBoxModel<>();
        List<Projeto> projetos = colaborador
                ? escopo.projetosDoColaborador(usuarioSessao.getId())
                : controller.listarProjetos();
        for (Projeto p : projetos) {
            modelo.addElement(p);
        }
        comboProjeto.setModel(modelo);
        comboProjeto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Projeto projeto) {
                    setText(projeto.getNome());
                }
                return this;
            }
        });
    }

    private void configurarJanela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 620);
        setLocationRelativeTo(null);

        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(conteudo);

        conteudo.add(construirTopo(), BorderLayout.NORTH);
        conteudo.add(construirCentro(), BorderLayout.CENTER);

        aplicarFonte(conteudo, FONTE_BASE);
        labelPercentual.setFont(FONTE_KPI);
    }

    private JPanel construirTopo() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painel.add(new JLabel("Projeto:"));
        painel.add(comboProjeto);
        JButton gerar = new JButton("Gerar Relatório");
        gerar.addActionListener(evento -> gerarRelatorio());
        painel.add(gerar);
        return painel;
    }

    private JPanel construirCentro() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));

        JPanel dadosBasicos = new JPanel(new GridLayout(4, 2, 8, 4));
        dadosBasicos.setBorder(BorderFactory.createTitledBorder("Dados do Projeto"));
        dadosBasicos.add(new JLabel("Nome:")); dadosBasicos.add(labelNome);
        dadosBasicos.add(new JLabel("Status:")); dadosBasicos.add(labelStatus);
        dadosBasicos.add(new JLabel("Início:")); dadosBasicos.add(labelInicio);
        dadosBasicos.add(new JLabel("Término previsto:")); dadosBasicos.add(labelTermino);

        JPanel kpi = new JPanel(new BorderLayout());
        kpi.setBorder(BorderFactory.createTitledBorder("Conclusão"));
        labelPercentual.setHorizontalAlignment(SwingConstants.CENTER);
        kpi.add(labelPercentual, BorderLayout.CENTER);
        labelContagens.setHorizontalAlignment(SwingConstants.CENTER);
        kpi.add(labelContagens, BorderLayout.SOUTH);
        labelMensagem.setHorizontalAlignment(SwingConstants.CENTER);
        kpi.add(labelMensagem, BorderLayout.NORTH);

        JPanel topo = new JPanel(new BorderLayout(8, 8));
        topo.add(dadosBasicos, BorderLayout.NORTH);
        topo.add(kpi, BorderLayout.CENTER);

        areaEquipes.setEditable(false);
        areaResponsaveis.setEditable(false);

        JPanel listas = new JPanel(new GridLayout(1, 2, 8, 0));
        JPanel painelEquipes = new JPanel(new BorderLayout());
        painelEquipes.setBorder(BorderFactory.createTitledBorder("Equipes Vinculadas"));
        painelEquipes.add(new JScrollPane(areaEquipes));
        JPanel painelResp = new JPanel(new BorderLayout());
        painelResp.setBorder(BorderFactory.createTitledBorder("Responsáveis Envolvidos"));
        painelResp.add(new JScrollPane(areaResponsaveis));
        listas.add(painelEquipes);
        listas.add(painelResp);

        painel.add(topo, BorderLayout.NORTH);
        painel.add(listas, BorderLayout.CENTER);
        return painel;
    }

    private void gerarRelatorio() {
        Projeto selecionado = (Projeto) comboProjeto.getSelectedItem();
        if (selecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            RelatorioProjeto r = controller.gerar(selecionado.getId());
            Projeto p = r.getProjeto();
            labelNome.setText(p.getNome());
            labelStatus.setText(p.getStatus().name());
            labelInicio.setText(DateUtils.format(p.getDataInicio()));
            labelTermino.setText(DateUtils.format(p.getDataTerminoPrevista()));
            labelPercentual.setText(String.format("%.1f%%", r.getPercentualConclusao()));
            labelContagens.setText(String.format("Total: %d | Concluídas: %d | Pendentes: %d",
                    r.getTotalTarefas(), r.getTarefasConcluidas(), r.getTarefasPendentes()));
            labelMensagem.setText(r.isSemTarefas() ? "Projeto sem tarefas cadastradas" : " ");

            StringBuilder equipesTxt = new StringBuilder();
            for (Equipe e : r.getEquipes()) {
                equipesTxt.append("• ").append(e.getNome()).append("\n");
            }
            areaEquipes.setText(equipesTxt.length() > 0 ? equipesTxt.toString() : "Nenhuma equipe vinculada");

            StringBuilder respTxt = new StringBuilder();
            for (Usuario u : r.getResponsaveis()) {
                respTxt.append("• ").append(u.getNomeCompleto()).append("\n");
            }
            areaResponsaveis.setText(respTxt.length() > 0 ? respTxt.toString() : "Nenhum responsável");
        } catch (Exception excecao) {
            JOptionPane.showMessageDialog(this, excecao.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFonte(Component componente, Font fonte) {
        componente.setFont(fonte);
        if (componente instanceof Container container) {
            for (Component filho : container.getComponents()) {
                aplicarFonte(filho, fonte);
            }
        }
    }
}

