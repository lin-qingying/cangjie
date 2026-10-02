// RUN_PIPELINE_TILL: FRONTEND
// 导入组（花括号多导入 / 选择式组导入）形态：
//   1. 多项组 import a.{b, c}；单项组 import a.{b}
//   2. 组内每一项都可 as 别名，别名在使用处生效
//   3. 组内项可带点号前缀（选择式组导入）import std.{math.sqrt, fs.Path}
//   4. 组内三项以上，组尾可带逗号
//   5. 组 import 与普通 import 混用
//   6. @When 条件编译注解可作用于组 import，注解随 DesugarImportMulti 复制到每一项
//   7. 可见性修饰符 internal 可作用于组 import
// 官方依据：src/Parse/ParseImports.cpp:216 ParseImportSingle（遇 LCURL 转多导入）、
//   :262 ParseImportMulti（组内逐项解析、逗号分隔、尾逗号）、:288-290 组内 as 别名、
//   :146 DesugarImportMulti（组展开为多个 ImportSpec，复制修饰符与注解）、
//   :323 CheckAllowedAnnoOnImport（import 上仅允许 @When）。
// cjc 1.0.5 / 1.1.3 零诊断。
package importgroupforms

// 多项组
import std.collection.{ArrayList, HashMap}
// 单项组
import std.collection.{HashSet}
// 组内 as 别名
import std.collection.{ArrayList as L, HashMap as M}
// 组内点号前缀：选择式组导入
import std.{math.sqrt, fs.Path}
// 三项组 + 尾逗号
import std.collection.{ArrayList as TA, HashMap as TB, HashSet as TC,}
// 组 import 与普通 import 混用
import std.fs.File
// @When 作用于组 import（连同组内别名）
@When[os == "Windows"]
import std.collection.{ArrayList as WL, HashMap as WM}
// 可见性修饰符 internal 作用于组 import
internal import std.collection.{ArrayList as IL}

func useImportGroups(path: Path): Float64 {
    let plain = ArrayList<Int64>()
    let grouped = HashMap<String, Int64>()
    let one = HashSet<Int64>()
    let aliased = L<Int64>()
    let aliasedMap = M<String, Int64>()
    let comma = TA<Int64>()
    let commaMap = TB<String, Int64>()
    let commaSet = TC<Int64>()
    let winAliased = WL<Int64>()
    let winAliasedMap = WM<String, Int64>()
    let internalAliased = IL<Int64>()
    let fi = File.create("./t.txt")
    let _ = fi
    plain.add(grouped.size + one.size + aliased.size + aliasedMap.size)
    plain.add(comma.size + commaMap.size + commaSet.size + path.toString().size)
    plain.add(winAliased.size + winAliasedMap.size + internalAliased.size)
    return sqrt(Float64(plain.size))
}
