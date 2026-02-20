plugins {
    id("java")
}

group = "kuzminov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.75")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.75")
}

tasks.test {
    useJUnitPlatform()
}