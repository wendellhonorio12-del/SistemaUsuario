package br.com.sistemausuario.controller;

import br.com.sistemausuario.dao.EquipeDao;
import br.com.sistemausuario.dao.ProjetoDao;
import br.com.sistemausuario.dao.ProjetoEquipeDao;
import br.com.sistemausuario.dao.RelatorioDao;
import br.com.sistemausuario.dao.UsuarioDao;
import br.com.sistemausuario.model.dto.RelatorioProjeto;
import br.com.sistemausuario.model.entity.Projeto;
import java.util.List;

public class RelatorioController {

    private final ProjetoDao projetoDao;
    private final RelatorioDao relatorioDao;
    private final ProjetoEquipeDao projetoEquipeDao;
    private final EquipeDao equipeDao;
    private final UsuarioDao usuarioDao;

    public RelatorioController(ProjetoDao projetoDao,
                               RelatorioDao relatorioDao,
                               ProjetoEquipeDao projetoEquipeDao,
                               EquipeDao equipeDao,
                               UsuarioDao usuarioDao) {
        this.projetoDao = projetoDao;
        this.relatorioDao = relatorioDao;
        this.projetoEquipeDao = projetoEquipeDao;
        this.equipeDao = equipeDao;
        this.usuarioDao = usuarioDao;
    }

    public List<Projeto> listarProjetos() {
        return projetoDao.listarTodos();
    }

    public RelatorioProjeto gerar(int projetoId) {
        return relatorioDao.gerarRelatorio(projetoId, projetoDao, projetoEquipeDao, equipeDao, usuarioDao);
    }
}
