import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.spring") version "1.6.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "com.koproxy"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.bootJar {
    archiveFileName.set("app.jar")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    runtimeOnly("org.postgresql:r2dbc-postgresql:1.0.0.RELEASE")
    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
    implementation("io.jsonwebtoken:jjwt-gson:0.11.2")
}

tasks {
    bootJar {
        dependsOn(test)
        dependsOn(ktlintCheck)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
