package com.saas.clienthub.service;

import com.saas.clienthub.model.entity.Usuario;
import com.saas.clienthub.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementação customizada do UserDetailsService do Spring Security.
 *
 * =====================================================================
 * CONCEITO: UserDetailsService
 * =====================================================================
 * O Spring Security precisa saber COMO buscar um usuário no banco
 * quando ele faz login. Essa interface é o contrato:
 *
 *   1. Usuário digita email e senha no formulário de login
 *   2. Spring Security chama loadUserByUsername(email)
 *   3. Nós buscamos o Usuario no banco e retornamos um UserDetails
 *   4. Spring Security compara a senha digitada com o hash BCrypt
 *   5. Se bater → autenticado! Se não → erro de login
 *
 * O UserDetails retornado contém:
 *   - username → email do usuário
 *   - password → hash BCrypt da senha (para comparação)
 *   - roles    → papéis do usuário (ADMIN, GESTOR, USUARIO)
 *
 * =====================================================================
 * CONCEITO: Por que não retornamos a entidade Usuario diretamente?
 * =====================================================================
 * O Spring Security trabalha com a interface UserDetails, que tem
 * métodos como isEnabled(), isAccountNonLocked(), etc.
 * Usamos User.builder() para criar um UserDetails compatível.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Método chamado automaticamente pelo Spring Security durante o login.
     *
     * @param email O valor digitado no campo "username" do formulário de login
     *              (no nosso caso, é o email do usuário)
     * @return UserDetails com as credenciais e roles do usuário
     * @throws UsernameNotFoundException se o email não existir ou o usuário estiver inativo
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Busca o usuário pelo email no banco de dados
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // Verifica se o usuário está ativo (soft delete)
        if (!usuario.getAtivo()) {
            throw new UsernameNotFoundException("Usuário desativado: " + email);
        }

        // Constrói o UserDetails que o Spring Security usa para autenticação
        // .roles() adiciona automaticamente o prefixo "ROLE_" (ex: ADMIN → ROLE_ADMIN)
        return User.builder()
                .username(usuario.getEmail())      // email como username
                .password(usuario.getSenha())       // hash BCrypt — NÃO a senha em texto
                .roles(usuario.getRole().name())    // ADMIN, GESTOR ou USUARIO
                .build();
    }
}
