package com.clone.epicgames;

import static spark.Spark.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        // 1. Configurar a porta do Railway ou uma porta padrão
        ProcessBuilder processBuilder = new ProcessBuilder();
        Integer port;
        if (processBuilder.environment().get("PORT") != null) {
            port = Integer.parseInt(processBuilder.environment().get("PORT"));
        } else {
            port = 4567; // Porta padrão se não estiver rodando no Railway
        }
        port(port);

        // 2. Configurar o CORS (continua igual)
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });
        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        
        System.out.println("Servidor Java iniciado. Aguardando requisições na porta " + port + "...");

        // --- 3. NOVA LÓGICA DE CONEXÃO COM O POSTGRESQL ---
        
        // Pega a URL de conexão do banco de dados das variáveis de ambiente do Railway
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null) {
            System.out.println("ERRO CRÍTICO: Variável de ambiente DATABASE_URL não encontrada.");
            return; // Para a aplicação se não houver como conectar
        }

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            System.out.println("Conexão com o banco de dados PostgreSQL estabelecida.");

            Statement stmt = conn.createStatement();
            // Comando SQL ajustado para PostgreSQL
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY," + // SERIAL é o autoincremento do PostgreSQL
                    "username TEXT NOT NULL," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL);";
            stmt.execute(sql);
            System.out.println("Tabela 'users' verificada/criada com sucesso.");

        } catch (Exception e) {
            System.out.println("ERRO CRÍTICO ao conectar ou criar tabela: " + e.getMessage());
            e.printStackTrace();
        }

        // --- 4. ROTA DE CADASTRO (O CÓDIGO AQUI DENTRO NÃO MUDA NADA) ---
        post("/api/cadastrar", (request, response) -> {
            response.type("application/json");

            String username = request.queryParams("username");
            String email = request.queryParams("email");
            String password = request.queryParams("password");

            if (username == null || username.isEmpty() || email == null || email.isEmpty() || password == null
                    || password.isEmpty()) {
                response.status(400);
                return "{\"message\": \"Todos os campos são obrigatórios.\"}";
            }

            // A lógica com PreparedStatement funciona perfeitamente com PostgreSQL
            String sql = "INSERT INTO users(username, email, password) VALUES(?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(dbUrl); // Conecta novamente usando a URL
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, password);
                pstmt.executeUpdate();

                return "{\"message\": \"Cadastro realizado com sucesso!\"}";

            } catch (java.sql.SQLException e) {
                if (e.getSQLState().equals("23505")) { // Código de erro para violação de UNIQUE no PostgreSQL
                    response.status(409); // Conflict
                    return "{\"message\": \"Este e-mail já está cadastrado.\"}";
                } else {
                    response.status(500);
                    e.printStackTrace();
                    return "{\"message\": \"Ocorreu um erro no servidor ao salvar os dados.\"}";
                }
            }
        });
    }
}