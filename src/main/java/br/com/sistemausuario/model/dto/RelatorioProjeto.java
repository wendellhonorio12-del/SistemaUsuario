package br.com.sistemausuario.model.dto;

import br.com.sistemausuario.model.entity.Equipe;
import br.com.sistemausuario.model.entity.Projeto;
import br.com.sistemausuario.model.entity.Usuario;
import java.util.List;

public class RelatorioProjeto {

    private final Projeto projeto;
    private final List<Equipe> equipes;
    private final List<Usuario> responsaveis;
    private final int totalTarefas;
    private final int tarefasConcluidas;
    private final int tarefasPendentes;
    private final double percentualConclusao;

    public RelatorioProjeto(Projeto projeto,
                            List<Equipe> equipes,
                            List<Usuario> responsaveis,
                            int totalTarefas,
                            int tarefasConcluidas,
                            int tarefasPendentes,
                            double percentualConclusao) {
        this.projeto = projeto;
        this.equipes = equipes;
        this.responsaveis = responsaveis;
        this.totalTarefas = totalTarefas;
        this.tarefasConcluidas = tarefasConcluidas;
        this.tarefasPendentes = tarefasPendentes;
        this.percentualConclusao = percentualConclusao;
    }

    public Projeto getProjeto()            { return projeto; }
    public List<Equipe> getEquipes()       { return equipes; }
    public List<Usuario> getResponsaveis() { return responsaveis; }
    public int getTotalTarefas()           { return totalTarefas; }
    public int getTarefasConcluidas()      { return tarefasConcluidas; }
    public int getTarefasPendentes()       { return tarefasPendentes; }
    public double getPercentualConclusao() { return percentualConclusao; }
    public boolean isSemTarefas()          { return totalTarefas == 0; }
}

