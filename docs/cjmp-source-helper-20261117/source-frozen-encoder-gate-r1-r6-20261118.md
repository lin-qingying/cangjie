# 冻结类型 encoder 六项独立审查

**REJECT，不能签发完整 S4。** 以下针对末次 Producer 候选 SHA-256 `ECD44775BA8F1E9233A2CA0B9B4375BAF08BE2D0DC3415019109895E25298784`。上游证据均来自指定 Kotlin/native 提交的 `git show`，链接仅用于定位。

1. **[P1] classifier 与自身 TP 没有作为同一投影产品发布。**  
   [名义类型 shape](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3100） 将声明改指 specific，实参却继续使用当前 context 的 owner 投影。`typeProjectionFor:1836` 没有合并被引用 classifier 自身的 counterpart TP 映射，因此 common self `C<Tcommon>` 可以形成“specific C＋common T”的绑定组合。该重定向还覆盖普通应用类型、Interface 和 Alias；native [TryGetSpecificImplementationTy](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L893) 只对 Class/Struct/Enum 的准确声明 self 身份执行双 memo。  
   **改法：**由完成家族发布 classifier、own TP、owner TP 的一致投影；serializer 按冻结类型角色消费，self 双身份登记严格遵循 native 条件。

2. **[P1] 新类型 FullId 路径让本地登记覆盖 imported 身份。**  
   新 `compositeInfo` 和 Generic 编码复用 [sourceExpressionTarget](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3031），先查本地及 `importedDeclarationIndices`，命中就写 CURRENT，未先检查冻结的 `isImported`。重导出正文扫描会将其引用的原包声明登记进该表；随后 SOURCE 类型对同一 imported classifier 的引用便丢掉外部 package/exportId。native [GetFullDeclIndex](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L791) 明确先处理 IMPORTED，再调用 `PreSaveDecl`。  
   **改法：**按冻结 imported 来源先确定外部 FullId；common 装载后的非 imported 声明独立执行 counterpart/current 投影，不能让已有槽位改变引用身份。

3. **[P1] Generic shape 丢掉捕获域和投影，fill 再次选择另一份 payload。**  
   Generic shape 仅保存投影后的 parameter；[fill 分支](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L3160） 随后用 `declarationTypeOf(parameter)`、parameter 自身 session 和 `typeProjectionFor(parameter)` 重查 bounds。这里查询的是冻结数据，但已经不是入口持有的 `(typeElement, projection)`。同一 TP 在不同捕获域中的 bounds classifier 绑定被 shape 合并后，fill 固定选择声明域，入口事实无法保留。  
   **改法：**Generic 保留身份锚点以及本次捕获、投影后的 bound 引用；按该域预登记 memo，fill 直接消费这些引用。bounds 不递归加入 shape key。此项独立于最终 upperBounds 尚未完成的问题。

4. **[P1] 新 RawArray 布局把已装载的多维数组认证成一维。**  
   [rawArrayLayout](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt)（L101） 根据嵌套 Cone 累计维度，但现有 [convertArrayType](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt)（L219） 只恢复单个叶子实参，完全未读取 `ArrayTyInfo.dimsOrSize`。因此原 wire 的 `(T, dims=2)` 被新构造器发布为 `(T, dims=1)`，新 encoder 又直接消费这个错误的“完成布局”。native [SaveArrayTy](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp#L1067) 本来就是单叶子＋累计 dims。  
   **改法：**装载、构造、替换共同保留完整布局产品；不能仅凭一个实参宣布一维布局已完成。

5. **[P1] 注解图入口要求 parser-owned 参数具有正文类型。**  
   [annotationArguments](../../cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt)（L93） 扫描全部注解，遇到字面量就要求 completed type。`Attribute/Overflow/When` 的 resolver 明确保留语法参数、不做普通实参类型解析；合法 `@Attribute[customFlag, "backendFlag"]` 的字符串因此会在正式 pipeline 生成 SOURCE 图时失败。官方 [Attribute 解析证据](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/unittests/Parse/ParserTest.cpp#L3210) 确认该语法有效，而 writer 对 Attribute 只写 TestRegistration 的空参数记录。  
   **改法：**由注解/export 事实 owner 发布正式待写实参计划，SOURCE 图仅接入计划中的表达式，避免把语法 token 当正文表达式。

6. **[P1] expressionOrNull 退休后，common CJO 注解实参没有接入路径。**  
   [新注解编码入口](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt)（L2971） 一律调用 canonical SOURCE `expressionOf`，但 SourceGraphProducer 明确跳过具有 CJO origin 的声明。`DeclarationInput.Source` 中保留的 common CJO 声明仍在 `declarationMetadata:1826` 调用 live `annotations(declaration)`；其 `@Deprecated[message: "old"]` 实参没有 canonical identity，写出会失败。已有 `importedAnnotations:1246` 仅覆盖 raw declaration 路径。  
   **改法：**让两类 CJO origin 声明统一消费原始 Anno、`arg.expr` 及 snapshot 身份，经现有 imported expression 编码进入同一池；源码注解消费 canonical 产品。

Alias self 与 RHS 已独立；Generic 回环的 reserve→fill 结构也已具备。Generic 最终 upperBounds、Alias 具体使用槽选择及严格 capture 仍 **OPEN**。

本轮只读，未修改文件、未跑 Gradle、未联系或停止旧会话。
