package com.aliucord.manager.patcher.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.steps.download.DownloadPatchesStep
import com.android.tools.smali.baksmali.Baksmali
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.smali.Smali
import com.android.tools.smali.smali.SmaliOptions
import com.github.diamondminer88.zip.ZipReader
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileNotFoundException

class SmaliPatchStep : Step(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()

    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_smali

    private val coreCount = Runtime.getRuntime().availableProcessors()
    private val smaliDir = paths.patchingWorkingDir().resolve("smali")
    private val outDex = smaliDir.resolve("patched.dex")

    override suspend fun execute(container: StepRunner) {
        val apk = container.getStep<CopyDependenciesStep>().patchedApk
        val patchesZip = container.getStep<DownloadPatchesStep>().targetFile

        val patches = mutableListOf<LoadedPatch>()

        // Load and parse all the patches from the smali patch archive
        container.log("Loading patches from smali patch archive: ${patchesZip.absolutePath}")
        ZipReader(patchesZip).use { zip ->
            for (patchFile in zip.entryNames) {
                container.log("Parsing patch file $patchFile")
                if (!patchFile.endsWith(".patch")) continue

                val lines = zip.openEntry(patchFile)!!.read()
                    .decodeToString()
                    .replace("\r\n", "\n") // Replace CRLF endings with LF endings to be sure here
                    .trimEnd { it == '\n' } // Remove trailing new lines to work with diff output properly
                    .split('\n')

                try {
                    val patch = LoadedPatch(
                        fullClassName = patchFile.removeSuffix(".patch"),
                        patch = UnifiedDiffUtils.parseUnifiedDiff(lines),
                    )
                    patches.add(patch)
                    container.log("Loaded patch file for class ${patch.fullClassName}")
                } catch (t: Throwable) {
                    throw Error("Failed to parse patch file $patchFile", t)
                }
            }
        }

        // Disassemble all the classes we have patches for from all the dex files
        container.log("Disassembling target classes in APK")
        ZipReader(apk).use { zip ->
            for (file in zip.entryNames) {
                if (!file.endsWith(".dex")) continue
                container.log("Disassembling dex $file")

                val dexFile = try {
                    DexBackedDexFile(
                        /* opcodes = */ Opcodes.getDefault(),
                        /* buf = */ zip.openEntry(file)!!.read(),
                    )
                } catch (t: Throwable) {
                    throw Error("Failed to parse dex $file", t)
                }

                val result = try {
                    Baksmali.disassembleDexFile(
                        /* dexFile = */ dexFile,
                        /* outputDir = */ smaliDir,
                        /* jobs = */ coreCount - 1,
                        /* options = */ BaksmaliOptions().apply { localsDirective = true },
                        /* classes = */ patches.map { "L${it.fullClassName};" },
                    )
                } catch (t: Throwable) {
                    throw Error("Failed to disassemble dex $file", t)
                }

                assert(result) { "Failed to disassemble dex $file (unknown reason)" }
                container.log("Disassembled dex file for potential target classes")
            }
        }

        // Apply all the patches to the smali files
        container.log("Applying smali patches to disassembled files")
        for ((fullClassName, patch) in patches) {
            container.log("Applying patch to class $fullClassName")

            val smaliFile = smaliDir.resolve("$fullClassName.smali")
            if (!smaliFile.exists()) {
                throw FileNotFoundException("Target smali file $fullClassName.smali not found for patching!")
            }

            val patched = try {
                DiffUtils.patch(smaliFile.readLines(), patch)
            } catch (t: Throwable) {
                throw Error("Failed to smali patch $fullClassName", t)
            }

            smaliFile.bufferedWriter().use { writer ->
                patched.forEach(writer::appendLine)
            }
        }

        // Assemble the patched classes back into a single dex
        container.log("Reassembling patches smali classes into new dex")
        smaliDir.mkdir()
        Smali.assemble(
            SmaliOptions().apply {
                this.jobs = coreCount - 1
                this.outputDexFile = outDex.absolutePath
            },
            listOf(smaliDir.absolutePath),
        )
    }

    override val dexPriority = 2
    override val dexCount = 1
    override fun getDexFiles() = listOf(outDex.readBytes())
}

private data class LoadedPatch(
    val fullClassName: String,
    val patch: Patch<String>,
)
