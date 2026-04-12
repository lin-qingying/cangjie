## 为什么

CFIR checker 框架已具备完整的基础设施（诊断定义、checker 注册体系、collector/visitor 管线），且约 1958 行的 `CfirDiagnosticsList` 已覆盖仓颉编译器全部 40+ 语义诊断分组。然而，已注册到 `CommonDeclarationCheckers` / `CommonExpressionCheckers` / `CommonTypeCheckers` 的 checker 实现仅覆盖其中约 30%，大量诊断分组——包括通用语义、函数语义、表达式语义、泛型深层检查、继承深层检查、属性语义、常量声明、注解、inout、VArray、effects、@Deprecated、common/specific 跨平台、Java/ObjC 互操作、@ForeignName、@IfAvailable、@APILevel、@Hide、Mock 等——的对应 checker 尚未实现。当前状态导致编译器前端无法完成完整的语义检查，大量本应在编译期拦截的错误会被漏检。

此变更的目标是：对齐 C++ 官方仓颉编译器 `src/Sema/` 目录下的全部语义检查逻辑，在 Kotlin/JVM 侧以 K2 FIR checker 框架的写法，系统性地为每个诊断分组实现完整的 checker，从而使 CFIR 前端管线具备与官方编译器对等的语义校验能力。

## 变更内容

**新增大量 checker 实现**，按仓颉官方编译器 Sema 模块的功能域划分为以下批次：

1. **通用语义 checker**（General）：节点有效性、类型推断失败、歧义使用、子包冲突、可访问性等
2. **函数语义 checker**（Function）：返回类型推断、subscript 语义、static 函数重载冲突、mut 函数引用限制、lambda 参数类型注解、trailing lambda、捕获可变变量约束等
3. **表达式语义 checker**（Expression）：浮点字面量范围、一元运算符合法性、subscript 表达式、成员访问、赋值合法性、or-pattern/or-condition 变量引入、不可达模式、optional chaining 非 optional 类型、enum 构造器参数检查、capture-before-initialization 等
4. **泛型深层 checker**（GenericDeep）：泛型类型替换一致性、参数个数匹配、约束宽松性、泛型实例化歧义、递归绑定、上界类型约束等
5. **继承深层 checker**（InheritanceDeep）：成员类型一致性、跨父类型成员冲突、sealed 继承约束、ThreadContext 约束、This 返回类型约束等
6. **类/结构体语义 checker**（ClassStruct）：static 成员未初始化、finalizer 限制、sealed 约束、泛型参数 static 依赖等
7. **属性语义 checker**（Property）：访问器必要性、immutable 属性 setter 约束、继承 mut/immut 一致性等
8. **常量声明 checker**（ConstDeclaration）：const 修饰合法性、const 函数内 var 限制、const 构造器约束等
9. **注解语义 checker**（AnnotationExtra）：@Annotation 参数约束、自定义注解位置、注解目标匹配等
10. **inout 语义 checker**（Inout）：CString/零大小类型限制、CType 约束、var 变量约束、堆变量约束等
11. **VArray 语义 checker**（VArrayExtra）：构造器参数个数、subscript 约束、CFunc 返回限制等
12. **Effects 语义 checker**（EffectsExtra）：resumption 类型匹配、try/handle return 约束等
13. **@Deprecated 语义 checker**（Deprecated）：deprecated 调用检查、严格度继承约束、override/redef 标记等
14. **common/specific 跨平台 checker**（CommonSpecific）：声明匹配、修饰符一致性、抽象成员约束等
15. **Extend 补充 checker**（ExtendExtra）：override 限制、成员遮蔽、非法成员等
16. **Spawn 语义 checker**（Spawn）：spawn 参数合法性
17. **接口语义 checker**（Interface）：未实现 static 成员调用
18. **Java 互操作 checker**（JavaInterop + JavaMirror + CJMapping）：@Java 类型约束、mirror/impl 继承规则、JType 兼容性等
19. **ObjC 互操作 checker**（ObjCInterop + ObjCCJMapping）：@ObjCMirror/@ObjCImpl 约束、ObjC 类型兼容性等
20. **@ForeignName checker**（ForeignName）：注解冲突、override 位置约束
21. **@IfAvailable / @APILevel / @Hide checker**：参数合法性、level 约束等
22. **Mock 语义 checker**（Mock）：mock 模式约束、@Frozen 兼容性等
23. **DeclarationStatus 补充 checker**：`PARAM_NAMED_MISMATCHED`、`OVERRIDE_STATIC_ERROR`、`REDEF_INSTANCE_ERROR`、`INVALID_OPERATOR_PARAMETER_COUNT` 等当前未实现的诊断

