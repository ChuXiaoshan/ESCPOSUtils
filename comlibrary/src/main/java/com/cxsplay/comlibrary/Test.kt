package com.cxsplay.comlibrary

import java.nio.charset.Charset

/**
 * Created by chuxiaoshan on 2023/12/31 01:43
 *
 * Description:
 */
object Test {

    fun test(): ByteArray {
        return """
        你好! Hello World!
        你好! Hello World!
        你好! Hello World!
        你好! Hello World!
        你好! Hello World!
        你好! Hello World!
        """.trimIndent()
            .toByteArray(Charset.forName("gb2312"))
    }
}