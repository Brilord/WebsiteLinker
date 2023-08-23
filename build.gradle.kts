plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.openjfx:javafx-controls:16") // Replace 16 with the version you are using
    implementation("org.openjfx:javafx-graphics:16") // Replace 16 with the version you are using
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    // Add more JavaFX dependencies if needed

    // Other dependencies
    // Add more JavaFX dependencies if needed

    // Other dependencies

    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp

    implementation("org.json:json:20210307")
    implementation("com.squareup.okhttp3:okhttp:4.9.1") // Use the latest version


}

tasks.test {
    useJUnitPlatform()
}