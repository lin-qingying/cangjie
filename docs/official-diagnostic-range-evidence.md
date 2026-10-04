# 官方 cjc 诊断位置取证矩阵

本文记录用官方 `cjc --diagnostic-format=json` 实测得到的诊断高亮位置，作为仓颉前端
诊断位置对齐与锚点改造的依据。所有数据均由本机 `cjc 1.0.5` 实际编译产生，非推测。

- 取证日期：2026-10-03
- 官方 SDK：`C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe`
- 复现方式：`cjc <file>.cj --diagnostic-format=json`，读取 `Diags[].MainHint.Range` 的
  `Begin/End`（行列均为一基，一列为该列字符本身）。

## 1. `sema_ambiguous_use` —— 整节点锚定（已对齐）

探针源码（同名 classifier 重声明后在类型使用位置产生歧义）：

```cangjie
open class A<X, Y> {}
open class B<X, Y> {}
open class A<X, Y> {}
open class B<X, Y> {}

func foo<X, Y>(a: B<Y, X>): Int64 {
    0
}
main() {}
```

官方输出：

```
Kind=sema_redefinition      Msg=redefinition of declaration 'A'   Range=L7C12 -> L7C13
Kind=sema_redefinition      Msg=redefinition of declaration 'B'   Range=L10C12 -> L10C13
Kind=sema_ambiguous_use     Msg=ambiguous use of 'B'              Range=L13C19 -> L13C26
```

**结论**：第 13 行列 19→26 覆盖 `B<Y, X>` 完整 7 字符，**含类型实参**。

这与官方源码一致：`src/Sema/Diags.cpp:436` 的 `DiagAmbiguousUse` 调用
`DiagnoseRefactor(kind, node, name)` **不传 Range**，因此
`include/cangjie/Basic/DiagnosticEngine.h:908-916` 恒取 `node.GetBegin()..node.GetEnd()`，
即整节点。

仓颉 fixture `cfir/analysis-tests/testData/llt/generics/generic_parameters2.cj:18`
期望 `<!AMBIGUOUS_USE!>B<Y, X><!>`，与官方**逐字符一致**。

> 该取证直接支撑了 `CFIR_AMBIGUOUS_USE` 使用 `SourceElementPositioningStrategies.DEFAULT`
> （整元素范围）的决策。若沿用 `REFERENCED_NAME_BY_QUALIFIED`（收窄到末段名称），
> 会标成 `X` 而非 `B<Y, X>`。

## 2. `sema_no_match_function_declaration_for_call` —— 函数名首字符

探针源码：

```cangjie
class Box {
    private func hidden(a: Int64): Int64 { return a }
}
public func use(): Int64 {
    let b = Box()
    return b.hidden(1)
}
main(): Int64 { return 0 }
```

官方输出：

```
Kind=sema_no_match_function_declaration_for_call
Msg=no matching function declaration for function call 'hidden'
Range=L8C14 -> L8C15
```

**结论**：第 8 行 `return b.hidden(1)` 中，col 14→15 只覆盖 **1 个字符**（`hidden` 的首字母
`h`），既不是完整函数名 `hidden`，也不是整条访问表达式 `b.hidden`。

## 3. `sema_wrong_number_of_arguments` —— 实参位置

探针源码与输出：

| 形态 | 源码片段 | 官方范围 |
|---|---|---|
| 多余实参 | `foo(1, 2)` | L2C38 → L2C44（即 `1, 2`） |
| 缺少实参 | `bar(3)` | L2C38 → L2C41（即 `3`） |
| 限定名+多余实参 | `Outer.pick(1, 2, 3)` | L4C45 → L4C54（即 `1, 2, 3`） |

**结论**：该诊断锚定在**实参列表的越界/缺失位置**，与函数名无关。

## 4. 与仓颉现状的差距

