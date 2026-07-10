plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String
var configApiVersion = project.property("config_api_version") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            excludeModule("one.pkg", "sewlia-config")
        }
    }
    maven("https://api.modrinth.com/maven")
    maven("https://mvnc.pkg.one/snapshots")
    maven("https://mvnc.pkg.one/releases")
    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("one.pkg.libsl:fabric:1.1.0+26.2")
    implementation("maven.modrinth:krypton:0.3.0")
    implementation("maven.modrinth:modmenu:20.0.1")
    implementation("org.yaml:snakeyaml:2.5")
    implementation("one.pkg:sewlia-config:$configApiVersion") {
        exclude(group = "org.yaml")
        exclude(group = "org.slf4j")
    }
    implementation("one.pkg:tiny-utils:2.4.0")

    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")!!,
            "loader_version" to project.property("loader_version")!!
        )
    }
}

val targetJavaVersion = 25
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    repositories {
    }
}