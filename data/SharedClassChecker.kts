@file:Import("data/SharedClass.kts")
val a = { input: Any? -> input is SharedClass.TestClass }
a