# CJMP whole-declaration ERROR 容器与 stub 边界复核

> 2026-10-06 后续验证：合法源码的生产 whole-tree roundtrip、detached 查询与真实物理文件
> loader attach 已由 WholeTreeStubRoundTripTest 通过；名义成员 FQName 生成缺口已修，
> SOURCE_STUB_VERSION=213。handle8336 exit0，整树1/1、独立名称边界5/5。
> 下文“未执行”描述的是原始取证时点；whole ERROR 包装、dangling 场景及其完整矩阵仍未通过，
> 不能由合法基线外推。最新过程见 1003 交接中的“整树 stub 与名义成员 FQName”段落。

本次只读审查对应 v13 验证期间源码；没有修改生产代码、测试或运行 Gradle。以下是下一批五项 CFIR `PARSE_*` 迁移的实现门槛，不是已完成项。

## 现有实际结构

* `CjAbstractClassBody.declarations`、`CjForeignBody.declarations` 在 AST 分支只取直接 `CjDeclaration`。`primaryConstructors`、`secondaryConstructors`、`finalizers`、`properties`、`variables` 同样使用 direct stub/PSI children。whole-declaration `PsiErrorElement` 包装会使 AST 分支丢失实际直接声明。
* `CjFile.declarations` 遍历直接 children，排除 package、展开 foreign body、加入声明。ERROR 包装 file declaration 或 foreign directive 后，目前分支无法访问它。
* `CjDeclarationStub.getParent` 有 stub 时返回 `stub.parentStub.psi`，否则保留真正 AST parent。不能全局改写这个 API 来隐藏 ERROR。
* `CjPrimaryConstructor.getContainingTypeStatement`、secondary 和 finalizer 当前使用 `parent?.parent as CjTypeStatement`。在 `BODY -> ERROR -> CTOR` 下失败；泛型 ancestor 搜索又会越过宏输入和局部函数，故需要严格容器判断。
* `CjFunctionImpl.isLocal`、AST `isTopLevel`、`CjVariable`、`CjProperty.isMember` 和 `CjNamedDeclarationUtil.getParentFqName` 有直接 parent 分类；wrapped declaration 会被误分类。function stub creation 读取 `psi.isTopLevel` 并持久化，错误分类会污染序列化结果。
* `CjMacroExpression.unwrappedDeclaration` 只在 `hasDeclarationInput` 为真时沿直接 input/嵌套声明宏链读取；`MACRO_INPUT` 本身没有 stub，stub carrier 直接位于宏 stub 下。这个边界必须保留，不能把 macro input 算成实际 body 的 constructor。
* `CjEnumBody.constructor` 已有直接 macro carrier 展开规则；这不授权 primary/secondary constructor getters 采用宏展开或任意后代查找。
* LT `buildFileDeclarations`、`extractClassMembers` 遍历直接节点；foreign 函数使用 direct `getChildrenByType(FUNC)`；`findPrimaryConstructorNode` 在实际 body 上 direct `findChildByType(PRIMARY_CONSTRUCTOR)`。whole-declaration ERROR 同样隐藏成员。
* `LightTreeModifierList` 的 foreign 属性读取 `tree.getParent(declarationNode)?.tokenType == FOREIGN_BODY`，需要使用同一严格 ERROR parent 规约。

## 方法级修复清单

1. 在 PSI 所属模块的 `CjDeclarationContainer.kt` 定义统一的实际容器子元素读取 API。AST 只递归展开 `PsiErrorElement`，保持源码顺序；遇到任意非 ERROR 节点立即交给 caller，不再深入。该 API 不展开 macro input、annotations、nominal body、函数 block。stub 分支读取实际直接 stub children；ERROR 不是 stub container。
2. `CjAbstractClassBody.declarations` 及其 typed getters、`CjForeignBody.declarations`、`CjFile.declarations` 都使用这同一边界。file 的 foreign 展开仍是 file 自己的显式语法规则，不添加到通用递归工具。
3. 在 `CjStubbedPsiUtil.kt` 增加只跳过连续 ERROR 的直接 owner API。不要改变现有 `getPsiOrStubParent` ancestor 查询的意义。constructor/finalizer owner 必须先确认直接有效 parent 是 `CjAbstractClassBody`，再确认 body 的直接有效 owner 是 `CjTypeStatement`；遇到 macro/input/block/其他声明停止。
4. 迁移上述 local/top-level/member/FQName callers，以及 stub `createStub` 所调用的分类入口；stub-only 情况以真实 `parentStub` 的类型/祖先边界判断，不能因为 detached file stub 的 `psi == null` 推断 local。已持久化的 top-level/classId/FQName 字段继续遵守既有 serializer 协议。
5. 在 LT 同模块 `LightTreeUtil.kt` 增加专用于 actual declaration container 的 ERROR 透明枚举和只跳 ERROR 的 parent API。**不得**改变通用 `forEachChildren`、`findChildByType`、`getChildrenByType` 为任意后代查询。
6. `buildFileDeclarations`、foreign actual direct members、`extractClassMembers`、`findPrimaryConstructorNode` 和 foreign modifier context 显式使用上述 LT API。pending annotation 顺序维持；宏节点仍交给原有宏转换路径；不把宏输入中的 ctor提升给宿主。
7. PSI raw builder 消费 `CjFile`/body getters 后可获得实际包装声明；进一步检查其 declaration 分类和 `hasError` 只针对 declaration 自己的语法错误。外层 CJMP 整声明 ERROR 不应凭空生成另一个名字/ClassId；原有非法 nested nominal 的 `CfirInvalidDeclaration` 合约仍然适用。

