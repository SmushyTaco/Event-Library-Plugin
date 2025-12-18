import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}

val name = providers.gradleProperty("name")
val projectGroup = providers.gradleProperty("group")
val projectVersion = providers.gradleProperty("version")
val projectDescription = providers.gradleProperty("description")

val publishingUrl = providers.gradleProperty("url")

val javaVersion = libs.versions.java.map { it.toInt() }

base.archivesName = name
group = projectGroup.get()
version = projectVersion.get()
description = projectDescription.get()

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.platform)
        bundledPlugins(providers.gradleProperty("platform_bundled_plugins").map { it.split(',') })
        plugins(providers.gradleProperty("platform_plugins").map { it.split(',') })
    }
}
intellijPlatform {
    pluginConfiguration {
        name = name
        version = projectVersion
        description = "An IntelliJ plugin for my high performance event system"
        changeNotes = ""
        ideaVersion { sinceBuild = providers.gradleProperty("plugin_since_build") }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = projectVersion.map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}
java {
    toolchain {
        languageVersion = javaVersion.map { JavaLanguageVersion.of(it) }
        vendor = JvmVendorSpec.ADOPTIUM
    }
    sourceCompatibility = JavaVersion.toVersion(javaVersion.get())
    targetCompatibility = JavaVersion.toVersion(javaVersion.get())
}
val licenseFile = run {
    val rootLicense = layout.projectDirectory.file("LICENSE")
    val parentLicense = layout.projectDirectory.file("../LICENSE")
    when {
        rootLicense.asFile.exists() -> {
            logger.lifecycle("Using LICENSE from project root: {}", rootLicense.asFile)
            rootLicense
        }
        parentLicense.asFile.exists() -> {
            logger.lifecycle("Using LICENSE from parent directory: {}", parentLicense.asFile)
            parentLicense
        }
        else -> {
            logger.warn("No LICENSE file found in project or parent directory.")
            null
        }
    }
}
tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.get().toString()
        targetCompatibility = javaVersion.get().toString()
        if (javaVersion.get() > 8) options.release = javaVersion
    }
    named<UpdateDaemonJvm>("updateDaemonJvm") {
        languageVersion = libs.versions.gradleJava.map { JavaLanguageVersion.of(it.toInt()) }
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
    withType<Test>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            extraWarnings = true
            jvmTarget = javaVersion.map { JvmTarget.valueOf("JVM_${if (it == 8) "1_8" else it}") }
        }
    }
    withType<Jar>().configureEach {
        licenseFile?.let {
            from(it) {
                rename { original -> "${original}_${archiveBaseName.get()}" }
            }
        }

    }
    wrapper {
        gradleVersion = libs.versions.gradle.get()
    }
}
