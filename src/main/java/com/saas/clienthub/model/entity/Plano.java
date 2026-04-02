package com.saas.clienthub.model.entity;

/**
 * Enum que representa os planos de assinatura disponíveis na plataforma SaaS.
 *
 * Um enum é uma forma de definir um conjunto fixo de constantes.
 * Em vez de usar Strings soltas como "basico", "profissional", "enterprise"
 * (que são propensas a erros de digitação), usamos um tipo que o compilador
 * conhece e valida em tempo de compilação.
 *
 * No banco de dados, o valor será salvo como texto (ex: "BASICO")
 * graças à anotação @Enumerated(EnumType.STRING) na entidade Empresa.
 */
public enum Plano {
    BASICO,       // Plano de entrada — recursos limitados
    PROFISSIONAL, // Plano intermediário
    ENTERPRISE    // Plano completo — todos os recursos
}
