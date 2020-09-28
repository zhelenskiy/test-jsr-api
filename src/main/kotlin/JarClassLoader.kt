import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Attributes
import kotlin.reflect.KClass


class JarClassLoader(val url: URL, parentClassLoader: ClassLoader = getSystemClassLoader()) :
    URLClassLoader(arrayOf(url), parentClassLoader) {

    constructor(jar: File, parentClassLoader: ClassLoader = getSystemClassLoader()) :
            this(URL("jar","", jar.toURI().toURL().toString() + "!/"), parentClassLoader)

    @Throws(IOException::class)
    fun getMainClassName(): String? {
        val u = URL("jar", "", url.file)
        val uc: JarURLConnection = u.openConnection() as JarURLConnection
        return uc.mainAttributes?.getValue(Attributes.Name.MAIN_CLASS)
    }

    @Throws(IOException::class)
    fun getMainClass(): KClass<*>? = loadClass(getMainClassName())?.kotlin

    @Throws(ClassNotFoundException::class, NoSuchMethodException::class, InvocationTargetException::class)
    fun invokeClass(name: String?, args: Array<String?>) {
        val c = loadClass(name, true)
        val m: Method = c.getMethod("main", args.javaClass)
        m.isAccessible = true
        val mods: Int = m.modifiers
        if (m.returnType !== Void.TYPE || !Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw NoSuchMethodException("main")
        }
        try {
            m.invoke(null, args)
        } catch (e: IllegalAccessException) {
            // This should not happen, as we have disabled access checks
        }
    }
}