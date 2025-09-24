package com.clone.epicgames;

import static spark.Spark.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        staticFiles.location("/public");
        // Adicione esta rota de teste
        get("/", (req, res) -> {
            res.type("text/html");
            return "<h1>Servidor está no ar!</h1>";
        });

        ProcessBuilder processBuilder = new ProcessBuilder();
        Integer port;
        if (processBuilder.environment().get("PORT") != null) {
            port = Integer.parseInt(processBuilder.environment().get("PORT"));
        } else {
            port = 4567;
        }
        port(port);

        options("/*", (request, response) -> {
            // ... (código CORS continua o mesmo)
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

        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");

            // --- LÓGICA DE CONEXÃO ROBUSTA E CORRIGIDA ---
            String databaseUrlFromEnv = System.getenv("DATABASE_URL");
            if (databaseUrlFromEnv == null) {
                throw new Exception("Variável de ambiente DATABASE_URL não encontrada.");
            }

            URI dbUri = new URI(databaseUrlFromEnv);

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);

            conn = DriverManager.getConnection(dbUrl, props);
            // --- FIM DA LÓGICA DE CONEXÃO ---

            System.out.println("Conexão com o banco de dados PostgreSQL estabelecida.");

            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY," +
                    "username TEXT NOT NULL," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL);";
            stmt.execute(sql);
            System.out.println("Tabela 'users' verificada/criada com sucesso.");

        } catch (Exception e) {
            System.out.println("ERRO CRÍTICO ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            return;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    /* Ignorar */ }
            }
        }

        post("/api/cadastrar", (request, response) -> {
            // ... (código da rota de cadastro continua o mesmo, sem alterações)
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

            // Reutiliza a mesma lógica de conexão robusta para a rota
            URI dbUri = new URI(System.getenv("DATABASE_URL"));
            String connUsername = dbUri.getUserInfo().split(":")[0];
            String connPassword = dbUri.getUserInfo().split(":")[1];
            String connDbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

            Properties props = new Properties();
            props.setProperty("user", connUsername);
            props.setProperty("password", connPassword);

            try (Connection connection = DriverManager.getConnection(connDbUrl, props);
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
            }
        });
    }
}