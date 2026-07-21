plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature:recorder"))
    implementation(project(":feature:storage"))
    implementation(project(":feature:crypto"))
    implementation(project(":feature:mesh"))
    implementation(project(":feature:relay"))

    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("com.meshstream.app.MeshStreamAppKt")
}
