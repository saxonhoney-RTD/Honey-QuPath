plugins {
    // Support writing the extension in Groovy (remove this if you don't want to)
    groovy
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
}

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "Honey-Executer"
    group = "io.github.qupath"
    version = "0.3.0"
    description = "Honey's 984th try"
    automaticModule = "io.github.qupath.extension.template"
}

// TODO: Define your dependencies here
dependencies {

    // Main dependencies for most QuPath extensions
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    // If you aren't using Groovy, this can be removed
    shadow(libs.bundles.groovy)

    // For testing
    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)

}

// Force Java 21 toolchain & bytecode (required for QuPath 0.6.0 artifacts)
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
tasks.withType<org.gradle.api.tasks.compile.GroovyCompile>().configureEach {
    groovyOptions.encoding = "UTF-8"
    options.release.set(21)
}