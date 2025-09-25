package com.clone.epicgames;

import static spark.Spark.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {

        // 1. Ordem de inicialização correta do Spark
        ProcessBuilder processBuilder = new ProcessBuilder();
        Integer port;
        if (processBuilder.environment().get("PORT") != null) {
            port = Integer.parseInt(processBuilder.environment().get("PORT"));
        } else {
            port = 4567;
        }
        port(port);

        staticFiles.location("/public");

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

        // --- 2. LÓGICA DE CONEXÃO COM SQLITE (SIMPLES E DIRETA) ---
        String dbUrl = "jdbc:sqlite:epicgames.db"; // Cria o arquivo na pasta da aplicação

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            System.out.println("Conexão com o banco de dados SQLite estabelecida.");
            Statement stmt = conn.createStatement();
            // SQL para SQLite
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL);";
            stmt.execute(sql);
            System.out.println("Tabela 'users' verificada/criada com sucesso.");

        } catch (Exception e) {
            System.out.println("ERRO CRÍTICO ao inicializar o banco de dados SQLite: " + e.getMessage());
            e.printStackTrace();
            stop(); // Para o servidor se a conexão com o DB falhar
            return;
        }

        // --- 3. ROTA DA API (USA A MESMA URL DE CONEXÃO DO SQLITE) ---
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

            try (Connection connection = DriverManager.getConnection(dbUrl);
                    PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, password);
                pstmt.executeUpdate();
                return "{\"message\": \"Cadastro realizado com sucesso!\"}";
            } catch (java.sql.SQLException e) {
                // Tratamento de erro específico do SQLite para e-mail duplicado
                if (e.getMessage().contains("UNIQUE constraint failed")) {
                    response.status(409);
                    return "{\"message\": \"Este e-mail já está cadastrado.\"}";
                } else {
                    response.status(500);
                    e.printStackTrace();
                    return "{\"message\": \"Ocorreu um erro no servidor ao salvar os dados.\"}";
                }
            } catch (Exception e) {
                response.status(500);
                e.printStackTrace();
                return "{\"message\": \"Um erro inesperado ocorreu.\"}";
            }
        });
    }
}