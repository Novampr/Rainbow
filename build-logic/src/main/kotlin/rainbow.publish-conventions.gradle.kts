plugins {
    `maven-publish`
}

val archivesBaseName = properties["archives_base_name"]!! as String

publishing {
    repositories {
        maven {
            name = "eclipseisoffline"
            url = uri(
                when {
                    version.toString().endsWith("-SNAPSHOT") -> "https://maven.eclipseisoffline.xyz/snapshots"
                    else -> "https://maven.eclipseisoffline.xyz/releases"
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
