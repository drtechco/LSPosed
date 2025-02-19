plugins {
    alias(libs.plugins.agp.lib)
    `maven-publish`
    signing
}

android {
    namespace = "com.google.libdandroid.api"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("proguard-rules.pro")
    }

    buildFeatures {
        androidResources = false
        buildConfig = false
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("api") {
            artifactId = "api"
            group = "com.google.libdandroid"
            version = "100"
            pom {
                name.set("api")
                description.set("Modern DAndroid API")
                url.set("https://github.com/libdandroid/api")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/libdandroid/api/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("libdandroid")
                        url.set("https://libdandroid.github.io")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/libdandroid/api.git")
                    url.set("https://github.com/libdandroid/api")
                }
            }
            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/libdandroid/api")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}


dependencies {
    compileOnly(libs.androidx.annotation)
}
