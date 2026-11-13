package com.admin.template.service.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRegistrationMigrationTest {
    @Test
    void migrationAllowsPasswordlessUsersAndGrantsOnlyTheFourOrdinaryMenusIdempotently() throws Exception {
        try (var db = database()) {
            migrate(db);
            try (var statement = db.createStatement()) {
                statement.executeUpdate("INSERT INTO users(id,password) VALUES(1,NULL)");
            }
            assertThat(permissions(db)).containsExactlyInAnyOrder("home", "ai", "chat", "pageAgent");
            migrate(db);
            assertThat(permissions(db)).containsExactlyInAnyOrder("home", "ai", "chat", "pageAgent");
            try (var statement = db.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM roles WHERE code='user' AND status=1")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (var statement = db.createStatement();
                 var result = statement.executeQuery("SELECT menu_id,parent_id,type FROM menus WHERE buttonPermission='system:file:manage'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("menu_id")).isEqualTo("menu_system_file_manage");
                assertThat(result.getString("parent_id")).isEqualTo("users");
                assertThat(result.getString("type")).isEqualTo("B");
                assertThat(result.next()).isFalse();
            }
        }
    }

    @Test
    void migrationPreservesAnExistingDisabledDefaultRoleAndDoesNotGrantItMenus() throws Exception {
        try (var db = database()) {
            try (var statement = db.createStatement()) {
                statement.executeUpdate("INSERT INTO roles(name,code,description,status,sort) VALUES('Custom','user','Kept',false,9)");
            }
            migrate(db);
            try (var statement = db.createStatement();
                 var role = statement.executeQuery("SELECT name,description,status,sort FROM roles WHERE code='user'")) {
                role.next();
                assertThat(role.getString("name")).isEqualTo("Custom");
                assertThat(role.getString("description")).isEqualTo("Kept");
                assertThat(role.getBoolean("status")).isFalse();
                assertThat(role.getInt("sort")).isEqualTo(9);
            }
            assertThat(permissions(db)).isEmpty();
        }
    }

    private Connection database() throws Exception {
        var db = DriverManager.getConnection("jdbc:h2:mem:github-migration-" + UUID.randomUUID() + ";MODE=MySQL");
        try (var statement = db.createStatement()) {
            statement.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,password VARCHAR(100) NOT NULL)");
            statement.execute("CREATE TABLE roles(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(50),code VARCHAR(50) UNIQUE,description VARCHAR(255),status BOOLEAN,sort INT)");
            statement.execute("CREATE TABLE menus(menu_id VARCHAR(50) PRIMARY KEY,parent_id VARCHAR(50),name VARCHAR(50),"
                    + "path VARCHAR(100),sort INT,hidden BOOLEAN,is_full BOOLEAN,type VARCHAR(1),buttonPermission VARCHAR(100),status BOOLEAN)");
            statement.execute("CREATE TABLE role_menus(role_id BIGINT,menu_id VARCHAR(50),UNIQUE(role_id,menu_id))");
            statement.executeUpdate("INSERT INTO menus(menu_id,path,type,status) VALUES('home','/home','C',true),('ai','/ai','M',true),"
                    + "('chat','/ai/intelligentConversation','C',true),('pageAgent','/ai/page-agent','C',true),"
                    + "('system','/system','M',true),('users','/system/user','C',true),('eval','/ai/retrieval-evaluation','C',true),"
                    + "('disabled','/home','C',false),('button','/home','F',true)");
        }
        return db;
    }

    private void migrate(Connection db) throws Exception {
        String script = Files.readString(Path.of("../deploy/mysql/migrations/2026-11-13-github-registration.sql"));
        // Adapt only MySQL connection/transaction syntax; execute schema and data statements unchanged.
        ScriptUtils.executeSqlScript(db, new ByteArrayResource(script.replace("SET NAMES utf8mb4;", "")
                .replace("START TRANSACTION;", "BEGIN;")
                .getBytes(StandardCharsets.UTF_8)));
    }

    private ArrayList<String> permissions(Connection db) throws Exception {
        var permissions = new ArrayList<String>();
        try (var statement = db.createStatement();
             var rows = statement.executeQuery("SELECT rm.menu_id FROM role_menus rm INNER JOIN roles r ON r.id=rm.role_id WHERE r.code='user'")) {
            while (rows.next()) permissions.add(rows.getString(1));
        }
        return permissions;
    }
}
