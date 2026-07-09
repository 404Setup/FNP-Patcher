
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

val embed: Configuration by configurations.creating {
    isTransitive = false
}

version = project.rootProject.property("mod_version") as String
group = project.rootProject.property("maven_group") as String

base {
    archivesName.set(project.rootProject.property("archives_base_name") as String + "-javaagent")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvnc.pkg.one/snapshots") {
        name = "OneSnapshot"
    }
}

dependencies {
    shadow("org.javassist:javassist:3.30.2-GA")
    shadow("org.ow2.asm:asm:9.9")
    shadow("org.ow2.asm:asm-commons:9.9")

    val velocityVersion = "3.4.0-SNAPSHOT"
    embed("one.pkg.velocity_rc:velocity-native:$velocityVersion")
    implementation("one.pkg.velocity_rc:velocity-native:$velocityVersion")
}

val javaAgentManifest = mapOf(
    "Main-Class" to "one.pkg.kreno_fpatcher.PKMain",
    "Premain-Class" to "one.pkg.kreno_fpatcher.PKAgent",
    "Agent-Class" to "one.pkg.kreno_fpatcher.PKAgent",
    "Can-Retransform-Classes" to true,
    "Can-Redefine-Classes" to true,
    "Boot-Class-Path" to ""
)

fun ShadowJar.applyCommonConfig() {
    relocate("org.objectweb.asm", "one.pkg.kreno_fpatcher.relocated.asm")
    relocate("javassist", "one.pkg.kreno_fpatcher.relocated.javassist")
}

val targetJavaVersion = 17
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
    from(embed) {
        into("META-INF/jars")
    }
    manifest.attributes(javaAgentManifest)
}

tasks.shadowJar {
    isZip64 = true
    configurations = listOf(project.configurations.shadow.get())
    archiveClassifier.set("dev")
    applyCommonConfig()
}

tasks.register<ShadowJar>("finallyShadowJar") {
    archiveClassifier.set("finally")
    configurations = listOf(project.configurations.shadow.get())
    from(zipTree(tasks.jar.flatMap { it.archiveFile }))
    applyCommonConfig()
    manifest.attributes(javaAgentManifest)
}

tasks.build {
    finalizedBy("finallyShadowJar")
}
