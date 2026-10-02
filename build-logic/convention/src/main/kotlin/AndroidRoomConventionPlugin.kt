import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room")
        // Exported schemas are committed: user.db migrations are tested against them and
        // content/pipeline/build_db.py builds content.db from them.
        extensions.configure<RoomExtension> { schemaDirectory("$projectDir/schemas") }
        dependencies {
            add("implementation", libs.lib("androidx-room-runtime"))
            add("implementation", libs.lib("androidx-room-ktx"))
            add("ksp", libs.lib("androidx-room-compiler"))
            add("androidTestImplementation", libs.lib("androidx-room-testing"))
        }
    }
}
