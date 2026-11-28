# 默认 constructor：qualified / raw / metadata / Mirror 原稿

本目录只新增独立 CJ 原稿与官方取证附件，没有修改原 SOURCE、正确 fixture 或旧期望；没有新增 Kotlin 测试实现，也没有运行 Gradle 或 kotlinc。四个 CJ 文件属于不同包，不能把本目录当作一个包直接编译。

SDK 为 Windows cjc 1.1.3；固定 native 对象 `14276740ffec1262b81b6b5236b1d840af270b37`，固定 Kotlin 对象 `3c7a5041f238102285764d370665f56f587f9dc4`。完整 CJO 原字节、SHA、解码记录及 Mirror 诊断 / 语义 AST 归档在 [native-evidence-20261119.json](native-evidence-20261119.json)。

| 原稿 | 入口及实际官方结果 |
| --- | --- |
| [defaultConstructorRoles.cj](defaultConstructorRoles.cj) | raw / metadata 共用模型；退出 0、0 error、4 条 `chir_dce_unused_variable` warning；CJO 为 D44 / T27 / E0。warning 来自两个 choose 中的 first / last，属于 CHIR，不改前端 inline 期望。 |
| [qualifiedDefaultConstructors.cj](qualifiedDefaultConstructors.cj) | `import default_constructor_models as Models` 后限定调用；使用上一原稿的实际 CJO；退出 0、无诊断输出；CJO 为 D11 / T19 / E41。 |
| [javaMirrorNoImplicitConstructor.cj](javaMirrorNoImplicitConstructor.cj) | Java Mirror 的依赖错误原稿；退出 1、4 error、0 warning，无 CJO；两个 class 的语义 AST 没有生成默认 init。 |
| [objcMirrorNoImplicitConstructor.cj](objcMirrorNoImplicitConstructor.cj) | ObjC Mirror 的依赖错误原稿；退出 1、4 error、0 warning，无 CJO；两个 class 的语义 AST 没有生成默认 init。 |

## raw 与完成 owner 的断言依据

native parser 的原 AST 中，普通无 constructor 的 class / struct 不包含默认 init；其默认节点由后续准备创建。CFIR 的早期 shared raw 工厂与 native parser 的生成时机不同，不能声称 native 原 parse AST 已有这个节点。

两 raw 入口应读取同一模型原稿，检查当前工厂产物的稳定角色、空 Block、owner / symbol、借用的 class TP 与成员顺序；RAW 空 Block 的存在不能代替正常 TYPES / STATUS / BODY 后的 resolved 返回、真实 CFG 和分析就绪。

固定 native `src/Sema/TypeChecker.cpp::CheckDefaultParamFunc`、`src/Sema/Utils.cpp::AddDefaultCtor/CreateDefaultCtor`：

- DefaultClass / DefaultStruct 的正常完成成员为 choose、默认 init；不能把 init 放到头部。
- StaticClass / StaticStruct 在语义 AST 中保留原 state、static init、read，并另追加默认实例 init。static 与实例 constructor 分开；static 返回 Unit，实例返回本 nominal。
- ExplicitClass 的显式 init 在 read 之前；不补默认实例 init，不把所有名为 init 的成员都移动到末尾。
- PrimaryClass 的 value 是普通参数；已有 stored、read 保持原顺序，primary init 追加，不凭空派生成员参数字段。
- PrimaryStruct 的成员参数按 value、flag 派生字段；正常完成次序 read、value、flag、primary init。
- NoConstructor interface 不生成 init；NoDefaultInit enum 保留 Empty / Carry 的独立 VarDecl / FuncDecl 角色，不补 class / struct 的默认 init。

Kotlin 框架读取点是 `PsiRawFirBuilder` 的 primary / 普通 constructor builders，以及 `FirMemberDeserializer.loadConstructor` 的 self type / class TP 引用。仓颉的 append、static、primary 和生成角色以以上 native 及实际原稿取证为准，不搬入 Kotlin 的 constructor 顺序语义。

## metadata 的独立合同

以下顺序是实际 CJO body，不能拿语义 AST 的全部成员直接充当 metadata 期望：

| nominal | CJO body |
| --- | --- |
| DefaultClass / DefaultStruct | choose、init |
| StaticClass / StaticStruct | state、read、默认实例 init；原 static init 没有进入此 native CJO body |
| ExplicitClass | init、read |
| PrimaryClass | stored、read、primary init |
| PrimaryStruct | read、value、flag、primary init |
| NoConstructor | 空 |
| NoDefaultInit | Empty、Carry |

