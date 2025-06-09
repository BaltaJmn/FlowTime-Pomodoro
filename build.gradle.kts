allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

tasks.named("testDebugUnitTest") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))
}