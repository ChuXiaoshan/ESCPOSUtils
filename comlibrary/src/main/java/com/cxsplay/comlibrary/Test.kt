package com.cxsplay.comlibrary

/**
 * Created by chuxiaoshan on 2023/12/31 01:43
 *
 * Description:
 */
object Test {

    fun test(): ByteArray {
        return """
        "Hello World!"
        "Hello World!"
        "Hello World!"
        "Hello World!"
        "Hello World!"
        "Hello World!"
        """.trimIndent()
            .toByteArray()
    }
}