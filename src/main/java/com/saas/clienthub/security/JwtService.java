package com.saas.clienthub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service responsável por gerar e validar JSON Web Tokens (JWT).
 *
 * =====================================================================
 * CONCEITO: O que é JWT?
 * =====================================================================
 * JWT (JSON Web Token) é um padrão aberto (RFC 7519) para transmitir
 * informações de forma segura entre partes como um objeto JSON assinado.
 *
 * Um token JWT tem 3 partes separadas por pontos:
 *   HEADER.PAYLOAD.SIGNATURE
 *
 *   Header:    {"alg": "HS256", "typ": "JWT"}
 *   Payload:   {"sub": "admin@clienthub.com", "role": "ADMIN", "exp": 1234567890}
 *   Signature: HMACSHA256(base64(header) + "." + base64(payload), secretKey)
 *
 * O token é assinado com uma chave secreta (HMAC-SHA256).
 * Qualquer alteração no payload invalida a assinatura.
 *
 * =====================================================================
 * CONCEITO: JWT vs Sessão
 * =====================================================================
 * Sessão (cookie):
 *   - Estado no servidor (memória/redis)
 *   - Cookie JSESSIONID no browser
 *   - Bom para aplicações web tradicionais (Thymeleaf)
 *
 * JWT (token):
 *   - Stateless — o servidor não guarda estado
 *   - Token no header Authorization: Bearer xxx
 *   - Bom para APIs REST consumidas por apps, SPAs, microserviços
 *
 * No ClientHub usamos AMBOS:
 *   - Sessão → para o frontend Thymeleaf (páginas HTML)
 *   - JWT → para a API REST (/api/**)
 *
 * =====================================================================
 * CONCEITO: @Value
 * =====================================================================
 * @Value injeta valores do application.properties nas variáveis.
 * ${jwt.secret} → lê a propriedade jwt.secret do properties.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    /**
     * Construtor que inicializa a chave secreta a partir do properties.
     *
     * Keys.hmacShaKeyFor() cria uma SecretKey compatível com HMAC-SHA256
     * a partir dos bytes da string. A chave deve ter >= 256 bits (32 bytes).
     */
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Gera um token JWT para o usuário autenticado.
     *
     * O token contém:
     *   - subject: email do usuário (identificador único)
     *   - claim "role": papel do usuário (ADMIN, GESTOR, USUARIO)
     *   - issuedAt: data/hora de criação
     *   - expiration: data/hora de expiração (issuedAt + jwt.expiration)
     *
     * @param email email do usuário (será o "subject" do token)
     * @param role  role do usuário (será uma claim customizada)
     * @return token JWT assinado como String
     */
    public String gerarToken(String email, String role) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiration);

        return Jwts.builder()
                .subject(email)                  // quem é o dono do token
                .claim("role", role)             // claim customizada com a role
                .issuedAt(agora)                 // quando o token foi criado
                .expiration(expiracao)            // quando o token expira
                .signWith(secretKey)              // assina com HMAC-SHA256
                .compact();                       // serializa para String
    }

    /**
     * Valida o token JWT e extrai as claims (dados) contidas nele.
     *
     * O método verifyWith() verifica:
     * 1. A assinatura bate com a secretKey (integridade)
     * 2. O token não expirou (expiration > agora)
     * 3. O formato do token é válido (3 partes, base64, JSON)
     *
     * Se qualquer validação falhar, lança JwtException.
     *
     * @param token o token JWT recebido no header Authorization
     * @return Claims com os dados do token (subject, role, etc.)
     * @throws JwtException se o token for inválido, expirado ou adulterado
     */
    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)        // configura a chave para verificação
                .build()
                .parseSignedClaims(token)     // valida e faz o parse
                .getPayload();                // retorna as claims (dados)
    }

    /**
     * Extrai o email (subject) do token.
     * Atalho para extrairClaims(token).getSubject().
     */
    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    /**
     * Extrai a role do token.
     * A role é uma claim customizada adicionada na geração.
     */
    public String extrairRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    /**
     * Verifica se o token é válido (assinatura correta e não expirado).
     * Retorna true se válido, false se inválido por qualquer motivo.
     *
     * Usamos try-catch porque o método extrairClaims() lança exceção
     * para tokens inválidos — aqui convertemos exceção em boolean.
     */
    public boolean isTokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
