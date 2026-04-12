## 1. 批次一：核心语义 Checker

- [x] 1.1 实现 `CfirGeneralSemanticsChecker`（General 分组）：节点有效性、类型推断失败、多重赋值类型检查、歧义使用、子包冲突、可访问性检查、参数个数通用检查、core.Object 缺失检查。对齐 `TypeCheckDecl.cpp`、`TypeCheckUtil.cpp`。
- [x] 1.2 实现 `CfirFunctionSemanticsChecker`（Function 分组）：返回类型推断、泛型函数类型参数推断、被调用对象合法性、return 位置合法性、subscript operator 语义、static 函数重载冲突、mut/unsafe 函数引用限制。对齐 `TypeCheckDecl.cpp`、`TypeCheckCall.cpp`。
- [ ] 1.3 实现 `CfirFunctionLambdaChecker`（Function 分组补充）：trailing lambda 类型检查、lambda 参数类型注解、默认参数限制、基本类型扩展歧义、捕获可变变量闭包限制。对齐 `TypeCheckExpr/LambdaExpr.cpp`。
- [x] 1.4 实现 `CfirExpressionSemanticsChecker`（Expression 分组）：表达式类型推断、浮点字面量范围、一元运算符合法性、subscript 表达式、成员访问、赋值合法性。对齐 `TypeCheckExpr/UnaryExpr.cpp`、`TypeCheckExpr/SubscriptExpr.cpp`、`TypeCheckExpr/NameReferenceExpr.cpp`、`TypeCheckExpr/AssignExpr.cpp`。
- [ ] 1.5 实现 `CfirPatternExpressionChecker`（Expression 分组补充）：or-pattern/or-condition 变量引入限制、不可达模式、enum 构造器参数检查、optional chaining 非 optional、capture-before-initialization、常量模式字符串插值、包名引用限制、需要导入表达式。对齐 `TypeCheckPattern.cpp`、`TypeCheckExpr/OptionalChainExpr.cpp`。
- [x] 1.6 实现 `CfirDeclarationStatusExtraChecker`（DeclarationStatus 补充）：PARAM_NAMED_MISMATCHED、OVERRIDE_STATIC_ERROR、REDEF_INSTANCE_ERROR、INVALID_OPERATOR_PARAMETER_COUNT。扩展现有 `CfirDeclarationStatusCheckers.kt` 或创建新文件。
- [ ] 1.7 实现 `CfirUnusedImportChecker`（Unused 分组）：未使用 import 语句检测。对齐 `CheckUnusedImportImpl.cpp`。
- [x] 1.8 将批次一所有新 checker 注册到 `CommonDeclarationCheckers` / `CommonExpressionCheckers`

## 2. 批次二：类型系统深层 Checker

- [ ] 2.1 实现 `CfirGenericDeepChecker`（GenericDeep 分组）：泛型类型替换一致性、参数个数匹配、约束宽松性、实例化歧义、递归绑定、上界类型约束。对齐 `TypeCheckGeneric.cpp`、`GenericInstantiation/`。
- [ ] 2.2 实现 `CfirGenericJavaInteropChecker`（GenericDeep Java 子集）：static 成员泛型依赖、基本类型泛型参数、间接约束满足、@Java 泛型上界约束。
- [ ] 2.3 实现 `CfirInheritanceDeepChecker`（InheritanceDeep 分组）：成员类型一致性、跨父成员冲突、抽象类 static 未实现、open/abstract 可见性、sealed 继承约束。对齐 `InheritanceChecker/`。
- [ ] 2.4 实现 `CfirInheritanceThreadContextChecker`（InheritanceDeep 补充）：ThreadContext 继承约束、This 返回类型约束。
- [x] 2.5 实现 `CfirClassStructSemanticsChecker`（ClassStruct 分组）：static 成员未初始化、finalizer 限制、sealed 约束、static 变量泛型依赖、@C struct 接口限制、同名 private 导出限制。对齐 `TypeCheckClassLike.cpp`、`LegalityOfUsage/`。
- [x] 2.6 实现 `CfirPropertySemanticsChecker`（Property 分组）：访问器必要性、immutable setter 限制、继承 mut/immut 一致性、接口属性完整实现。
- [ ] 2.7 实现 `CfirConstDeclarationChecker`（ConstDeclaration 分组）：const 修饰合法性、const 函数内 var 限制、const 构造器前置条件、const 构造器 var 成员冲突。对齐 `ConstEvaluationChecker.cpp`。
- [ ] 2.8 将批次二所有新 checker 注册到 `CommonDeclarationCheckers` / `CommonExpressionCheckers`

## 3. 批次三：语言特性 Checker

