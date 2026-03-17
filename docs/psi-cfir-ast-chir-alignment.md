# PSI -> CFIR -> 官方 AST -> CHIR 对齐说明

日期：2026-03-16  
范围：`psi`、`cfir/raw-cfir`、`external/cangjie_compiler/include/cangjie/{AST,CHIR}`

## 1. 读取范围与依据

- PSI 节点入口：`psi/src/org/cangnova/cangjie/psi/CjNodeTypes.java`
- PSI -> raw-cfir 转换入口：`cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt`
- raw-cfir 失效/错误表达式构造：`cfir/raw-cfir/raw-cfir-common/src/org/cangnova/cangjie/cfir/builder/AbstractRawCfirBuilder.kt`
- 官方 AST 定义：`external/cangjie_compiler/include/cangjie/AST/Node.h`、`ASTKind.inc`
- 官方 CHIR 定义：`external/cangjie_compiler/include/cangjie/CHIR/Expression/Expression.h`、`Expression/Terminator.h`、`ExprKind.inc`、`Package.h`、`Type/*.h`
- 官方 AST->CHIR 翻译入口（接口层）：`external/cangjie_compiler/include/cangjie/CHIR/AST2CHIR/TranslateASTNode/Translator.h`

说明：
- `PSI -> CFIR` 为直接代码映射（来自 `PsiRawCfirBuilder`）。
- `AST -> CHIR` 多为“翻译/降级”关系，非严格 1:1；文档中按“主要落点”描述。

## 2. 声明节点对齐（Declaration）

`PsiRawCfirBuilder.convertDeclaration` 覆盖项见 `PsiRawCfirBuilder.kt:141-156`。

| PSI | CFIR | 官方 AST | 官方 CHIR（主要落点） | 备注 |
|---|---|---|---|---|
| `CjClass` | `CfirClass(kind=CLASS)` | `ClassDecl` | `ClassDef` | 直接对应 |
| `CjInterface` | `CfirClass(kind=INTERFACE)` | `InterfaceDecl` | `ClassDef`(isClass=false) | 官方 CHIR 里接口也走 `ClassDef` |
| `CjStruct` | `CfirClass(kind=STRUCT)` | `StructDecl` | `StructDef` | 直接对应 |
| `CjEnum` | `CfirClass(kind=ENUM)` | `EnumDecl` | `EnumDef` | 枚举构造器在 CFIR 内补充处理 |
| `CjExtend` | `CfirExtend` | `ExtendDecl` | `ExtendDef` | 直接对应 |
| `CjNamedFunction` | `CfirFunction` | `FuncDecl` | `Func` | 顶层/成员/局部函数都最终落到 CHIR `Func` |
| `CjMainFunction` | `CfirMainFunction` | `MainDecl` | `Func`(入口函数) | AST 有独立 `MainDecl` |
| `CjMacroDeclaration` | `CfirMacroDeclaration` | `MacroDecl` | 主要转 `Func`/编译期流程 | CHIR 无单独 `Macro` 值节点 |
| `CjFinalizer` | `CfirFinalizer` | `FuncDecl`(`~init`) | `Func` | 官方 AST 通过 `FuncDecl.IsFinalizer()` 区分 |
| `CjProperty` | `CfirProperty` | `PropDecl` | `GlobalVar`/成员字段 + `Func`(getter/setter) | CHIR 按值与访问器拆分 |
| `CjFieldVariable` | `CfirVariable` | `VarDecl` | `GlobalVar`/`LocalVar` | 依作用域决定 |
| `CjPatternVariable` | `CfirPatternVariable` | `VarDecl`/`VarWithPatternDecl` | `LocalVar` + 绑定逻辑 | 模式绑定经翻译拆解 |
| `CjPrimaryConstructor` | `CfirConstructor` | `PrimaryCtorDecl` | `Func`(ctor) | 官方有 `PrimaryCtorDecl` |
| `CjSecondaryConstructor` | `CfirConstructor` | `FuncDecl`(constructor) | `Func`(ctor) | 次构造常表现为函数声明 |
| `CjTypeAlias` | `CfirTypeAlias` | `TypeAliasDecl` | 无独立 runtime 值节点 | 主要体现在类型翻译阶段 |

## 3. 表达式节点对齐（Expression）

`PsiRawCfirBuilder.convertExpression` 覆盖项见 `PsiRawCfirBuilder.kt:510-546`。

