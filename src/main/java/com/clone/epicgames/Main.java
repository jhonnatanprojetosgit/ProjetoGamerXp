package com.clone.epicgames;

import static spark.Spark.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {

        // --- 1. ORDEM DE INICIALIZAÇÃO CORRETA DO SPARK ---
        ProcessBuilder processBuilder = new ProcessBuilder();
        Integer port;
        if (processBuilder.environment().get("PORT") != null) {
            port = Integer.parseInt(processBuilder.environment().get("PORT"));
        } else {
            port = 4567;
        }
        port(port);

        staticFiles.location("/public");
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

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

        // --- 2. NOVA LÓGICA DE CONEXÃO USANDO VARIÁVEIS SEPARADAS ---
        try {
            Class.forName("org.postgresql.Driver");

            // Pega cada parte da conexão de sua respectiva variável de ambiente
            String dbHost = System.getenv("PGHOST");
            String dbPort = System.getenv("PGPORT");
            String dbName = System.getenv("PGDATABASE");
            String dbUser = System.getenv("PGUSER");
            String dbPassword = System.getenv("PGPASSWORD");

            // Monta a URL JDBC limpa, sem usuário e senha
            String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

            // Conecta passando o usuário e a senha separadamente (forma mais segura)
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
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
            stop(); // Para o servidor se a conexão com o DB falhar
            return;
        }

        // --- 3. ROTA DA API (USA A MESMA LÓGICA DE CONEXÃO) ---
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

            // Pega as variáveis novamente para a conexão da rota
            String dbHost = System.getenv("PGHOST");
            String dbPort = System.getenv("PGPORT");
            String dbName = System.getenv("PGDATABASE");
            String dbUser = System.getenv("PGUSER");
            String dbPassword = System.getenv("PGPASSWORD");
            String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                    PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
            } catch (Exception e) {
                response.status(500);
                e.printStackTrace();
                return "{\"message\": \"Um erro inesperado ocorreu.\"}";
            }
        });
    }
}