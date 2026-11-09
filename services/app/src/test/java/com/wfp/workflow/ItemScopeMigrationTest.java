package com.wfp.workflow;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItemScopeMigrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private final JdbcTemplate jdbc = new JdbcTemplate(
            new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));

    @Test
    void linksCommentsToItemsAndCopiesDefinitionsToProjects() {
        flyway().target("6").load().migrate();

        UUID platform = project("tenant-a", "PROJ");
        UUID bug = itemType(platform, "Bug", "bug-flow");
        itemType(platform, "Task", "bug-flow");
        itemType(platform, "Request", "request-flow");
        UUID operations = project("tenant-a", "OPS");
        itemType(operations, "Incident", "bug-flow");
        UUID otherTenantProject = project("tenant-b", "PROJ");
        UUID otherTenantType = itemType(otherTenantProject, "Bug", "bug-flow");

        UUID severity = definition("tenant-a", "bug-flow", "severity");
        option(severity, "High");
        definition("tenant-a", "request-flow", "severity");
        definition("tenant-a", "unused-flow", "unused");

        UUID item = item("tenant-a", "PROJ-1", platform, bug, "run-1");
        item("tenant-b", "PROJ-1", otherTenantProject, otherTenantType, "run-1");
        UUID commentOnItemRun = comment("tenant-a", "run-1");
        UUID commentOnOldProcess = comment("tenant-a", "run-without-item");

        flyway().target("8").load().migrate();

        assertThat(commentItem(commentOnItemRun)).isEqualTo(item);
        assertThat(commentItem(commentOnOldProcess)).isNull();

        assertThat(jdbc.queryForList("""
                SELECT string_agg(item_type.name, ',' ORDER BY item_type.name)
                FROM field_schema_item_type link
                JOIN wf_item_type item_type ON item_type.id = link.item_type_id
                JOIN field_schema definition ON definition.id = link.field_schema_id
                WHERE definition.project_id = ? GROUP BY definition.id
                """, String.class, platform)).containsExactlyInAnyOrder("Bug,Task", "Request");
        assertThat(count("SELECT count(*) FROM field_schema WHERE project_id = ?", operations)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM field_schema WHERE project_id = ?", otherTenantProject)).isZero();
        assertThat(count("""
                SELECT count(*) FROM field_option copied_option
                JOIN field_schema definition ON definition.id = copied_option.field_schema_id
                WHERE definition.project_id = ? AND copied_option.value = 'High'
                """, platform)).isEqualTo(1);

        assertThat(count("""
                SELECT count(*) FROM field_schema WHERE project_id IS NULL AND process_definition_key = ?
                """, "bug-flow")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM field_schema WHERE project_id IS NOT NULL AND field_key = ?", "unused"))
                .isZero();

        flyway().load().migrate();

        assertThat(jdbc.queryForList("SELECT id FROM wf_comments", UUID.class)).containsExactly(commentOnItemRun);
        assertThat(commentItem(commentOnItemRun)).isEqualTo(item);
        assertThat(count("SELECT count(*) FROM field_schema WHERE field_key = ?", "unused")).isZero();
        assertThat(count("SELECT count(*) FROM field_schema WHERE field_key = ?", "severity")).isEqualTo(3);
        assertThat(count("""
                SELECT count(*) FROM field_option copied_option
                JOIN field_schema definition ON definition.id = copied_option.field_schema_id
                WHERE definition.project_id = ? AND copied_option.value = 'High'
                """, platform)).isEqualTo(1);
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
    }

    private UUID project(String tenant, String key) {
        return inserted("INSERT INTO wf_project (tenant_id, project_key, name) VALUES (?, ?, ?) RETURNING id",
                tenant, key, key);
    }

    private UUID itemType(UUID project, String name, String workflow) {
        return inserted("INSERT INTO wf_item_type (project_id, name, workflow_key) VALUES (?, ?, ?) RETURNING id",
                project, name, workflow);
    }

    private UUID definition(String tenant, String workflow, String fieldKey) {
        return inserted("""
                INSERT INTO field_schema (process_definition_key, field_key, label, field_type, tenant_id)
                VALUES (?, ?, ?, 'DROPDOWN', ?) RETURNING id
                """, workflow, fieldKey, fieldKey, tenant);
    }

    private void option(UUID definition, String value) {
        jdbc.update("INSERT INTO field_option (field_schema_id, label, value) VALUES (?, ?, ?)",
                definition, value, value);
    }

    private UUID item(String tenant, String key, UUID project, UUID type, String run) {
        return inserted("""
                INSERT INTO wf_item (tenant_id, item_key, project_id, item_type_id, title, priority, reporter, run_id)
                VALUES (?, ?, ?, ?, 'Title', 'LOW', 'alice', ?) RETURNING id
                """, tenant, key, project, type, run);
    }

    private UUID comment(String tenant, String run) {
        return inserted("""
                INSERT INTO wf_comments (process_instance_id, user_id, content, tenant_id)
                VALUES (?, 'alice', 'Hello', ?) RETURNING id
                """, run, tenant);
    }

    private UUID commentItem(UUID comment) {
        return jdbc.queryForObject("SELECT item_id FROM wf_comments WHERE id = ?", UUID.class, comment);
    }

    private UUID inserted(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, UUID.class, arguments);
    }

    private int count(String sql, Object argument) {
        return jdbc.queryForObject(sql, Integer.class, argument);
    }
}
