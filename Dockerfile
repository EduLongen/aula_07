# =============================================================================
# CONCEITO: Multi-Stage Build
# =============================================================================
# Usamos dois estágios para manter a imagem final pequena:
#
# Estágio 1 (build): JDK completo + Maven → compila o projeto
# Estágio 2 (runtime): apenas JRE → executa o .jar gerado
#
# Sem multi-stage, a imagem final teria ~700MB (JDK + Maven + código-fonte).
# Com multi-stage, fica ~200MB (só o JRE + o .jar compilado).

# =============================================================================
# ESTÁGIO 1: BUILD — compila o projeto Java com Maven
# =============================================================================
# eclipse-temurin:21-jdk → imagem oficial do Java 21 com JDK completo (necessário para compilar)
FROM eclipse-temurin:21-jdk AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia os arquivos do Maven Wrapper ANTES do código-fonte.
# Isso aproveita o cache de layers do Docker:
# se o pom.xml não mudar, o Docker reutiliza o cache de dependências.
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Dá permissão de execução ao Maven Wrapper e baixa todas as dependências
# O flag -q (quiet) reduz o output para não poluir o log do build
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Agora copia o código-fonte (feito depois para aproveitar o cache de deps)
COPY src ./src

# Compila e empacota o projeto em um .jar, pulando os testes
# O -q reduz o output; o .jar fica em target/ClientHub-0.0.1-SNAPSHOT.jar
RUN ./mvnw package -DskipTests -q

# =============================================================================
# ESTÁGIO 2: RUNTIME — executa o .jar com JRE mínimo
# =============================================================================
# eclipse-temurin:21-jre → apenas o Java Runtime Environment, sem ferramentas de compilação
# Resultado: imagem muito menor e mais segura (menos superfície de ataque)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia APENAS o .jar gerado no estágio de build
# O *.jar é um glob — pega qualquer .jar em target/ (evita hardcode do nome)
COPY --from=build /app/target/*.jar app.jar

# Documenta a porta que o container expõe (não publica a porta — isso é do docker-compose)
EXPOSE 8080

# Comando executado quando o container iniciar
# java -jar app.jar → inicia o Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
