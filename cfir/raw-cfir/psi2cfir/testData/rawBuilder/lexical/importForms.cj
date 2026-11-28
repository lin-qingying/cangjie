// RUN_PIPELINE_TILL: FRONTEND
// import 的全部形态：通配 `*`、选择式（`包.符号`）、别名（`包 as 名`）、core / math / time / sort / regex / env / collection 各包与同名别名。
// 官方依据：Cangjie 文档《包管理》《import》；官方 Parse/ParseImports.cpp；cjc 1.0.5 / 1.1.3 零诊断。
import std.collection.*
import std.collection as c1
import std.core.*
import std.core as c2
import std.math.*
import std.time.*
import std.sort.*
import std.regex.*
import std.env.*

func importForms(xs: ArrayList<Int64>): Int64 {
    return xs.size
}
