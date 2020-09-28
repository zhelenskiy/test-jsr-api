import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl
import kotlin.script.experimental.jvmhost.saveToJar
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@Suppress("UNCHECKED_CAST")
fun <T> Any?.to() = this as T

@ExperimentalTime
fun main() {
    val engine = TableScriptEngineFactory().scriptEngine
    heatUp(engine)
    testMultiFile(engine)
    testSpeed(engine)
    testCompileEval(engine) //After each compilation must be evaluation, else state brakes
    testJars()
    testBigImport(engine)
    testSharedObjects(engine)
    testSafety(engine)

}

fun testSharedObjects(engine: KotlinJsr223ScriptEngineImpl) {
    engine.state.history.reset()
    val instance = engine.eval(Files.readString(Paths.get("data/SharedClassUsage.kts")))
    println(instance)
    println(instance::class)
    engine.state.history.reset()
    val checker = engine.eval(Files.readString(Paths.get("data/SharedClassChecker.kts")))
    println(checker)
    println(checker::class)
    println(checker::class.java.methods[0].let { it.isAccessible = true; it.invoke(checker, instance) }) //<<< is false, unfortunately, could use rmi or something like that
    //TODO("Not yet implemented")
}

private fun testCompileEval(engine: KotlinJsr223ScriptEngineImpl) {
    engine.compile("1").eval()
}

@ExperimentalTime
private fun testSpeed(engine: KotlinJsr223ScriptEngineImpl) {
    for (i in 1..20)
        print("${measureTimedValue { engine.eval("2+2") }.also { assert(it.value == 4) }.duration} ")
    println()
}

private fun testMultiFile(engine: KotlinJsr223ScriptEngineImpl) {
    engine.state.history.reset()
    val file = Files.readString(Paths.get("data/2.kts"))
    println(engine.eval(file))
}

@ExperimentalTime
private fun testBigImport(engine: KotlinJsr223ScriptEngineImpl) {
    engine.state.history.reset()
    val file = Files.readString(Paths.get("data/BigImport.kts"))
    println(measureTime { engine.eval(file) })
}

private fun heatUp(engine: KotlinJsr223ScriptEngineImpl) {
    try {
        for (i in 1..5) engine.eval("1")
    } catch (e: Throwable) {
        println(e.message)
    }
}

private fun testJars() {
    //    println("In .jar file:")
//    getModule(engine, file).compilerOutputFiles.keys.forEach(::println)
////    val jar = createTempFile(suffix = ".jar")
//    val jar = File("bad.jar")
//    getCompiledScript(engine, "print(\"loaded from jar\")")
//        /*.also { ((it::class.constructors).forEach { it.parameters.forEach(::println); println()}) }*/
//        .saveToJar(jar)
//    //todo for jars loading look at the IDEA project `simple-kts` because starting jars is simple there and it needs to be the same here too

//    val jarClassLoader = JarClassLoader(jar)
//    println(jarClassLoader.getMainClass()!!.constructors.size)
//    jarClassLoader.getMainClass()!!.constructors.forEach(::println)
//    jarClassLoader.getMainClass()!!.constructors.first().call(ScriptEvaluationConfiguration.jvm.mainArguments)
}

private fun testSafety(engine: KotlinJsr223ScriptEngineImpl) {
    engine.compile("System.exit(1)").apply {
        println(
            SafeExecutor().execute { kotlin.runCatching { eval() } }.exceptionOrNull() ?: throw IllegalStateException()
        )
    }
}

@ExperimentalTime
private fun getModule(engine: KotlinJsr223ScriptEngineImpl, file: String) =
    getCompiledScript(engine, file)
        .getCompiledModule().to<KJvmCompiledModuleInMemory>()

@ExperimentalTime
private fun getCompiledScript(
    engine: KotlinJsr223ScriptEngineImpl,
    file: String
) = measureTimedValue { engine.compile(file) }.also { println(it.duration) }
    .value.to<KotlinJsr223JvmScriptEngineBase.CompiledKotlinScript>()
    .compiledData.data.to<LinkedSnippetImpl<*>>()
    .get().to<KJvmCompiledScript>()