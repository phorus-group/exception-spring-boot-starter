import com.kageiit.jacobo.JacoboTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.net.URI
import java.time.LocalDate

plugins {
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.spring") version "2.3.10"
    kotlin("jvm") version "2.3.10"
    id("org.jetbrains.dokka").version("2.1.0")
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("com.kageiit.jacobo") version "2.1.0"
    jacoco
}

ext["jackson-2-bom.version"] = "2.21.1"
ext["jackson-bom.version"] = "3.1.0"

group = "group.phorus"
description = "Spring Boot WebFlux autoconfiguration for the Phorus exception handling library."
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Phorus
    api("group.phorus:exception-core:1.0.0")
    implementation("group.phorus:metrics-commons:2.0.3")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework:spring-tx")

    // OpenAPI (optional, only active when springdoc is on classpath)
    compileOnly("org.springdoc:springdoc-openapi-starter-webflux-api:3.0.2")

    // Kotlin
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springdoc:springdoc-openapi-starter-webflux-api:3.0.2")
    testImplementation("org.springframework.boot:spring-boot-webtestclient")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java:7.34.2")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.34.2")
    testImplementation("io.cucumber:cucumber-spring:7.34.2")
}

val repoUrl = System.getenv("GITHUB_REPOSITORY")?.let { "https://github.com/$it" }
    ?: "https://github.com/phorus-group/exception-spring-boot-starter"

tasks {
    getByName<BootJar>("bootJar") {
        enabled = false
    }

    jacocoTestReport {
        executionData.setFrom(fileTree(project.layout.buildDirectory).include("/jacoco/*.exec"))

        classDirectories.setFrom(
            sourceSets.main.get().output.classesDirs.map { dir ->
                fileTree(dir).exclude(
                    "**/model/**",
                    "**/dtos/**",
                    "**/config/**",
                    "**/repositories/**",
                    "**/*Application*",
                )
            }
        )


        reports {
            xml.required.set(true)
            csv.required.set(true)
        }

        finalizedBy("jacobo")
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    register<JacoboTask>("jacobo") {
        description = "Transforms jacoco xml report to cobertura"
        group = "verification"

        jacocoReport = file("${project.layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        coberturaReport = file("${project.layout.buildDirectory.get()}/reports/cobertura/cobertura.xml")
        includeFileNames = emptySet()

        val field = JacoboTask::class.java.getDeclaredField("srcDirs")
        field.isAccessible = true
        field.set(this, sourceSets["main"].allSource.srcDirs.map { it.path }.toTypedArray())

        dependsOn(jacocoTestReport)
    }

    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.fromTarget(java.targetCompatibility.toString()))
        }
    }

    dokka {
        val branch = System.getenv("GITHUB_REF_NAME") ?: "main"
        val currentYear = LocalDate.now().year

        dokkaPublications.html {
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        }

        dokkaSourceSets.configureEach {
            reportUndocumented.set(true)
            jdkVersion.set(java.targetCompatibility.majorVersion.toInt())
            sourceRoots.from(file("src"))

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URI("$repoUrl/tree/$branch/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }

        pluginsConfiguration.html {
            footerMessage.set("© $currentYear Phorus Group - Licensed under the <a target=\"_blank\" href=\"$repoUrl/blob/$branch/LICENSE\">Apache 2 license</a>.")
        }
    }
}

afterEvaluate {
    tasks.named("generateMetadataFileForMavenPublication") {
        dependsOn("dokkaJavadocJar")
    }
}


mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString()
    )

    pom {
        name.set(project.name)
        description.set(project.description ?: "")
        url.set(repoUrl)

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("irios.phorus")
                name.set("Martin Rios")
                email.set("irios@phorus.group")
                organization.set("Phorus Group")
                organizationUrl.set("https://phorus.group")
            }
        }

        scm {
            url.set(repoUrl)
            connection.set("scm:git:$repoUrl.git")
            developerConnection.set("scm:git:$repoUrl.git")
        }
    }

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
