plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

group = "com.requena.orykaisoftware"
version = "1.0.0"
application {
    mainClass.set("com.requena.supportdesk.server.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.java.jwt)
    implementation(libs.logback)
    implementation(libs.postgresql)
    implementation(libs.jbcrypt)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.ktor.serverCallLogging)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverForwardedHeader)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverRateLimit)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serializationKotlinxJson)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.embedded.postgres)
}
