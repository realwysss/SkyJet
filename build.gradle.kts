plugins {
    id("dev.kikugie.loom-back-compat")
}

val modId = property("mod.id") as String
val mcVersion = stonecutter.current.version
val javaVersion = (property("mod.java") as String).toInt()

version = "${property("mod.version")}+$mcVersion"
base.archivesName = modId

repositories {
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    // Compile only: the Mod Menu entrypoint is never loaded when Mod Menu is absent, so the
    // integration class costs nothing and the jar stays free of a hard dependency.
    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    toolchain { languageVersion = JavaLanguageVersion.of(javaVersion) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.encoding = "UTF-8"
}

val templateProperties = mapOf(
    "id" to modId,
    "name" to property("mod.name") as String,
    "version" to project.version.toString(),
    "description" to property("mod.description") as String,
    "mc_dep" to property("mod.mc_dep") as String,
    "java_dep" to ">=" + property("mod.java"),
    // Declared floor is deliberately independent of the loader we compile against, so the
    // jar installs on older loaders instead of demanding whatever was newest at build time.
    "loader_dep" to ">=" + property("mod.loader_dep")
)

tasks.processResources {
    inputs.properties(templateProperties)
    filesMatching("fabric.mod.json") { expand(templateProperties) }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "skyjet-test"
        enableGameTests = false
        eula = true
    }
}
