plugins {
    `maven-publish`
}

val archivesBaseName = properties["archives_base_name"]!! as String

publishing {
    repositories {
        maven {
            name = "geysermc"
            url = uri(
                when {
                    version.toString().endsWith("-SNAPSHOT") -> "https://repo.opencollab.dev/maven-snapshots"
                    else -> "https://repo.opencollab.dev/maven-releases"
                }
            )
            credentials(PasswordCredentials::class)
        }
    }

    publications {
        register("publish", MavenPublication::class) {
            artifactId = archivesBaseName
            from(project.components["java"])
        }
    }
}
