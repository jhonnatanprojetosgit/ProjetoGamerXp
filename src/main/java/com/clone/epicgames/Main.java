package com.clone.epicgames;

import static spark.Spark.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet; // Import necessário para ler os resultados da query

public class Main {
    public static void main(String[] args) {
        // 1. Configurar a porta primeiro
        port(4567);

        // 2. Configurar o CORS
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

        System.out.println("Servidor Java iniciado. Aguardando requisições na porta 4567...");

        String userHome = System.getProperty("user.home");
        String dbPath = userHome + "/Desktop/epicgames.db";
        String url = "jdbc:sqlite:" + dbPath;

        System.out.println("Usando banco de dados em: " + dbPath);

        try (Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement()) {
            
            System.out.println("Conexão com o banco de dados SQLite estabelecida.");
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL);";
            stmt.execute(sql);
            System.out.println("Tabela 'users' verificada/criada com sucesso.");

        } catch (Exception e) {
            System.out.println("ERRO CRÍTICO ao conectar ou criar tabela: " + e.getMessage());
        }

        // Rota para o formulário de cadastro
        post("/api/cadastrar", (request, response) -> {
            response.type("application/json");

            String username = request.queryParams("username");
            String email = request.queryParams("email");
            String password = request.queryParams("password");

            System.out.println("Recebida requisição de cadastro: " + username + ", " + email);

            if (username == null || username.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty()) {
                response.status(400);
                System.out.println("Falha na validação: campos obrigatórios ausentes.");
                return "{\"message\": \"Todos os campos são obrigatórios.\"}";
            }

            String sql = "INSERT INTO users(username, email, password) VALUES(?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, password);
                pstmt.executeUpdate();

                System.out.println("SUCESSO: Novo usuário cadastrado: " + email);
                return "{\"message\": \"Cadastro realizado com sucesso!\"}";

            } catch (java.sql.SQLException e) {
                if (e.getMessage().contains("UNIQUE constraint failed")) {
                    response.status(409); // Conflict
                    System.out.println("ERRO: E-mail já cadastrado: " + email);
                    return "{\"message\": \"Este e-mail já está cadastrado.\"}";
                } else {
                    response.status(500);
                    System.out.println("ERRO de SQL: " + e.getMessage());
                    e.printStackTrace();
                    return "{\"message\": \"Ocorreu um erro no servidor ao salvar os dados.\"}";
                }
            } catch (Exception e) {
                response.status(500);
                System.out.println("ERRO Inesperado: " + e.getMessage());
                e.printStackTrace();
                return "{\"message\": \"Ocorreu um erro inesperado no servidor.\"}";
            }
        });

        // =======================================================
        // NOVA ROTA DE LOGIN ADICIONADA AQUI
        // =======================================================
        post("/api/login", (request, response) -> {
            response.type("application/json");

            String email = request.queryParams("email");
            String password = request.queryParams("password");

            // Log para depuração
            System.out.println("Recebida requisição de login para o e-mail: " + email);

            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                response.status(400); // Bad Request
                return "{\"message\": \"E-mail e senha são obrigatórios.\"}";
            }
            
            String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

            try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, email);
                pstmt.setString(2, password);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // Se rs.next() for verdadeiro, significa que encontrou um usuário
                    System.out.println("SUCESSO: Login bem-sucedido para: " + email);
                    response.status(200);
                    return "{\"message\": \"Login bem-sucedido!\"}";
                } else {
                    // Se não encontrou, o e-mail ou a senha estão errados
                    System.out.println("FALHA: Tentativa de login com credenciais incorretas para: " + email);
                    response.status(401); // Unauthorized
                    return "{\"message\": \"E-mail ou senha incorretos.\"}";
                }

            } catch (Exception e) {
                response.status(500);
                System.out.println("ERRO no banco de dados durante o login: " + e.getMessage());
                e.printStackTrace();
                return "{\"message\": \"Ocorreu um erro no servidor.\"}";
            }
        });
    }
}