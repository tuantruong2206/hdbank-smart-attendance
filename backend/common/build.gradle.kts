plugins {
    `java-library`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    api("jakarta.validation:jakarta.validation-api:3.1.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // For HeaderAuthFilter and MdcFilter (servlet + spring-security + spring-web + slf4j)
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
    compileOnly("org.springframework.security:spring-security-core:6.3.4")
    compileOnly("org.springframework.security:spring-security-web:6.3.4")
    compileOnly("org.springframework:spring-web:6.1.14")
    compileOnly("org.slf4j:slf4j-api:2.0.16")
}
