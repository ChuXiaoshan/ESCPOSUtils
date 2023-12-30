package com.cxsplay.comlibrary

/**
 * Created by chuxiaoshan on 2023/12/31 01:51
 *
 * Description: ESC/POS 指令工具
 */
object Com {

    /**
     * 初始化打印机
     * ASCII    [ESC, @]
     * Hex      [1B, 40]
     * Decimal  [27, 64]
     */
    fun init() = byteArrayOf(27, 64)

    /**
     * 设置字体大小、加粗、下划线
     * 下划线和粗细指令：  Decimal [27, 33, n]
     * 宽高指令：        Decimal [29, 33, n]
     * 字体：[height] 区间在1-8之间；1-正常大小，其他-放大相应倍数；
     * 字体：[width] 区间在1-8之间；1-正常大小，其他-放大相应倍数；
     * [underline] true-加下划线，false-正常；
     * [bold] true-加粗，false-正常。
     */
    fun setFontSize(
        height: Int = 1,
        width: Int = 1,
        underline: Boolean = false,
        bold: Boolean = false,
    ): ByteArray {

        // 加粗 - 00001000
        var mode = (if (bold) 1 else 0) shl 3

        // 下划线 - 10000000，加粗和下划线 - 10001000
        mode = mode or ((if (underline) 1 else 0) shl 7)

        // 宽高控制：wwwwhhhh，8位，前h位置控制高度，w位置控制宽度
        val size = (height - 1).coerceAtLeast(0) or (width.coerceAtMost(7) shl 4)

        return byteArrayOf(27, 33, mode.toByte(), 29, 33, size.toByte())
    }
}