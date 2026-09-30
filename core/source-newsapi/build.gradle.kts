// Plugin module: depends only on the extension interfaces (architecture Section 6.2).
plugins {
    id("alerting.java-library-conventions")
}

dependencies {
    implementation(project(":alerting-spi"))
}
