# 仓颉补全与 Kotlin K2 的语义差异表

> 维护要求（tasks.md 11.3）：**按官方语义维护**。
> 每条差异必须附证据来源；没有证据的猜测不得写进本表。

本表记录**补全实现必须区别对待**的差异。Kotlin 侧行为一律以
`external/kotlin` 的 K2 实现为准；仓颉侧以官方 `cjc` 实测或官方手册为准。
本变更**不承诺** Kotlin 全语言特性的逐项等价，只记录影响候选生成/过滤/插入的差异。

| # | 维度 | Kotlin K2 | 仓颉 | 证据 |
|---|---|---|---|---|
| 1 | **命名实参前缀** | `name = value` | `name: value` | `cjc 1.1.3` 实测；实现见 `namedArgumentSnapshot`（前缀由签名固定，调用方无法传分隔符，因此写不出等号形式） |
| 2 | **类型别名关键字** | `typealias A = B` | `type A = B` | `cjc 1.1.3` 实测：`typealias` 报 `expected declaration, found 'typealias'`；`type` 通过（仅缺 `main`） |
| 3 | **枚举同名不同 payload 个数** | 不允许 | **允许**：`\| Dot` 与 `\| Dot(Int64)` 并存 | `cjc 1.1.3` 实测合法；因此去重必须按「名字 + payload 签名」，见 9.6 |
| 4 | **文档注释主语** | KDoc `@param x`（裸名） | CDoc `@param [x]`（方括号里是**链接**） | 仓内夹具 `analysis-api/testData/components/docProvider/cdoc/cdoc.cj`；官方手册《Tokens 相关类型和 quote 表达式》 |
| 5 | **文档标签集合** | KDoc 标签表 | `CDocKnownTag` 的 11 个（AUTHOR/THROWS/EXCEPTION/PARAM/RETURN/SEE/SINCE/CONSTRUCTOR/PROPERTY/SAMPLE/SUPPRESS） | 仓内 `psi/.../lexer/cdoc/parser/CDocKnownTag.kt`（仓颉自己的词法解析器定义），**不抄 KDoc** |
| 6 | **主构造器书写** | 类头 `class C(val x: Int)` | 类头**只能无参**，带参必须用类体 `init(...)` | `cjc 1.1.3` 实测：`class Box<T>(value: T)` 报 `expected '{', found '('` |
| 7 | **静态成员** | `companion object { }` | `static` 修饰符：`public static func size()` | `cjc 1.1.3` 实测；类型限定访问只走静态作用域（见 #8） |
| 8 | **类型限定访问的作用域** | 视成员形态分别判定 | `Type.member` 走**静态**成员作用域，实例成员在那里找不到 | 实测：夹具用实例方法时「恢复成功但候选为空」，改静态后通过 |
| 9 | **import 的等价写法** | 包级与声明级语义不同 | `import a.b`、`import a.b.c`、`import a.b.*` **三者都能**直接引用 `c()` | `cjc 1.1.3` 按目录布局实测；因此「是否已导入」必须结构化判定，不能逐行比文本（见 10.5） |
| 10 | **模式匹配** | `when (x) { is T -> }` | `match (x) { case p => }`，模式位置需要枚举构造器候选 | `cjc 1.1.3` 实测；实现见 11.3 的 match 分支 |
| 11 | **关键字转义标识符** | 反引号 | 反引号 | 一致；但**判据仍须用 PSI 类型**：按文本判会把转义的 `super` 当关键字（见 11.3） |

## 未列入本表的项

- **尾随 lambda**：仓颉有对应语法糖（官方手册《函数调用语法糖》），**不是**差异，故不列。
- **KDoc 专属规则**（`@property`、`@sample`、`$name` 插值等）：仓颉 CDoc 无对应能力，
  实现里**不复制**；它们不是「差异」而是「不适用」，按变更纪律不逐条登记。
- **Java/JVM 互操作相关**：不属于本次对齐范围。

## 维护规则

新增一条差异必须同时给出：**证据来源**（cjc 实测命令/输出，或官方手册篇名，或仓内文件路径）
与**受影响的实现点**。只写「Kotlin 是这样、仓颉是那样」而没有证据的条目，
会在下一次有人按 Kotlin 习惯改回来时失去约束力。
