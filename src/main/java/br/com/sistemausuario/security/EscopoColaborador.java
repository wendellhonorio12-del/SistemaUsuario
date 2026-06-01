package br.com.sistemausuario.security;

import br.com.sistemausuario.dao.EquipeUsuarioDao;
import br.com.sistemausuario.dao.ProjetoDao;
import br.com.sistemausuario.dao.ProjetoEquipeDao;
import br.com.sistemausuario.model.entity.Projeto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolve o escopo de dados visível a um Colaborador: os projetos das equipes
 * das quais ele participa (PRD §11.2). Reusa os DAOs de vínculo
 * (equipe_usuario, projeto_equipe) e o {@link ProjetoDao} para hidratar as
 * entidades.
 *
 * <p>Isolado da camada de view para ser testável sem Swing; reusado por
 * Consulta e Relatório.
 */
public class EscopoColaborador {

    private final EquipeUsuarioDao equipeUsuarioDao;
    private final ProjetoEquipeDao projetoEquipeDao;
    private final ProjetoDao projetoDao;

    public EscopoColaborador(EquipeUsuarioDao equipeUsuarioDao,
                             ProjetoEquipeDao projetoEquipeDao,
                             ProjetoDao projetoDao) {
        this.equipeUsuarioDao = equipeUsuarioDao;
        this.projetoEquipeDao = projetoEquipeDao;
        this.projetoDao = projetoDao;
    }

    /**
     * IDs dos projetos vinculados às equipes do colaborador, sem duplicatas.
     * Útil quando apenas os identificadores bastam (ex.: filtro de consulta).
     */
    public Set<Integer> projetoIdsDoColaborador(int usuarioId) {
        Set<Integer> projetoIds = new LinkedHashSet<>();
        for (int equipeId : equipeUsuarioDao.listarEquipeIdsDoUsuario(usuarioId)) {
            projetoIds.addAll(projetoEquipeDao.listarProjetoIds(equipeId));
        }
        return projetoIds;
    }

    /**
     * Projetos do escopo do colaborador, ordenados por id. Resolve o conjunto
     * de ids permitidos e filtra a listagem de projetos numa única consulta,
     * evitando uma busca por id para cada projeto.
     */
    public List<Projeto> projetosDoColaborador(int usuarioId) {
        Set<Integer> ids = projetoIdsDoColaborador(usuarioId);
        List<Projeto> projetos = new ArrayList<>();
        if (ids.isEmpty()) {
            return projetos;
        }
        for (Projeto projeto : projetoDao.listarTodos()) {
            if (ids.contains(projeto.getId())) {
                projetos.add(projeto);
            }
        }
        return projetos;
    }
}
