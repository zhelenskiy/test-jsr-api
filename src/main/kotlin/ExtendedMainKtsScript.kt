import org.jetbrains.kotlin.mainKts.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223

@KotlinScript(displayName = "TableScript", fileExtension = "table.kts",
    hostConfiguration = TableScriptHostConfiguration::class,
    compilationConfiguration = TableCompilationConfiguration::class,
    evaluationConfiguration = MainKtsEvaluationConfiguration::class
)
abstract class ExtendedMainKtsScript(val args: Array<String> = emptyArray())
object TableScriptHostConfiguration : ScriptingHostConfiguration({
    jvm {
        baseClassLoader(ClassLoader.getSystemClassLoader())
        jdkHome
        val dir = Files.createDirectories(Path.of(".cache")).toFile()
        compilationCache(CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
            File(dir, compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar")
        })
    }
})

private fun compiledScriptUniqueName(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("SHA-256")

    fun addToDigest(chunk: String) = with(digestWrapper) {
        val chunkBytes = chunk.toByteArray()
        update(chunkBytes.size.toByteArray())
        update(chunkBytes)
    }

    digestWrapper.update(COMPILED_SCRIPTS_CACHE_VERSION.toByteArray())
    addToDigest(script.text)
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            addToDigest(it.key.name)
            addToDigest(it.value.toString())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES).also { it.putInt(this) }.array()

class AnnotationsHandler(
    vararg val annotations: KClass<out Annotation>,
    val handler: RefineScriptCompilationConfigurationHandler
) {
    operator fun component1() = annotations
    operator fun component2() = handler
}

var additionalAnnotationsHandler = arrayOf<AnnotationsHandler>(
)

var additionalDependencies = arrayOf<ScriptDependency>(
)

class TableCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class,
        *additionalAnnotationsHandler.flatMap { it.annotations.asIterable() }.toTypedArray())
    jvm {
        dependenciesFromClassContext(
            TableCompilationConfiguration::class,
            "kotlin-main-kts", "kotlin-stdlib", "kotlin-reflect"
        )
        if (additionalDependencies.isNotEmpty()) {
            dependencies.append(*additionalDependencies)
        }
    }
    refineConfiguration {
        onAnnotations(
            DependsOn::class, Repository::class, Import::class, CompilerOptions::class,
            handler = MainKtsConfigurator()
        )
        beforeCompiling(::configureProvidedPropertiesFromJsr223Context)
        for ((annotations, handler) in additionalAnnotationsHandler) {
            onAnnotations(*annotations, handler = handler)
        }
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
    jsr223 {
        importAllBindings(true)
    }
})