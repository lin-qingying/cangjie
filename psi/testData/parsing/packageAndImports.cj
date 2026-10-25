// 包声明与导入：package、单类型导入、通配符导入、别名导入与导入分组。
// 官方依据：Cangjie 文档《package 与 import》；cjc 1.0.5 / 1.1.3 零诊断。
package sample.parsing

import sample.lib.RemoteAlias
import sample.lib.echo
import sample.lib.*
import sample.lib.echo as shout

func useImports(): Int64 {
    return echo()
}