## 2026-10-06 补充：实际参数容器不能遗漏

本节由独立只读 mapper 对当前一方实现与 Kotlin 对位实现逐入口复核；没有执行下列新 gate。
`CjParameterList` 不是 `CjDeclarationContainer`，应独立保留实际参数列表的文法边界。
低层连续 ERROR 的 child/parent 规约可以共用，各调用者仍明确自己的实际容器。

* `CjParameterList.parameters` 当前直接读取 VALUE_PARAMETER；整参数 ERROR 会使 AST 丢参数，
  而 DefaultStubBuilder 的非 stub ERROR flatten 又可能使 stub 路径保留参数。
  两条路径都必须保留首、中、末参数的源码顺序。
* `CjParameter.ownerFunction` / `ownerDeclaration` 和 `checkParentOfParentType`，以及
  `CjParameterList.ownerFunction`，需要在每条直接 owner 边上只跳连续 ERROR。
  参数的有效直接父必须是实际参数列表；列表的有效直接父按现有 function、constructor、
  function type、lambda、catch 文法判断，不能寻找最近函数或构造器。
* `CjPsiUtil.getClassIfParameterIsProperty`、`getEnclosingElementForLocalDeclaration`，
  `psiUtil.CjPsiUtil.isPropertyParameter` / `containingTypeStatement`、参数 useScope 和
  `CjNamedDeclarationUtil.getParentFqName` 都依赖上述实际 owner。
  `CjParameterElementType.createStub` 读取 `psi.fqName`，错误分类会写入持久化事实；
  不能在 serializer 新增字段或反序列化时猜测 owner 来掩盖 AST 分类错误。
* PSI raw 的各 callable 转换消费同一个 `CjParameterList.parameters`；参数自身类型、默认值、
  注解和 source 不改为外层 ERROR source。containingDeclarationSymbol 由实际 callable 传入，
  不能将 raw parameter 的既有 `isLocal=false` 改成 PSI local 分类来修复漏读。
* `PsiRawCfirBuilder.addPrimaryConstructorParameterFields` 尚有
  `PsiTreeUtil.findChildrenOfType(body, CjNamedFunction)` 按类名查找的旧后代兜底。
  它必须由实际 primary 身份路径替换，不能随 ERROR 透明实现继续保留。宏 replacement
  产物的 primary 支持必须明确产物身份，不能借宿主后代的同名函数参数生成字段。
* `LightTreeRawCfirDeclarationBuilder.extractValueParameterNodes` 与
  `hasVariableLengthArgument` 尚有 `findFirstDescendantByType(VALUE_PARAMETER_LIST)` 兜底。
  应限定到声明自身实际直接签名列表，只 ERROR 透明；再在该列表中只 ERROR 透明地枚举
  VALUE_PARAMETER。函数体、嵌套 callable、macro input 的列表不能成为该声明签名。
  同一边界还必须用于 `countValueParameters`、`isSubscriptSetOperator`、
  `commaListPosition`、`collectFunctionBodyDiagnosticData` 的重复列表记录，以及
  `addPrimaryConstructorParameterFields` 的节点/CFIR 参数 zip。
  `...` 的过滤仍属于实际签名列表；macro 逗号位置读取未过滤的真实顺序。

Kotlin 对位证据为 `KtParameterList.java` 的直接参数/owner、`KtParameter.java` 的列表 owner
与作用域分类、`KtParameterElementType.java` 的 FQName 持久化，以及
`LightTreeRawFirDeclarationBuilder.convertValueParameters` 对实际 VALUE_PARAMETER_LIST 的直接枚举。
PSI raw primary 参数与 FIR 参数 zip 是身份关联依据；仓颉字段模型仍按原生反糖，不能改成 Kotlin property。
Kotlin 并无通用后代查询来补取参数；仓颉整参数 ERROR 透明是自身诊断范围载体所需的语法适配。

