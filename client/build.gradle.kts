plugins {
    id("rainbow.base-conventions")
}

dependencies {
    // Implement namedElements so IntelliJ can use it correctly, but include the remapped build
    implementation(project(path = ":rainbow", configuration = "namedElements"))
    include(project(":rainbow"))
}
