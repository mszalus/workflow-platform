plugins {
    id("wfp.spring-boot-app")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-mvc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":libs:wfp-common"))
    implementation(project(":libs:wfp-security"))
}
