import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val springVersion = "6.1.5"
val springDataMongoVer = "4.2.5"
val modulithVer = "1.1.1"

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
    id("application") // оставить!
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.tripplanner"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework:spring-core:$springVersion")
    implementation("org.springframework:spring-context:$springVersion")
    implementation("org.springframework:spring-web:$springVersion")
    implementation("org.springframework:spring-webmvc:$springVersion")
    implementation("org.springframework:spring-webflux:$springVersion")
    annotationProcessor("org.springframework:spring-context-indexer:$springVersion")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core:$modulithVer")


    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // Jetty
    implementation("org.eclipse.jetty:jetty-server:11.0.17")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.17")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.15.3")

    // Тесты
    testImplementation("org.springframework:spring-test:$springVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    implementation("org.telegram:telegrambots:6.7.0")
    implementation("org.telegram:telegrambots-meta:6.7.0")

    // Spring WebFlux
    implementation("org.springframework:spring-webflux:6.1.5")
    implementation("io.projectreactor.netty:reactor-netty:1.1.13")
    
    // Spring Security
    implementation("org.springframework.security:spring-security-web:6.2.2")
    implementation("org.springframework.security:spring-security-config:6.2.2")
}

application {
    mainClass.set("org.tripplanner.Main")
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dspring.profiles.active=local")
}
