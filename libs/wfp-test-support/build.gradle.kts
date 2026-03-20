plugins {
    id("wfp.library-conventions")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-test:3.3.5")
    api("org.springframework.security:spring-security-test:6.3.4")
    api("org.springframework.boot:spring-boot-testcontainers:3.3.5")
    api("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
    api("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    api("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    api("org.testcontainers:rabbitmq:${property("testcontainersVersion")}")
    api(project(":libs:wfp-security"))
}