| 诊断 | 官方范围 | 仓颉现状 | 差距 |
|---|---|---|---|
| `AMBIGUOUS_USE` | 整节点 `B<Y, X>` | `DEFAULT`（整元素） | ✅ 已对齐（本次修复） |
| `NO_MATCH_FUNCTION_DECLARATION_FOR_CALL` | 函数名**首字符** | `anchor.start .. start+name.length`（**完整函数名**） | ❌ 不一致 |
| `WRONG_NUMBER_OF_ARGUMENTS` | 实参位置 | 见 `CfirMapArguments.kt:730` | 待核 |

仓颉现有 fixture `cfir/analysis-tests/testData/llt/call/lexical_function_shadowing.cj:37`
固化了完整函数名期望：

```
<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>same<!>(1)
```

## 5. 基线上已存在的测试失败（非本次改动引入）

以下失败在**移除本次全部改动**（`git stash`）后同样失败，已逐一验证，属仓库既有状态，
不可与本次诊断锚点改造的回归混淆。

### 5.1 CJMP 声明匹配（8 例）

`CfirAnalysisDiagnostics2TestGenerated` / `CfirAnalysisDiagnostics2PsiTestGenerated`
各 4 例，同一批用例：

| 用例 | 现象 |
|---|---|
| `testCjmpSpecificShadowingBeforeImplicitTypes` | 期望无诊断，实际报 `AMBIGUOUS_FUNCTION_CALL` |
| `testCjmpBothImplicitReturnTypesMatch` | 期望零诊断，实际报 `AMBIGUOUS_FUNCTION_CALL` |
| `testCjmpCommonValueSpecificEmptyBodyMismatch` | 期望 `NOT_MATCHED`，实际 `RETURN_TYPE_INCOMPATIBLE` |
| `testCjmpImplicitSpecificBodyErrorPreservesMatch` | 期望 `RETURN_TYPE_INCOMPATIBLE`，实际两侧 `NOT_MATCHED` |

涉及 `CJMP_MODE: COMMON/SPECIFIC` 的声明配对语义，与诊断锚点无关。

### 5.2 宏 raw builder 覆盖面（1 例）

`MacroConstructionArchitectureGuardTest.rawBuildersCoverAllMacroSurfaceShapes`：

```
PSI and LightTree raw builders must not diverge in macro surface coverage:
PSI raw builder: missing explicit IfAvailableSurface construction or BuiltinNonMacroSurface branch
```

该守卫断言 `IfAvailableSurface` / `BuiltinNonMacroSurface` 同时出现在 PSI 与 LightTree
raw builder 中；这两个符号目前只存在于 `cfir/providers/.../macro/` 与
`compiler/frontend/.../MacroExpandPhase.kt`，未出现在两个 raw builder 中。

## 6. 回归基线

本次改动后的验证范围与结果：

| 套件 | 用例数 | 失败 |
|---|---|---|
| `:cfir:analysis-tests` | 8964 | 8（即 5.1） |
| `:analysis:analysis-api-cfir` | 1531 | 0 |
| `:lsp` | 40 | 0 |
| `:cfir:providers` | 30 | 0 |
| `:compiler:frontend` | 114 | 1（即 5.2） |

即：本次诊断锚点改造**零新增失败**。

## 7. 后续工作

`NO_MATCH_FUNCTION_DECLARATION_FOR_CALL` 的范围对齐**必须作为独立任务**处理：

1. 该诊断当前以 offsets-only 锚点构造（`coneDiagnosticToCfirDiagnostic.kt:162`），
   在 analysis API 下会触发 `LLCfirDiagnosticReporter` 的
   `error("Unknown diagnostic type ...")`，属崩溃点；
2. 但直接转换锚点只会把"崩溃 + 错误范围"变成"不崩溃 + 错误范围"；
3. 正解需同时（a）把范围对齐到官方的首字符、（b）保留 PSI 锚点，（c）更新受影响 fixture；
4. 涉及范围变更，须走独立的诊断位置对齐评审，不能与崩溃修复混做。

同类需一并取证的还有仓颉中所有 `name.length` 宽度计算点
（`coneDiagnosticToCfirDiagnostic.kt:162/990/1422`、`CfirMapArguments.kt:730/855`、
`DeclaredSupertypeClassification.kt:263`）。

