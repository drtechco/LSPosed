plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.google.libdandroid.service"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        buildConfig = false
        resValues = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    sourceSets {
        getByName("main") {
            aidl {
                srcDirs("src/main/aidl")
            }
        }
    }

    buildFeatures {
        aidl = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(project(":libdandroidapi"))
    compileOnly("androidx.annotation:annotation:1.7.1")
}

publishing {
    publications {
        register<MavenPublication>("service") {
            artifactId = "service"
            group = "com.google.libdandroid"
            version = "100-1.0.0"
            pom {
                name.set("service")
                description.set("Modern DAndroid Service Interface")
                url.set("https://github.com/libdandroid/service")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/libdandroid/service/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("libdandroid")
                        url.set("https://libdandroid.github.io")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/libdandroid/service.git")
                    url.set("https://github.com/libdandroid/service")
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
            url = uri("https://maven.pkg.github.com/libdandroid/service")
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
