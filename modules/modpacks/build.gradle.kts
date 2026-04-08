dependencies {
    implementation("com.simibubi.create:create-1.21.1:6.0.10-272:slim") {
        exclude("info.journeymap")
        exclude("mezz.jei")
    }
    implementation("com.gregtechceu.gtceu:gtceu-1.21.1:7.0.2-SNAPSHOT")
    compileOnly("mezz.jei:jei-1.21.1-neoforge:19.27.0.340")
    localRuntime("mezz.jei:jei-1.21.1-neoforge:19.27.0.340")

}