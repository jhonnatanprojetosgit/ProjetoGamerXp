package com.clone.epicgames;

import static spark.Spark.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        staticFiles.location("/public");

        ProcessBuilder processBuilder = new ProcessBuilder();
        Integer port;
        if (processBuilder.environment().get("PORT") != null) {
            port = Integer.parseInt(processBuilder.environment().get("PORT"));
        } else {
            port = 4567;
        }
        port(port);

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

        String dbUrl = null;
        try {
            Class.forName("org.postgresql.Driver");

            // --- CORREÇÃO FINAL E DEFINITIVA AQUI ---
            String databaseUrlFromEnv = System.getenv("DATABASE_URL");
            if (databaseUrlFromEnv == null) {
                throw new Exception("Variável de ambiente DATABASE_URL não encontrada.");
            }
            // Substituímos o início da URL para o formato JDBC exato
            dbUrl = databaseUrlFromEnv.replace("postgresql://", "jdbc:postgresql://");

            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                System.out.println("Conexão com o banco de dados PostgreSQL estabelecida.");

                Statement stmt = conn.createStatement();
                String sql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id SERIAL PRIMARY KEY," +
                        "username TEXT NOT NULL," +
                        "email TEXT NOT NULL UNIQUE," +
                        "password TEXT NOT NULL);";
                stmt.execute(sql);
                System.out.println("Tabela 'users' verificada/criada com sucesso.");
            }

        } catch (Exception e) {
            System.out.println("ERRO CRÍTICO ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            return; // Para a aplicação se não conseguir inicializar o DB
        }

        // Passa a URL final para a rota de cadastro
        final String finalDbUrl = dbUrl;
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

            String sql = "INSERT INTO users(username, email, password) VALUES(?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(finalDbUrl);
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, password);
                pstmt.executeUpdate();

                return "{\"message\": \"Cadastro realizado com sucesso!\"}";

            } catch (java.sql.SQLException e) {
                if (e.getSQLState().equals("23505")) {
                    response.status(409);
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