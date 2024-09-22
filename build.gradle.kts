plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "com.homo.tadokoro"
version = "1.14.514"

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-proxy:3.4.0-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}