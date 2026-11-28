// 导入形态：花括号组导入、组内别名、模块别名、通配符、可见性导入与条件导入。
// 官方依据：Cangjie 文档《package 与 import》；cjc 1.0.5 / 1.1.3 零诊断。
package sample.importforms

import std.collection.{ArrayList, HashMap}
import std.collection.{HashSet}
import std.collection.{ArrayList as L, HashMap as M}
import std.{math.sqrt, fs.Path}
import std.collection as col
import std.fs.*
import std.fs.File

@When[os == "Windows"]
import std.fs.File as WinFile

internal import std.collection.{ArrayList as IL}

func useImports(path: Path): Float64 {
    let list = ArrayList<Int64>()
    let plain = col.ArrayList<Int64>()
    let _ = (list, plain)
    return sqrt(2.0)
}
