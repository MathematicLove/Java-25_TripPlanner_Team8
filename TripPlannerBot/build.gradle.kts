import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val springVersion = "6.1.5"
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
java.sourceCompatibility = JavaVersion.VERSION_22

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
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-test:$springVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("io.projectreactor:reactor-test")

    implementation("org.telegram:telegrambots:6.7.0")
    implementation("org.telegram:telegrambots-meta:6.7.0")

    // Spring WebFlux
    implementation("org.springframework:spring-webflux:6.1.5")
    implementation("io.projectreactor.netty:reactor-netty:1.1.13")

    // Spring Security
    implementation("org.springframework.security:spring-security-web:6.2.2")
    implementation("org.springframework.security:spring-security-config:6.2.2")

    // SpringDoc OpenAPI для документации
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.3.0")
    implementation("io.swagger.core.v3:swagger-core:2.2.20")
    implementation("io.swagger.core.v3:swagger-models:2.2.20")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // PlantUML для диаграмм
    implementation("net.sourceforge.plantuml:plantuml:1.2024.3")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-io:1.3.2")
    implementation("org.apache.commons:commons-collections4:4.4")

    // Java AWT для графики
    implementation("org.openjfx:javafx-graphics:21.0.2")
    implementation("org.openjfx:javafx-swing:21.0.2")
}

application {
    mainClass.set("org.tripplanner.Main")
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dspring.profiles.active=local")
}

tasks.register("generateDocs") {
    group = "documentation"
    description = "Generates API documentation and diagrams"
    doLast {
        println("Generating documentation and diagrams...")
    }
}

tasks.test {
    useJUnitPlatform()
    workingDir = project.projectDir
    testLogging {
        events("passed", "skipped", "failed")
    }
}