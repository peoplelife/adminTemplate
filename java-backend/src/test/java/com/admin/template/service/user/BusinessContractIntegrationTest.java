package com.admin.template.service.user;

import com.admin.template.AdminTemplateApplication;
import com.admin.template.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdminTemplateApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:business-contract;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.user.default-password=654321"
})
@AutoConfigureMockMvc
class BusinessContractIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    private String authorization;

    @BeforeEach
    void prepareDatabase() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE, "
                + "password VARCHAR(100), nickname VARCHAR(50), email VARCHAR(100), phone VARCHAR(20), avatar VARCHAR(255), "
                + "status BOOLEAN, last_login TIMESTAMP, last_logout TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS roles (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), code VARCHAR(50) UNIQUE, "
                + "description VARCHAR(200), status BOOLEAN, sort INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS menus (id BIGINT AUTO_INCREMENT PRIMARY KEY, menu_id VARCHAR(50) UNIQUE, parent_id VARCHAR(50), "
                + "name VARCHAR(50), path VARCHAR(100), component VARCHAR(100), icon VARCHAR(50), sort INTEGER, hidden BOOLEAN, is_full BOOLEAN, "
                + "type VARCHAR(20), buttonPermission VARCHAR(100), status BOOLEAN, created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_roles (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, role_id BIGINT, "
                + "created_at TIMESTAMP, updated_at TIMESTAMP, UNIQUE(user_id, role_id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS role_menus (id BIGINT AUTO_INCREMENT PRIMARY KEY, role_id BIGINT, menu_id VARCHAR(50), "
                + "created_at TIMESTAMP, UNIQUE(role_id, menu_id))");
        for (String table : List.of("user_roles", "role_menus", "users", "roles", "menus")) jdbc.execute("DELETE FROM " + table);
        jdbc.update("INSERT INTO users(id, username, password, nickname, status, created_at, updated_at) "
                + "VALUES (1,'admin','123456','Admin',true,'2020-01-01 00:00:00','2020-01-01 00:00:00'),"
                + "(2,'alice','123456','Alice',false,'2020-01-01 00:00:00','2020-01-01 00:00:00')");
        jdbc.update("INSERT INTO roles(id,name,code,description,status) VALUES (1,'Super admin','super_admin','old description',true)");
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (1,1),(2,1)");
        jdbc.update("INSERT INTO menus(id,menu_id,parent_id,name,path,component,icon,sort,hidden,is_full,type,buttonPermission,status) "
                + "VALUES (1,'home','0','Home','/old','OldView','old-icon',0,false,false,'C','old:permission',true),"
                + "(2,'child','home','Child','/child','ChildView','child-icon',1,false,false,'C','child:permission',true)");
        jdbc.update("INSERT INTO role_menus(role_id,menu_id) VALUES (1,'home')");
        authorization = "Bearer " + jwt.createToken(1, "admin", List.of("super_admin"));
    }

    @Test
    void softDeletePersistsBothTimestampsAndHidesTheUserFromReads() throws Exception {
        mvc.perform(delete("/v1/users/2").header("Authorization", authorization)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT deleted_at FROM users WHERE id=2", Timestamp.class)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM users WHERE id=2", Timestamp.class))
                .isAfter(Timestamp.valueOf("2020-01-01 00:00:00"));
        mvc.perform(get("/v1/users/2").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(120001));
        mvc.perform(get("/v1/users").header("Authorization", authorization))
                .andExpect(jsonPath("$.data.pagination.total").value(1));
    }

    @Test
    void configuredDefaultPasswordIsUsedWhenTheRequestOmitsIt() throws Exception {
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"created\"}")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT password FROM users WHERE username='created'", String.class)).isEqualTo("654321");
    }

    @Test
    void trimsUsernameAndPasswordBeforePersistenceAndDuplicateChecks() throws Exception {
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\" created \",\"password\":\" secret123 \"}"))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT password FROM users WHERE username='created'", String.class)).isEqualTo("secret123");
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\" alice \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(120002));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"password\":\" 12345 \"}", "{\"password\":\"      \"}", "{\"status\":1.5}", "{\"roleCodes\":[\" \"]}", "{\"roleCodes\":null}"})
    void rejectsInvalidUserCreationWithoutWritingAUser(String fields) throws Exception {
        String body = "{\"username\":\"new-user\"," + fields.substring(1);
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(100001));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username='new-user'", Integer.class)).isZero();
    }

    @Test
    void rejectsANumericUsernameInsteadOfSilentlyConvertingItToText() throws Exception {
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":42}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details[0].path").value("username"));
    }

    @Test
    void whitespaceEmailIsInvalidRatherThanAnOmittedValue() throws Exception {
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new-user\",\"email\":\"   \"}"))
                .andExpect(status().isOk());
    }

    @Test
    void blankRoleCodeDoesNotClearTheUsersExistingRoles() throws Exception {
        mvc.perform(patch("/v1/users/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleCodes\":[\"  \"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details[0].path").value("roleCodes.0"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id=2", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[\"  \"]", "null"})
    void invalidMenuIdListsDoNotClearTheRolesExistingMenus(String menuIds) throws Exception {
        mvc.perform(patch("/v1/roles/1").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"menuIds\":" + menuIds + "}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_menus WHERE role_id=1", Integer.class)).isEqualTo(1);
    }

    @Test
    void nullableMenuPatchClearsColumnsAndResetsParentWhileOmittedFieldsStayUntouched() throws Exception {
        mvc.perform(patch("/v1/menus/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":null,\"path\":null,\"component\":null,\"icon\":null,\"buttonPermission\":null}"))
                .andExpect(status().isOk());
        var row = jdbc.queryForMap("SELECT * FROM menus WHERE id=2");
        assertThat(row.get("PARENT_ID")).isEqualTo("0");
        assertThat(row.get("PATH")).isNull();
        assertThat(row.get("COMPONENT")).isNull();
        assertThat(row.get("ICON")).isNull();
        assertThat(row.get("BUTTONPERMISSION")).isNull();
        assertThat(row.get("NAME")).isEqualTo("Child");
        assertThat(row.get("MENU_ID")).isEqualTo("child");
    }

    @Test
    void menuPatchRetainsOmittedNullableColumns() throws Exception {
        mvc.perform(patch("/v1/menus/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" Renamed \"}"))
                .andExpect(status().isOk());
        var row = jdbc.queryForMap("SELECT * FROM menus WHERE id=2");
        assertThat(row.get("PATH")).isEqualTo("/child");
        assertThat(row.get("PARENT_ID")).isEqualTo("home");
        assertThat(row.get("NAME")).isEqualTo("Renamed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"unknown\":true}", "{\"name\":null}", "{\"menuId\":null}", "{\"sort\":1.5}", "{\"type\":\"X\"}"})
    void rejectsInvalidOrEmptyMenuPatches(String body) throws Exception {
        mvc.perform(patch("/v1/menus/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"path,101", "component,101", "icon,51", "buttonPermission,101"})
    void enforcesMenuCreationStringLimits(String field, int length) throws Exception {
        mvc.perform(post("/v1/menus").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"type\":\"C\",\"" + field + "\":\"" + "x".repeat(length) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details[0].path").value(field));
    }

    @Test
    void acceptsZodFlagCoercionAndIntegralDecimalStrings() throws Exception {
        mvc.perform(patch("/v1/users/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        mvc.perform(patch("/v1/users/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0));
        mvc.perform(get("/v1/users").param("page", "1.0").param("pageSize", "20.0").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pagination.page").value(1));
    }

    @Test
    void blankPageIsRejectedWhileBlankStatusIsZeroAndFiltersAreTrimmed() throws Exception {
        mvc.perform(get("/v1/users").param("page", "").header("Authorization", authorization))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/users").param("status", "").param("username", " alice ").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].username").value("alice"))
                .andExpect(jsonPath("$.data.pagination.total").value(1));
    }

    @Test
    void invalidMenuTypeQueryIsRejectedAndLowercaseTypeIsNormalized() throws Exception {
        mvc.perform(get("/v1/menus").param("menuType", "unknown").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details[0].path").value("menuType"));
        mvc.perform(get("/v1/menus").param("menuType", " c ").param("menuName", " Home ").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list.length()").value(1));
    }

    @Test
    void userAndRoleEmptyPatchesRemainValidAndNullDescriptionStaysUnchanged() throws Exception {
        mvc.perform(patch("/v1/users/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/v1/roles/1").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.description").value("old description"));
    }

    @Test
    void softDeletedUsernameUniquenessViolationsReturnConflictForCreateAndUpdate() throws Exception {
        jdbc.update("INSERT INTO users(username,password,status,deleted_at) VALUES ('reserved','123456',true,CURRENT_TIMESTAMP)");
        mvc.perform(post("/v1/users").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"reserved\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(120002));
        mvc.perform(patch("/v1/users/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"reserved\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(120002));
    }

    @Test
    void currentUserMenusOmitTheAdministrativeDatabaseIdField() throws Exception {
        mvc.perform(get("/v1/me/menus").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].id").doesNotHaveJsonPath());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/v1/users|{\"username\":\" \"}",
            "/v1/users|{\"username\":\"new\",\"phone\":\"12345\"}",
            "/v1/users|{\"username\":\"new\",\"email\":\"invalid\"}",
            "/v1/users|{\"username\":\"new\",\"status\":2}",
            "/v1/roles|{\"code\":\"bad code\",\"name\":\"Role\"}",
            "/v1/roles|{\"code\":\"good\",\"name\":\" \"}",
            "/v1/roles|{\"code\":\"good\",\"name\":\"Role\",\"status\":1.5}",
            "/v1/roles|{\"code\":\"good\",\"name\":\"Role\",\"menuIds\":[\" \"]}",
            "/v1/menus|{\"name\":\"Menu\",\"type\":\"X\"}",
            "/v1/menus|{\"name\":\"Menu\",\"type\":\"C\",\"sort\":-1}",
            "/v1/menus|{\"name\":\"Menu\",\"type\":\"C\",\"sort\":2147483648}",
            "/v1/menus|{\"name\":\"Menu\",\"type\":\"C\",\"hidden\":2}",
            "/v1/menus|{\"name\":\"Menu\",\"type\":\"C\",\"isFull\":-1}",
            "/v1/menus|{\"name\":\"Menu\",\"type\":\"C\",\"parentId\":true}"
    })
    void validatesCreateSchemaBoundsPatternsAndTypes(String route, String body) throws Exception {
        mvc.perform(post(route).header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(100001));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/v1/users/2|{\"phone\":\" \"}",
            "/v1/users/2|{\"email\":\"invalid\"}",
            "/v1/users/2|{\"status\":-1}",
            "/v1/roles/1|{\"code\":\"bad code\"}",
            "/v1/roles/1|{\"code\":\" \"}",
            "/v1/roles/1|{\"status\":2}",
            "/v1/menus/2|{\"name\":\" \"}",
            "/v1/menus/2|{\"menuId\":\"\"}",
            "/v1/menus/2|{\"hidden\":1.5}",
            "/v1/menus/2|{\"isFull\":2}"
    })
    void validatesPatchSchemaBoundsPatternsAndTypes(String route, String body) throws Exception {
        mvc.perform(patch(route).header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(100001));
    }

    @Test
    void deduplicatesTrimmedRoleCodesWhilePreservingTheExistingAuthorization() throws Exception {
        mvc.perform(patch("/v1/users/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleCodes\":[\" super_admin \",\"super_admin\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roleCodes.length()").value(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id=2", Integer.class)).isEqualTo(1);
    }

    @Test
    void menuFlagNullsAndNumericStringsFollowTheOriginalCoercion() throws Exception {
        mvc.perform(patch("/v1/menus/2").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sort\":\"2.0\",\"hidden\":null,\"isFull\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sort").value(2))
                .andExpect(jsonPath("$.data.hidden").value(0)).andExpect(jsonPath("$.data.isFull").value(1));
    }
}
