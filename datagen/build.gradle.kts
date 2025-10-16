plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

dependencies {
    implementation(project(path = ":rainbow", configuration = "namedElements"))
}

loom {
    accessWidenerPath = file("src/main/resources/rainbow-datagen.accesswidener")
}
