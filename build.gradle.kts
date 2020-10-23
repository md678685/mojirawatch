plugins {
    application
}

group = "io.github.md678685"
version = "0.1-SNAPSHOT"

application {
    mainClass.set("io.github.md678685.mojirawatch.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.8.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("org.kitteh.irc:client-lib:7.3.0")
    implementation("org.spongepowered:configurate-hocon:3.7.1")
    implementation("org.slf4j:slf4j-api:1.7.30")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")
}
