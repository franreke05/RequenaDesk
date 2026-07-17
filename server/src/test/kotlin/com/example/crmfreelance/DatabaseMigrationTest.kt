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

    @Test
    fun invoicePersistenceRemovalDropsTablesAndTheNumberingSequence() {
        val persistenceRemoval = migrationText("db/migration/V3__remove_invoice_persistence.sql")
        val sequenceRemoval = migrationText("db/migration/V4__remove_invoice_sequence.sql")

        assertTrue(persistenceRemoval.contains("DROP TABLE IF EXISTS invoice_items"))
        assertTrue(persistenceRemoval.contains("DROP TABLE IF EXISTS invoices"))
        assertTrue(sequenceRemoval.contains("DROP SEQUENCE IF EXISTS invoice_number_seq"))
    }

    @Test
    fun clientCrmMigrationCreatesContactsAndActivities() {
        val migration = migrationText("db/migration/V6__client_crm_contacts_and_activities.sql")

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS client_contacts"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS client_activities"))
        assertTrue(migration.contains("client_contacts_primary_per_client_idx"))
        assertTrue(migration.contains("client_activities_type_check"))
    }

    @Test
    fun securityMigrationEnablesRlsAndRemovesDirectPublicAccess() {
        val migration = migrationText("db/migration/V7__secure_public_schema_with_rls.sql")

        assertTrue(migration.contains("ALTER TABLE public.clients ENABLE ROW LEVEL SECURITY"))
        assertTrue(migration.contains("ALTER TABLE public.client_activities ENABLE ROW LEVEL SECURITY"))
        assertTrue(migration.contains("REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM PUBLIC"))
        assertTrue(migration.contains("'anon', 'authenticated'"))
        assertTrue(migration.contains("REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA public FROM PUBLIC"))
    }

    private fun migrationText(path: String): String {
        val resource = requireNotNull(javaClass.classLoader.getResource(path)) {
            "Missing migration resource: $path"
        }
        return resource.openStream().bufferedReader().use { it.readText() }
    }
}
