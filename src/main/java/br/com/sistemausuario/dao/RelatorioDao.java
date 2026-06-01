package br.com.sistemausuario.dao;

import br.com.sistemausuario.database.DatabaseConnection;
import br.com.sistemausuario.model.dto.RelatorioProjeto;
import br.com.sistemausuario.model.entity.Equipe;
import br.com.sistemausuario.model.entity.Projeto;
import br.com.sistemausuario.model.entity.Usuario;
import java.util.ArrayList;
import java.util.List;

public class RelatorioDao {

    private final DatabaseConnection databaseConnection;

    public RelatorioDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public int contarTarefasPorProjeto(int projetoId) {
        // TODO: implementar consulta real no banco
        return 0;
    }

    public int contarConcluidasPorProjeto(int projetoId) {
        // TODO: implementar consulta real no banco
        return 0;
    }

    public int contarPendentesPorProjeto(int projetoId) {
        // TODO: implementar consulta real no banco
        return 0;
    }

    public List<Integer> listarResponsavelIds(int projetoId) {
        // TODO: implementar consulta real no banco
        return new ArrayList<>();
    }

    /**
     * Gera o relatório consolidado de um projeto.
     *
     * @param projetoId ID do projeto
     * @param projetoDao DAO para buscar o projeto
     * @param projetoEquipeDao DAO para equipes ligadas ao projeto
     * @param equipeDao DAO para dados de equipe
     * @param usuarioDao DAO para dados de usuários
     * @return RelatorioProjeto DTO com os dados consolidados
     */
    public RelatorioProjeto gerarRelatorio(int projetoId,
                                           ProjetoDao projetoDao,
                                           ProjetoEquipeDao projetoEquipeDao,
                                           EquipeDao equipeDao,
                                           UsuarioDao usuarioDao) {
        Projeto projeto = projetoDao.buscarPorId(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + projetoId));

        int total = contarTarefasPorProjeto(projetoId);
        int concluidas = contarConcluidasPorProjeto(projetoId);
        int pendentes = contarPendentesPorProjeto(projetoId);
        double percentual = total == 0 ? 0.0 : (concluidas * 100.0) / total;

        List<Integer> equipeIds = projetoEquipeDao.listarEquipeIds(projetoId);
        List<Equipe> equipes = new ArrayList<>();
        for (int equipeId : equipeIds) {
            equipeDao.buscarPorId(equipeId).ifPresent(equipes::add);
        }

        List<Usuario> usuarios = usuarioDao.listarTodos();

        return new RelatorioProjeto(projeto, equipes, usuarios, total, concluidas, pendentes, percentual);
    }
}