- [ ] 3.1 实现 `CfirAnnotationExtraChecker`（AnnotationExtra 分组）：@Annotation 参数约束、非 public 注解警告、自定义注解位置、注解参数合法性、注解目标限制、JFFI 注解限制。对齐 `TypeCheckAnnotation.cpp`。
- [ ] 3.2 实现 `CfirExtendExtraChecker`（ExtendExtra 分组）：override 限制、成员遮蔽、非法成员、检查顺序、导出依赖、@Java 类型 extend 限制、导入接口限制。对齐 `TypeCheckExtend.cpp`。
- [ ] 3.3 实现 `CfirDeprecatedSemanticsChecker`（Deprecated 分组）：deprecated 调用检查（error/warning 级别）、严格度继承约束、override/redef @Deprecated 一致性。对齐 `DeclAttributeChecker.cpp`。
- [ ] 3.4 实现 `CfirEffectsExtraChecker`（EffectsExtra 分组）：resumption 类型合法性、返回类型匹配、command-resumption 匹配、resume resumption 类型、try/handle return 限制、无用 command 类型。对齐 `TypeCheckExpr/PerformExpr.cpp`、`TypeCheckExpr/ResumeExpr.cpp`、`TypeCheckExpr/TryExpr.cpp`。
- [ ] 3.5 实现 `CfirSpawnSemanticsChecker`（Spawn 分组）：spawn 参数合法性、spawn 参数效果检查。对齐 `TypeCheckExpr/SpawnExpr.cpp`。
- [ ] 3.6 实现 `CfirInterfaceSemanticsChecker`（Interface 分组）：接口未实现 static 成员调用检查。对齐 `TypeCheckClassLike.cpp`。
- [ ] 3.7 实现 `CfirInoutSemanticsChecker`（Inout 分组）：CString/零大小类型限制、CType 约束、var 变量约束、堆变量约束、CFunc 调用上下文限制、参数类型匹配。对齐 `FFI/CFFICheck.cpp`。
- [ ] 3.8 实现 `CfirVArrayExtraChecker`（VArrayExtra 分组）：构造器参数个数、subscript 约束、CFunc 返回限制、元素类型限制。对齐 `TypeCheckType.cpp`。
- [ ] 3.9 将批次三所有新 checker 注册到 `CommonDeclarationCheckers` / `CommonExpressionCheckers`

## 4. 批次四：互操作与平台 Checker

- [ ] 4.1 实现 `CfirJavaInteropChecker`（JavaInterop 分组）：@Java 类型使用约束、JType 兼容性、继承约束��不支持的声明、注解缺失检查、变量/泛型类型限制。对齐 `FFI/FFICheck.cpp`。
- [ ] 4.2 实现 `CfirJavaMirrorChecker`（JavaMirror 分组）：mirror 构造器/方法/属性类型约束、mirror/impl 继承约束、mirror extend 限制、impl 重定义、interoplib 导入检查、@JavaHasDefault 约束。
- [ ] 4.3 实现 `CfirCJMappingChecker`（CJMapping 分组）：struct 泛型限制、struct 接口继承限制、声明类型限制、方法参数/返回类型限制、实例配置检查。
- [ ] 4.4 实现 `CfirObjCInteropChecker`（ObjCInterop 分组）：构造器/方法/属性/字段类型兼容性、mirror 继承约束、interoplib 导入、@ForeignName 约束、ObjCPointer 约束、ObjCImpl super class 约束。
- [ ] 4.5 实现 `CfirObjCCJMappingChecker`（ObjCCJMapping 分组）：继承接口限制、泛型限制。
- [ ] 4.6 实现 `CfirForeignNameChecker`（ForeignName 分组）：override 位置约束、注解冲突、派生注解冲突。
- [ ] 4.7 实现 `CfirIfAvailableChecker`（IfAvailable 分组）：参数合法性（名称、字面量、未知参数名）、APILevel 限制。
- [ ] 4.8 实现 `CfirAPILevelChecker`（APILevel 分组）：多注解检查、参数检查、字面量限制、引用更高 level 检查、syscap 检查。
- [ ] 4.9 实现 `CfirHideChecker`（Hide 分组）：多注解检查、函数参数限制、缺失检查、编译期可见性、参数检查、注解位置警告。
- [ ] 4.10 实现 `CfirMockSemanticsChecker`（Mock 分组）：功能启用检查、类型约束、static 声明约束、包兼容性、@Frozen 兼容性。对齐 `Test/`。
- [ ] 4.11 实现 `CfirCommonSpecificChecker`（CommonSpecific 分组）：声明匹配、类型/修饰符/注解/参数/超类型一致性、common open class 构造器、多 specific 实现、var/let 一致性、成员实现体、main 限制、抽象成员修饰符、@Frozen 泛型限制。对齐 `CJMP/`。
- [ ] 4.12 将批次四所有新 checker 注册到 `CommonDeclarationCheckers` / `CommonExpressionCheckers`

## 5. 测试与验证

- [ ] 5.1 为批次一核心语义 checker 编写测试用例（参考 `cfir/analysis-tests/` 现有测试结构）
- [ ] 5.2 为批次二类型系统深层 checker 编写测试用例
- [ ] 5.3 为批次三语言特性 checker 编写测试用例
- [ ] 5.4 为批次四互操作与平台 checker 编写测试用例
- [ ] 5.5 集成测试：验证所有 checker 在完整编译管线中正确运行，无相互干扰
- [ ] 5.6 对齐验证：选取官方 C++ 编译器的测试用例，验证 CFIR checker 输出与官方编译器一致