新增验收须覆盖：真实 parser 的 whole-parameter ERROR 首/中/末顺序与范围；primary 字段
参数与对应 CFIR 参数身份及不移位；operator 参数数量/下标 `value!`、foreign `...`、
parameter macro 逗号位置；function type/lambda/catch/macro 参数不提升；PSI/LT NORMAL/LAZY
的 symbol/default/type/source；whole-tree roundtrip 的参数 parent/order/dangling/FQName；
detached 和 fresh loader attach 的 owner/property/local/FQName 查询前后不加载 AST。

## Kotlin 声明容器对照

已读取主仓库只读镜像中的：

* `external/kotlin/compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtClassBody.kt`：declarations 与 typed members 均 direct stub/PSI children；未提供通用 DFS。
* 同目录 `KtStubbedPsiUtil.java`：有 stub 则 `getParentStubOfType`，否则 `PsiTreeUtil.getParentOfType`。它是既有 ancestor API，不能冒充严格 actual direct owner API。
* `external/kotlin/analysis/stubs/testFixtures/org/jetbrains/kotlin/analysis/stubs/StubsTestEngine.kt`：逐节点调用 serializer/deserializer、检查字节耗尽与类型，然后重建子树。这能证明字段 serializer，却不能证明 IntelliJ whole-tree dangling metadata 或真实 PsiFile stub attach。

## 已核验的 IntelliJ 253.29346.379 入口

读取安装 SDK 的 `core-impl-253.29346.379.jar`、`indexing-impl-253.29346.379.jar` 的 `javap -p -c` 字节码：

* `DefaultStubBuilder.StubBuildingWalkingVisitor` 将当前 parent stub 传递到未生成 stub 的节点的子节点遍历。ERROR 不创建声明容器 stub，子声明的 stub 因此位于最近实际 stub owner 下。
* `StubTreeSerializerBase` 检查 `ObjectStubBase.isDangling`/`StubBase.isDangling`，额外写入 dangling 协议标志；反序列化调用 `ObjectStubBase.markDangling`。仅调用每个 element type 的 serializer 并不会保存这层 metadata。
* `SerializationManagerImpl` 提供 `serialize(Stub, OutputStream)` / `deserialize(InputStream)`，构造需要 CoroutineScope（或 Path、readonly 参数）。serializer 注册和名称存储属于真实服务生命周期；不能手动绕过后声称 whole-tree 已验证。
* `PsiFileImpl.getStubTreeOrFileElement` 经 `StubTreeLoader.getInstance().readOrBuild(project, virtualFile, file)` 获取 `StubTree`，内部私有 `setStubTree(PsiFileStub)` 才执行关联与安装。`PsiFileImpl.isContentsLoaded` 检查实际 AST 是否存在。
* `StubTreeLoader` 是可替换服务，公开 `readOrBuild`、`build`、`readFromVFile`、`rebuildStubTree`、`canHaveStub`。测试应通过正常 loader 服务返回真实 roundtrip tree，使 `PsiFileImpl` 执行正常 attach；禁止反射调用 private setStubTree，或仅 root.setPsi(file) 冒充完整 attach。

## 2026-10-06 补充：真实 whole-tree / physical attach harness 入口

独立 gatekeeper 复核 IntelliJ 253.29346.379 字节码，以下是可实施的生产 API，
尚未在当前 fixture 运行，不是通过证据。

* `SerializationManagerImpl(Supplier<Path?> { null }, false, application.getCoroutineScope())`
  的 null path 是生产支持的 `InMemoryDataEnumerator` 模式。向 MockApplication 注册
  `SerializationManagerEx` 服务并调用 `initialize()`；使用 manager 的
  `serialize(originalRoot, output)` / `deserialize(input)`，不逐节点重建协议。
* `serialize` / `deserialize` 自动执行 `initSerializers()`；该方法经
  `IStubElementType.loadRegisteredStubElementTypes()`、`StubElementRegistryService` 和
  `IElementType.mapNotNull` 收集真实 serializer。注册方法为 private，不反射调用。
  CoreApplicationEnvironment 已注册 StubElementRegistryServiceImpl 与对应 extension EP。
  当前 fixture 仍需实际确认/补足 `StubElementTypeHolderEP.EP_NAME` 和
  `StubIndexExtension.EP_NAME`，不能假定 headless bootstrap 已注册。
* 生产 whole-tree serializer 恢复 dangling 标志及 `PsiFileStubImpl.stubRoots`。
  roots 要与 fresh provider 的 descriptors 一致；逐节点字段 roundtrip 不覆盖该事实。