默认 init 的实际 attribute 包含 COMPILER_ADD、IMPLICIT_ADD、CONSTRUCTOR，PRIMARY_CONSTRUCTOR 未置位；显式普通 init 没有这两个生成位；两个声明 primary 的生成 init 有 PRIMARY_CONSTRUCTOR。`native-evidence-20261119.json` 保存每个声明的完整 unsigned 64-bit word 向量，包括末尾零 word，不能按最高置位 bit 截断或只投影已识别 flags。

DefaultClass 的 self / constructor 返回都引用类型 1，其实参类型为 Generic 2、3，绑定该 class 的 T、U 声明 2、3。DefaultStruct 对应 self / constructor 返回为类型 7，实参 Generic 8、9 绑定本 struct 的 TP 声明 10、11。两者默认 constructor 的自有 Generic 列表均为空；这是借用 owner TP，不能按名字创建或重绑新的 T、U。

genericDecl 的 INVALID FullId 槽按原 pkgId / decl / index 保存，不把空 index / 空 decl 误作有效外部引用。正式前端 / writer 回归应使用独立 `AbstractCfirDiagnosticTestBase` 和 handler，按实际表重编号后比较角色和绑定，不修改已有 FrozenTypes 测试。

## qualified 调用原稿

Models 是实际导入包的别名，类型和调用均使用 Models 限定；没有把 wildcard 导入擅自当作可用包命名空间。

七个正常 SOURCE 默认方法覆盖 generic default class / struct、static-only class / struct、显式普通 init、普通参数 primary class、成员参数 primary struct。官方实参数量按方法顺序为 0、0、0、0、0、1、2；返回绑定分别为各自的实际 nominal。

实际 CJO `callKind` 数值为 class 2、struct 4。固定 native `include/cangjie/AST/Node.h::CallKind` 将 FUNCTION_PTR 放在 3、STRUCT_CREATION 放在 4；固定 `schema/ModuleFormat.fbs` 的枚举声明顺序不同。附件保存真实字节值，不用 schema 的枚举标签把 struct 源调用误判成 super，不改官方源或原期望来规避格式差异。

本次 qualified 编译覆盖实际库导入边界。后续若要覆盖 SOURCE symbol provider 的新 implicit origin，须从正式依赖 source module / 正常 SOURCE 编译入口接入这些模型，不用手工 symbol 或 CJO Library origin 冒称 SOURCE origin 已覆盖。

## Mirror 负原稿的边界

两个 WithoutInit 的实际完成 AST constructor 数量为 0；两个 WithDeclaredInit 各保留一个明确声明的无正文 init，未新增默认 init。

Java 负原稿的四条真实错误是：两条 `sema_member_not_imported`（java.lang.JObject）以及两条 `sema_java_mirror_interoplib_must_be_imported`。ObjC 对应 objc.lang.ObjCId 和 `sema_objc_mirror_interoplib_must_be_imported`。完整定义名、位置和 AST 已归档；原稿没有先发明 CFIR inline 期望。

这些输入含依赖错误及 IS_BROKEN，观察到没有默认 ctor，不能因此关闭“完整有效 Mirror 身份先于默认 constructor 决策发布”的生产门；它们也不构成可链接或可执行的互操作正例。有效 Mirror 正例需要真实 java.lang / objc.lang 与 interoplib 依赖再由官方取证，不以补 synthetic library 或放松阶段合同替代。

## 重取证入口

在指定 worktree 根目录，以同一 SDK 分别编译单个 CJ 文件，使用：

```text
cjc <本目录/defaultConstructorRoles.cj> --output-type=staticlib --output-dir <独立/models目录> --dump-ast --diagnostic-format=json
cjc <本目录/qualifiedDefaultConstructors.cj> --output-type=staticlib --output-dir <独立/qualified目录> --import-path <独立/models目录> --dump-ast --diagnostic-format=json
cjc <本目录/javaMirrorNoImplicitConstructor.cj> --output-type=staticlib --output-dir <独立/java目录> --dump-ast --diagnostic-format=json
cjc <本目录/objcMirrorNoImplicitConstructor.cj> --output-type=staticlib --output-dir <独立/objc目录> --dump-ast --diagnostic-format=json
```

本次原生产产物位于 `build/default-constructor-drafts-native-v113/`；附件中的压缩原字节避免清理 build 后丢失参考。归档来自官方 cjc，不把本库 actual 反向写成 oracle。
