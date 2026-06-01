package br.com.sistemausuario.security;

/**
 * Recursos (telas/funcionalidades) do sistema sujeitos a controle de acesso
 * por perfil (RBAC). Cada item corresponde a uma área navegável da Tela
 * Principal.
 */
public enum Recurso {
    USUARIOS,
    PROJETOS,
    TAREFAS,
    EQUIPES,
    CONSULTA,
    RELATORIO
}