* `AbstractFileViewProvider` 的 physical 条件同时要求 eventSystemEnabled、virtualFile
  不是 LightVirtualFile、fileSystem 不是 NonPhysicalFileSystem。
  现有 CjParsingTestCase.createPsiFile 使用 LightVirtualFile，传 true 也不是 physical；
  `PsiFileImpl.getTreeElement()` 对非 physical provider 会先加载 AST。
* 使用实际物理文件、Core localFileSystem 的 `findFileByNioFile` 和
  `PsiManager.findFile` 创建 fresh CjFile。其 virtual file 与原始 PSI 不同、文本完全一致。
  续查实际 MockComponentManager 方法体发现 `replaceRegularServiceInstance` 与
  `unregisterService` 均抛 UnsupportedOperationException，不能用作 fixture 替换；
  上轮仅据签名提出的替换调用已撤回。`unregisterComponent(Class)` 同样不可用。
  实际公开实现是 `application.picoContainer.unregisterComponent(serviceClass.name)`，
  再调用 `application.registerService(serviceClass, instance)`；Mock 的实例注册按 Class.name
  作为 key 且不先移除。finally 应以同一 key 移除测试实例、重新注册原 loader。
  该实际方法体已核实，fixture 运行仍待验证。
  loader 必须严格核对 project/file/PSI 身份，只对该文件
  `readOrBuild` 返回 `StubTree(restoredRoot)`，其他入口不建 AST。
  测后恢复服务或依托完整 fixture application 关闭，避免污染其他测试。
* detached 查询与 attach 用两次独立反序列化的树，不能复用已物化 child PSI 的 detached tree。
  fresh provider physical、`isContentsLoaded == false` 先断言；公开 `freshFile.stubTree`
  经 SDK 正常 loader/内部 setStubTree 完成 attach，核对调用次数/root 身份/根 PSI 关联。
  测试不调用 root.setPsi 或私有 setStubTree。随后每项 getter 查询都核对 AST 未加载。

上述服务初始化、EP、physical 文件与查询 gate 尚需实际实现、执行；字节码取证不能代替运行验证。

2026-10-06 真实编译补证：SDK CoroutineScope 签名所需依赖是
`com.intellij.platform:kotlinx-coroutines-core-jvm:1.10.1-intellij-5`，不是缓存的外部 Kotlin
1.8/1.9。固定 core-impl/ide-impl/util-coroutines POM 均将其登记为 runtime，现有 aggregate
transitive 不向 testCompile 暴露该 API。已在版本目录登记准确 SDK fork、在
dependencies:intellij-core 显式 api；不放开整个 SDK compile 传递图。
Supplier 本体非空而 get 返回值可空，Kotlin 必须使用 Path?；MockApplication 的
getCoroutineScope 是显式函数，不可写属性。CjParsingTestCase 的 inherited JUnit3
assertNotNull 返回 Unit，值读取必须明确 kotlin.test.assertNotNull。新 harness 已修复
这些实际编译错误，仍需重新编译/执行，不能把 SDK 静态审查接受当成 gate 通过。

## 全部必须通过的测试 gate

* 真实 parser 产生 whole-declaration ERROR，不能测试手工构建的伪 AST。heavy 与 light 同一文本的 declaration type/order/range、syntax diagnostics range/数量均一致。
* File、foreign、class/interface/struct/enum/extend body 分别覆盖包装声明；typed getters 与通用 declarations 一致；直接构造器被识别，nested nominal/function-local/parens macro input constructor 均不提升给宿主。
* Annotation/macro carrier 顺序与原有合法语法保持；直接声明宏与括号 token input 分开，错误包装不成为任意 macro unwrap 授权。
* PSI/LT raw CFIR 的 direct members、primary identity、param-field promotion、foreign 修饰符和源码范围一致；验证 NORMAL/LAZY 模式。
* 一份真实 `CjFileStubBuilder` 树经 `SerializationManagerEx`/`SerializationManagerImpl` whole-tree roundtrip 后比较 stub type/order/parent、dangling flag、topLevel/FQName/classId/body facts。当前逐节点 roundtrip tests 仅为已有字段测试，不能替代这一项。
* detached roundtrip root 没有 file PSI 时，使用真实 stub parent 读取 typed declaration owner/classification，确认没有 AST 读取。
* fresh file 通过实际 `StubTreeLoader.readOrBuild` 返回上述 tree，读取 file/body declarations、constructor owner、topLevel/local/FQName 前后都断言 `PsiFileImpl.isContentsLoaded == false`。触发 AST 的 getter视为失败；不通过缓存旧 PSI、root.setPsi、反射/手工 parent 关联规避。

以上 whole-tree roundtrip 和 fresh file attach gate 本次没有执行，仍需实现并验证。
