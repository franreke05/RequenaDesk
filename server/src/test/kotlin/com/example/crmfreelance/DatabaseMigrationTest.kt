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

    @Test
    fun programCatalogMigrationCreatesSubscriptionsRequestsAuditAndRls() {
        val migration = migrationText("db/migration/V8__client_program_catalog_and_subscriptions.sql")

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS product_catalog"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS client_product_subscriptions"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS client_program_requests"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS client_subscription_events"))
        assertTrue(migration.contains("'SERVICE_SLA'"))
        assertTrue(migration.contains("'SHEETS'"))
        assertTrue(migration.contains("ALTER TABLE public.client_program_requests ENABLE ROW LEVEL SECURITY"))
        assertTrue(migration.contains("REVOKE ALL PRIVILEGES ON TABLE public.product_catalog"))
    }

    @Test
    fun freeBetaCatalogDefinesAllSevenBusinessProgramsAtZeroPrice() {
        val migration = migrationText("db/migration/V9__business_programs_free_beta_catalog.sql")

        assertTrue(migration.contains("'BUSINESS_INVOICING'"))
        assertTrue(migration.contains("'BUSINESS_ACCOUNTING'"))
        assertTrue(migration.contains("'BUSINESS_CUSTOMERS'"))
        assertTrue(migration.contains("'BUSINESS_QUOTES'"))
        assertTrue(migration.contains("'BUSINESS_CATALOG'"))
        assertTrue(migration.contains("'BUSINESS_BOOKINGS'"))
        assertTrue(migration.contains("'BUSINESS_DOCUMENTS'"))
        assertTrue(migration.contains("monthly_price_cents = 0"))
        assertTrue(migration.contains("client_program_requests_quote_check"))
    }

    @Test
    fun businessBetaMigrationsPersistEachProgramFamilyBehindRls() {
        val finance = migrationText("db/migration/V10__business_finance_beta.sql")
        val operations = migrationText("db/migration/V11__business_operations_beta.sql")
        val sales = migrationText("db/migration/V12__business_sales_beta.sql")

        assertTrue(finance.contains("CREATE TABLE IF NOT EXISTS business_sales_documents"))
        assertTrue(finance.contains("CREATE TABLE IF NOT EXISTS business_finance_entries"))
        assertTrue(finance.contains("ENABLE ROW LEVEL SECURITY"))
        assertTrue(operations.contains("CREATE TABLE business_appointments"))
        assertTrue(operations.contains("business_appointments_no_active_overlap"))
        assertTrue(operations.contains("CREATE TABLE business_document_versions"))
        assertTrue(operations.contains("ENABLE ROW LEVEL SECURITY"))
        assertTrue(sales.contains("CREATE TABLE IF NOT EXISTS business_sales_customers"))
        assertTrue(sales.contains("CREATE TABLE IF NOT EXISTS business_catalog_items"))
        assertTrue(sales.contains("CREATE TABLE IF NOT EXISTS business_sales_quotes"))
        assertTrue(sales.contains("ENABLE ROW LEVEL SECURITY"))
        assertTrue(sales.contains("REVOKE ALL PRIVILEGES ON TABLE public.business_catalog_stock_summary"))
    }

    @Test
    fun workSessionsMigrationCreatesSessionsSegmentsAndProtectsThem() {
        val migration = migrationText("db/migration/V13__work_sessions_and_segments.sql")

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS work_sessions"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS work_segments"))
        assertTrue(migration.contains("idx_work_sessions_single_active_per_owner"))
        assertTrue(migration.contains("idx_work_segments_single_open_per_session"))
        assertTrue(migration.contains("idx_time_logs_one_historic_per_task"))
        assertTrue(migration.contains("ALTER TABLE public.work_sessions ENABLE ROW LEVEL SECURITY"))
        assertTrue(migration.contains("ALTER TABLE public.work_segments ENABLE ROW LEVEL SECURITY"))
        assertFalse(migration.contains("invoice", ignoreCase = true))
    }

    @Test
    fun dailyTaskBoardMigrationPersistsCreatorAndNormalizedPlacements() {
        val migration = migrationText("db/migration/V14__daily_task_board.sql")

        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS created_by_user_id"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_placements"))
        assertTrue(migration.contains("task_board_placements_position_x_check"))
        assertTrue(migration.contains("task_board_placements_position_y_check"))
        assertTrue(migration.contains("remove_completed_task_board_placement"))
        assertTrue(migration.contains("ALTER TABLE public.task_board_placements ENABLE ROW LEVEL SECURITY"))
        assertTrue(migration.contains("REVOKE ALL PRIVILEGES ON public.task_board_placements FROM PUBLIC"))
    }

    @Test
    fun v15CreatesPrivateCrmProjectsBoardAndTimeTables() {
        val migration = migrationText("db/migration/V15__business_customers_and_projects.sql")

        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS customer_type"))
        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS commercial_stage"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS business_sales_customer_activities"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS business_projects"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS business_tasks"))
        assertTrue(migration.contains("business_tasks_single_parent_check"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS business_task_board_placements"))
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS business_work_sessions"))
        assertTrue(migration.contains("business_work_sessions_one_live_idx"))
        assertTrue(migration.contains("'BUSINESS_PROJECTS'"))
        assertTrue(migration.contains("'[\"projects\", \"tasks\", \"daily_board\", \"time_tracking\"]'::jsonb"))
        assertTrue(migration.contains("ENABLE ROW LEVEL SECURITY"))
        assertTrue(migration.contains("REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC"))
    }

    private fun migrationText(path: String): String {
        val resource = requireNotNull(javaClass.classLoader.getResource(path)) {
            "Missing migration resource: $path"
        }
        return resource.openStream().bufferedReader().use { it.readText() }
    }
}
