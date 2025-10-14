plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

dependencies {
    // Implement namedElements so IDEs can use it correctly, but include the remapped build
    implementation(project(path = ":rainbow", configuration = "namedElements"))
    include(project(":rainbow"))
}

loom {
    accessWidenerPath = file("src/main/resources/rainbow-datagen.accesswidener")
}
