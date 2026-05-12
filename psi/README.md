# psi/ — 仓颉源码 PSI、Lexer、Parser

前端管线阶段 2 `PARSE` 的实现：将 `.cj` 源码转换为 IntelliJ PSI 树，作为后续 CFIR 构建（阶段 6）的输入。

对齐 Kotlin K2 `compiler/psi`：使用 IntelliJ PSI / LightTree，不自研 Parser。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.lexer` | JFlex 词法分析器与 token 类型，含文档注释子集 `lexer/cdoc` |
| `org.cangnova.cangjie.parsing` | 手写递归下降 Parser（`CangJieParsing.kt`，覆盖完整语法） |
| `org.cangnova.cangjie.psi` | PSI 节点接口、`impl` 实现、`nodetypes` 节点类型、`codeFragmentUtil` 代码片段工具、`dummpholder` 占位节点 |
| `org.cangnova.cangjie.lang` | 语言定义与声明结构 |
| `org.cangnova.cangjie.annotator` | 注解器 |
| `org.cangnova.cangjie.macro` | 宏占位 PSI |
| `org.cangnova.cangjie.fileClasses` | 文件级 PSI（`.cj` / `.cjd`） |
| `org.cangnova.cangjie.name` | PSI 内的名称工具 |
| `org.cangnova.cangjie.icon` / `messages` | 图标与本地化消息 |

## 依赖

- `:common`、`:util`
- `intellijCore()`（compileOnly，统一由 `:dependencies:intellij-core` 提供）
- `libs.guava`

## Lexer 生成

参考 Kotlin K2 的 `jflexPath` 模式，不使用 GrammarKit 插件。Lexer 源文件位于 `src/org/cangnova/cangjie/lexer/*.flex`，生成产物在 `gen/`。

```bash
./gradlew :psi:compileKotlin    # 自动触发 Lexer 生成
./gradlew :psi:test
```

## 测试

- 接入 `:tests:test-infrastructure`（testFixtures）
- 解析器测试位于 `test/org/cangnova/cangjie/parsing/`

## 上游接入（IDE 插件）

`prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide` 聚合本模块为 fat jar，供 `intellij-ide/` 子项目通过 dependency substitution 使用。

## 相关文档

- `../docs/psi-cfir-ast-chir-alignment.md` — PSI ↔ CFIR ↔ 官方 AST ↔ CHIR 节点级对照
- `docs/psi-parser-comparison.md` — Parser 实现对比说明
