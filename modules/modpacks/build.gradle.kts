dependencies {
    implementation("com.simibubi.create:create-1.21.1:6.0.10-272:slim") {
        exclude("info.journeymap")
        exclude("mezz.jei")
    }
    implementation("com.gregtechceu.gtceu:gtceu-1.21.1:7.0.2-SNAPSHOT")
    compileOnly("mezz.jei:jei-1.21.1-neoforge:19.27.0.340")
    localRuntime("mezz.jei:jei-1.21.1-neoforge:19.27.0.340")
    implementation(project(":core"))
    localRuntime("dev.ftb.mods:ftb-quests-neoforge:2101.1.24")
    localRuntime("curse.maven:commandstructures-565119:6428806")
    implementation("com.github.glitchfiend:TerraBlender-neoforge:1.21.1-21.10.0.0")
//    implementation("curse.maven:simply-improved-terrain-472872:6112229")
}