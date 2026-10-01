// RUN_PIPELINE_TILL: FRONTEND
// 包声明（根包 / 多级子包）与 import 形态：通配符 *、别名 as、选择性导入、导入组 import (a.b, c.d)、@When 作用在 import 上。
// 官方依据：Cangjie 文档《包的声明》《导入声明》；cjc 1.0.5 / 1.1.3 零诊断（未使用的 import 会有 warning，不是 error）。
package sample.feature.sub

import std.collection.*
import std.collection as col
import std.fs.File
import std.fs.Path

@When[os == "Windows"]
import std.fs.File as WinFile

func importUses(xs: ArrayList<Int64>): Int64 {
    let list = ArrayList<Int64>()
    list.add(xs.size)
    return list.size
}
