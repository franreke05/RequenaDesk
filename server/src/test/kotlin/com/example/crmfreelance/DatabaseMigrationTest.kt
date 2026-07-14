package com.example.crmfreelance

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseMigrationTest {
    @Test
    fun baseMigrationAvoidsUnsupportedSubqueriesInCheckConstraints() {
        val migration = migrationText("db/migration/V1__supportdesk_schema.sql")

        assertFalse(migration.contains("CHECK (EXISTS", ignoreCase = true))
        assertTrue(migration.contains("FOREIGN KEY (owner_admin_id)"))
        assertTrue(migration.contains("touch_ticket_from_attachment"))
        assertFalse(migration.contains("NEW.ticket_id :="))
    }

    @Test
    fun invoiceMigrationProvidesTablesConstraintsAndIndexes() {
        val migration = migrationText("db/migration/V2__invoices.sql")

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS invoices"))
        assertTrue(migration.contains("invoices_status_check"))
        assertTrue(migration.contains("idx_invoice_items_invoice_id"))
    }

    private fun migrationText(path: String): String {
        val resource = requireNotNull(javaClass.classLoader.getResource(path)) {
            "Missing migration resource: $path"
        }
        return resource.openStream().bufferedReader().use { it.readText() }
    }
}
