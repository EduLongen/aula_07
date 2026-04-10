package com.saas.clienthub.model.entity;

/**
 * Enum que define os papéis (roles) de acesso dos usuários no sistema.
 *
 * =====================================================================
 * CONCEITO: Role-Based Access Control (RBAC)
 * =====================================================================
 * RBAC é um modelo de controle de acesso onde permissões são associadas
 * a papéis, e usuários recebem papéis. Isso simplifica a gestão de
 * permissões — em vez de configurar acesso por usuário, configuramos por papel.
 *
 * No Spring Security, cada role é prefixada automaticamente com "ROLE_"
 * (ex: ADMIN → ROLE_ADMIN). Usamos hasRole("ADMIN") nas configurações
 * e o Spring adiciona o prefixo internamente.
 */
public enum Role {
    ADMIN,    // Administrador global — acessa tudo, gerencia empresas e usuários
    GESTOR,   // Gestor de empresa — gerencia clientes e produtos da sua empresa
    USUARIO   // Usuário comum — visualiza dados da sua empresa
}