| PSI | CFIR | 官方 AST | 官方 CHIR（主要落点） | 备注 |
|---|---|---|---|---|
| `CjBlockExpression` | `CfirBlock` | `Block` | `Block`/`BlockGroup` | 结构块 |
| `CjConstantExpression` | `CfirLiteralExpression` | `LitConstExpr` | `Constant` | 直接字面量 |
| `CjStringTemplateExpression` | 字符串字面量/调用组合 | 字符串相关 AST 节点 | `Constant`/`Apply` 等 | 插值会被降级 |
| `CjBinaryExpression` | `CfirBinaryLogicExpression` / `CfirFunctionCall`(运算符) | `BinaryExpr`/`AssignExpr`/`RangeExpr` | `BinaryExpression` 或 `Apply/Invoke` | 运算符重载可能转调用 |
| `CjPrefixExpression` | `CfirFunctionCall` | `UnaryExpr` | `UnaryExpression` 或调用 | |
| `CjPostfixExpression` | `CfirFunctionCall` | `IncOrDecExpr` 等 | `UnaryExpression` 或调用 | |
| `CjCallExpression` | `CfirFunctionCall` | `CallExpr` | `Apply`/`Invoke`/`InvokeStatic`/`Intrinsic` | 调用类型由语义决定 |
| `CjSpawnExpression` | `CfirSpawnExpression` | `SpawnExpr` | `Spawn` / `SpawnWithException` | 在 `convertCall` 内分支处理 |
| `CjDotQualifiedExpression` | `CfirPropertyAccessExpression` | `MemberAccess` | `Field`/`FieldByName`/`Load` | 依目标是字段或函数名 |
| `CjSafeQualifiedExpression` | 同上 | `MemberAccess`/可选链相关 | 通常降级为控制流 + 字段/调用 | 非严格 1:1 |
| `CjNameReferenceExpression` | `CfirQualifiedAccessExpression` | `RefExpr` | `Load`/`GetElementByName`/函数值 | |
| `CjIfExpression` | `CfirIfExpression` | `IfExpr` | `If`，后续降级为 `Branch` | `Expression.h` 注释明确 |
| `CjMatchExpression` | `CfirMatchExpression` | `MatchExpr` | `Branch`/`MultiBranch` | 可优化成表驱动分支 |
| `CjForExpression` | `CfirForInExpression` | `ForInExpr` | `ForInRange`/`ForInIter`/`ForInClosedRange` + `Branch` | |
| `CjWhileExpression` | `CfirLoopExpression` | `WhileExpr` | `Loop` + `Branch` | |
| `CjDoWhileExpression` | `CfirLoopExpression` | `DoWhileExpr` | `Loop` + `Branch` | |
| `CjReturnExpression` | `CfirReturnExpression` | `ReturnExpr` | 终结控制流（`Exit`） | CHIR 中属 terminator 语义 |
| `CjBreakExpression`/`CjContinueExpression` | `CfirJumpExpression` | `JumpExpr` | `GoTo`/`Branch` | CFG 跳转 |
| `CjThrowExpression` | `CfirThrowExpression` | `ThrowExpr` | `RaiseException` | terminator |
| `CjTryExpression` | `CfirTryExpression` | `TryExpr` | 异常流 terminator + `GetException` 等 | CHIR 有 `*WithException` |
| `CjLambdaExpression` | `CfirLambdaExpression` + 匿名 `CfirFunction` | `LambdaExpr` | `Lambda` + `Func`/`FuncBody` | |
| `CjArrayAccessExpression` | `CfirSubscriptExpression` | `SubscriptExpr` | `GetElementRef`/`StoreElementRef` | |
| `CjCollectionLiteralExpression` | `CfirArrayLiteral` | `ArrayLit` | `RawArrayLiteralInit`/`VArray`/`VArrayBuilder` | |
| `CjTupleExpression` | `CfirTupleLiteral` | `TupleLit` | `Tuple` | |
| `CjIsExpression` | `CfirTypeOperatorCall` | `IsExpr` | `InstanceOf` | |
| `CjThisExpression` | `CfirQualifiedAccessExpression("<this>")` | `RefExpr`/语义 this | `Parameter(this)`/相关加载 | |
| `CjSuperExpression` | `CfirQualifiedAccessExpression("<super>")` | `RefExpr`/`MemberAccess` | `Invoke`/`InvokeStatic`(super 路径) | |

## 4. raw-cfir “原始节点失效”路径

### 4.1 声明失效（InvalidDeclaration）

- 位置：`PsiRawCfirBuilder.kt:157-165`
- 触发：`convertDeclaration` 未覆盖到的 `CjDeclaration`
- 行为：构造 `CfirInvalidDeclaration`，`reason = "Unsupported declaration: ..."`

### 4.2 表达式失效（ErrorExpression）

- 统一构造：`AbstractRawCfirBuilder.kt:99` `buildErrorExpression(...)`
- 未覆盖表达式：`PsiRawCfirBuilder.kt:546`  
  `Unsupported expression: ...`
- 缺字段/坏形态兜底（示例）：
  - 缺左右操作数：`619/621`
  - 缺 `if` 条件：`799`
  - 缺 `match` subject：`813`
  - 缺 `for-in` iterable：`865`
  - 缺 `while/do-while` 条件：`883/894`
  - 缺 `throw` 表达式：`914`
  - 缺下标接收者：`993`
  - 缺 `is` 操作数：`1017`

### 4.3 Lazy body 非失效但“延迟空壳”

- 位置：`PsiRawCfirBuilder.kt:54` 与多处 `BodyBuildingMode.LAZY_BODIES`
- 行为：函数体/初始化器可被置空（如 `buildBlock { }`），后续阶段再填充；这不是 invalid，但会让 raw-cfir 看起来“节点缺失”。

## 5. 目前可见的对齐缺口（按 raw-cfir 视角）

以下 PSI 节点在 `convertExpression` 中未显式分支，当前会落入 `Unsupported expression`（或依赖上游先行降级）：

- `CjSynchronizedExpression`（官方 AST 有 `SynchronizedExpr`）
- `CjUnsafeExpression`
- `CjLetExpression`
- `CjQuoteExpression`
- `CjMacroExpression`（表达式级宏）
- 其他未纳入 `convertExpression` 的新 PSI 节点

这部分会导致“PSI 有节点，raw-cfir 无对应专用节点”，即你提到的“原始节点失效/断档”现象。

## 6. 结论

- 本项目当前 `PSI -> raw-cfir` 主干已覆盖大量核心语法（声明 + 常见表达式）。
- 官方侧 `AST -> CHIR` 本质是翻译与降级流程，控制流与异常相关节点在 CHIR 中大量体现为 terminator/CFG 结构，而非源级 AST 的 1:1 类名映射。
- raw-cfir 断档点主要由两类导致：
  - 未覆盖 PSI 节点直接落 `Unsupported ...`；
  - `LAZY_BODIES` 模式下先构空壳，观察时看似“缺节点”。