**修改**：

- `CommonDeclarationCheckers`：注册新增的 declaration checker 实例
- `CommonExpressionCheckers`：注册新增的 expression checker 实例
- `CommonTypeCheckers`：按需注册新增的 type checker 实例
- 可能需要在 `DeclarationCheckers` / `ExpressionCheckers` 生成器中扩展新的 checker 分类（如有必要）

## 功能 (Capabilities)

### 新增功能
- `general-semantics-checker`: 通用语义检查——节点有效性、类型推断、歧义使用、子包冲突、可访问性
- `function-semantics-checker`: 函数语义检查——返回类型推断、subscript 合法性、static 重载冲突、mut/unsafe 函数引用限制、lambda/trailing-lambda 约束
- `expression-semantics-checker`: 表达式语义检查——浮点范围、一元运算、subscript、成员访问、赋值合法性、or-pattern/or-condition、optional chaining、capture-before-init
- `generic-deep-checker`: 泛型深层检查——类型替换一致性、参数匹配、约束宽松性、递归绑定、上界约束
- `inheritance-deep-checker`: 继承深层检查——成员类型一致性、跨父成员冲突、sealed/ThreadContext/This 约束
- `class-struct-semantics-checker`: 类/结构体语义检查——static 未初始化、finalizer 限制、sealed 约束
- `property-semantics-checker`: 属性语义检查——访问器约束、mut/immut 继承一致性
- `const-declaration-checker`: 常量声明检查——const 修饰、var 限制、const 构造器约束
- `annotation-extra-checker`: 注解补充检查——@Annotation 参数、自定义注解位置、注解目标
- `inout-semantics-checker`: inout 语义检查——类型约束、var 限制、堆变量约束
- `varray-extra-checker`: VArray 语义检查——构造器参数、subscript、CFunc 返回约束
- `effects-extra-checker`: Effects 补充检查——resumption 类型、try/handle return
- `deprecated-semantics-checker`: @Deprecated 语义检查——调用检查、严格度约束、override/redef 标记
- `common-specific-checker`: common/specific 跨平台检查——声明匹配、修饰符一致性、抽象成员约束
- `extend-extra-checker`: Extend 补充检查——override 限制、成员遮蔽、非法成员
- `spawn-semantics-checker`: Spawn 语义检查——参数合法性
- `interface-semantics-checker`: 接口语义检查——未实现 static 成员调用
- `java-interop-checker`: Java 互操作检查——@Java 类型约束、JType 兼容性、mirror/impl 继承
- `objc-interop-checker`: ObjC 互操作检查——@ObjCMirror/@ObjCImpl 约束、类型兼容性
- `foreign-name-checker`: @ForeignName 检查——注解冲突、override 位置
- `if-available-api-level-hide-checker`: @IfAvailable / @APILevel / @Hide 检查——参数合法性、level 约束
- `mock-semantics-checker`: Mock 语义检查——mock 模式约束、@Frozen 兼容性
- `declaration-status-extra-checker`: 声明状态补充检查——参数命名一致性、override/redef static 检查、操作符参数个数

### 修改功能

（无已有 spec 需修改）

## 影响

- **核心影响模块**：`cfir/checkers/src/` 下的 `checkers/declaration/`、`checkers/expression/`、`checkers/type/` 目录
- **注册入口**：`CommonDeclarationCheckers.kt`、`CommonExpressionCheckers.kt`、`CommonTypeCheckers.kt`
- **生成器**：`checkers-component-generator/` 如需新增 checker 分类（如 spawn、interface 等），需同步更新 `CheckersConfigurator.kt` 和 `Generator.kt`
- **诊断系统**：`CfirDiagnosticsList.kt` 已完成全部定义（无需新增），`CfirErrors.kt`（生成）和 `CfirErrorsDefaultMessages.kt` 需保持同步
- **外部参考**：实现逻辑需严格对齐 `external/cangjie_compiler/src/Sema/` 下对应的 C++ 语义检查代码
- **测试**：需为每个新 checker 编写单元测试用例，参考 `cfir/analysis-tests/` 目录下的现有测试结构
