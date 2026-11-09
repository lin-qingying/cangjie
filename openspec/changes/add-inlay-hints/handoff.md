# Handoff：inlay hints 变更收尾

本变更（`add-inlay-hints`）功能与验证均已完成，等待合并。设计与逐项决策见 `design.md`，
本文只写「怎么接手」。

## 分支与提交

| 仓库 | 分支 | 相对基线 | 提交数 |
|---|---|---|---|
| 根仓库 | `feature/inlay-hints-next` | `main`（`bcd4d8884`） | 7 |
| intellij-ide | `feature/inlay-hints-next` | `cfir-new`（`5396db16`） | 5 |

两个工作树都在根仓库内部，IDE 侧是 `cangjie-wt-hints/intellij-ide`（必须如此，见 design D45）。

## 合并

顺序不能反：IDE 侧引用共享层的 `CangJieInlayHintTarget.RANGE_VALUE` 等枚举，需先合根仓库。

```bash
git -C D:/code/intellij/cangjie merge feature/inlay-hints-next
git -C D:/code/intellij/cangjie/intellij-ide merge feature/inlay-hints-next
```

已用 `git merge-tree --write-tree` 预演：两侧均无冲突。注意主检出区当时有未提交改动，
合并前先 `git status` 确认干净。

## 验证矩阵（全部通过）

| 命令 | 结果 |
|---|---|
| `gradlew.bat :code-insight:hints:test` | 34/34 |
| `gradlew.bat :lsp:test`（全量） | 124/124 |
| `intellij-ide: gradlew.bat :modules:ide:base:test --tests *CangJieInlayHintsProviderTest` | 13/13 |

## 已交付的五个 provider

| providerId | 逐项开关 | 说明 |
|---|---|---|
| `cangjie.references.types.hints` | 4 项 | 变量/局部/属性/函数返回类型 |
| `cangjie.parameters.hints` | 无 | 调用点形参名 |
| `cangjie.values.hints` | `cangjie.values.ranges` | 区间开闭性 `0 ≤ .. < 10` |
| `cangjie.lambdas.hints` | `hints.lambda.return` | lambda 隐式返回类型 |
| `cangjie.call.chains.hints` | 无 | 多行链各环节类型 |

## 两个未落地项（前置都不在提示层）

1. **lambda 隐式接收者 `this: T`**（design D48）：`CaFunctionType` 与 `ConeFunctionType`
   都没有 receiver 槽位——仓颉把扩展接收者建模成首参，不是解析器或 API 没暴露。
2. **注解实参名**（design D41 + D56）：探针已确认声明不会被宏吞掉，卡点是
   `MACRO_INPUT` 里的实参是原始 token（`QUOTE_TOKENS`），需要先做
   「macro input token 区间 → 表达式列表」的重建工具。建议单开变更，先做工具与单测再接提示。

## 环境坑（会误导判断，务必先看）

- 构建要读另一棵工作树的产物时，**必须在 agent 沙箱外跑**，否则表现为「Gradle daemon 无故消失」（D46）。
- 15G 内存 + IDEA 常驻时，`-Xmx5g` 会 native OOM（`Chunk::new`）；用 `-Xmx3~4g` + `--max-workers=1`，
  跑前先 `gradlew.bat --stop`（D47）。
- `gradlew-queue.bat` 在新建工作树里首跑 bootstrap 会 `ClassNotFoundException: GradleQueueMainKt`，
  直连 `gradlew.bat`（D51）。
- PowerShell 传 `-D...` 参数必须放进字符串数组，否则 `-Dorg` 前缀被吞（D51）。
- 构建会改写 `*/gen/**` 生成文件，提交前 `git checkout -- analysis/ cfir/ psi/gen compiler/frontend/gen`。
