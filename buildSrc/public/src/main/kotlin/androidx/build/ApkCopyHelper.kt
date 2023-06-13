/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.HasAndroidTest
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ApkCopyTask : DefaultTask() {
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkFolder: DirectoryProperty

    @get:Internal
    abstract val apkLoader: Property<BuiltArtifactsLoader>

    @get:OutputFile
    abstract val outputApk: RegularFileProperty

    @TaskAction
    fun copyApk() {
        val destinationApk = outputApk.get().asFile
        val apk = apkLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
        val apkBuiltArtifact = apk.elements.single()
        File(apkBuiltArtifact.outputFile).copyTo(destinationApk, overwrite = true)
    }
}

fun setupAppApkCopy(project: Project, buildType: String) {
    project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType(buildType)) { variant ->
            val apkCopy = project.tasks.register("copyAppApk", ApkCopyTask::class.java) { task ->
                task.apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
                task.apkLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                task.outputApk.set(
                    File(
                        project.getDistributionDirectory(),
                        "apks/${project.path.asFilenamePrefix()}-${variant.name}.apk"
                    )
                )
            }
            project.addToBuildOnServer(apkCopy)
        }
    } ?: throw Exception("Unable to set up app APK copying")
}

fun setupTestApkCopy(project: Project) {
    project.extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            var name: String? = null
            var artifacts: Artifacts? = null
            when {
                variant is HasAndroidTest -> {
                    name = variant.androidTest?.name
                    artifacts = variant.androidTest?.artifacts
                }

                project.plugins.hasPlugin("com.android.test") -> {
                    name = variant.name
                    artifacts = variant.artifacts
                }
            }
            if (name == null || artifacts == null) {
                return@onVariants
            }
            val apkCopy = project.tasks.register("copyTestApk", ApkCopyTask::class.java) { task ->
                task.apkFolder.set(artifacts.get(SingleArtifact.APK))
                task.apkLoader.set(artifacts.getBuiltArtifactsLoader())
                task.outputApk.set(
                    File(
                        project.getDistributionDirectory(),
                        "apks/${project.path.asFilenamePrefix()}-$name.apk"
                    )
                )
            }
            project.addToBuildOnServer(apkCopy)
        }
    }
}

/**
 * Returns a string that is a valid filename and loosely based on the project name
 * The value returned for each project will be distinct
 */
fun String.asFilenamePrefix(): String {
    return this.substring(1).replace(':', '-')
}