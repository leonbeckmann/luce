package policies.luce_lang

import org.junit.jupiter.api.Test
import java.io.File

internal class LuceLangTest {

    @Test
    fun testPolicyDeserialization() {
        val path = javaClass.classLoader.getResource("policies/policy1.json")!!.path
        val str = File(path).inputStream().readBytes().toString(Charsets.UTF_8)
        val json = LuceLang.deserialize(str)
        println(json)
    }

}