plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    // maven { 
    //     url = uri("https://repo.dmulloy2.net/repository/public/")
    // }
    
}

dependencies {
    //compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    implementation("org.json:json:20210307")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}