# 原 CJMP 方案前端子集实施与验收（2026-11-17）

> 当前默认参数/lambda必要依赖定向集已核验75/75：queue `d6c71a37` DONE/退出0，复制19、metadata36、生命周期18、原负例2，无失败/错误/跳过；六原XML15,334字节全部在真实窗口，6826输入事前/事后SHA一致。[完整终态](cjmp-source-helper-20261117/front-default-final-75-results.json)。新增late/OBL/PCLA四例在该轮均通过，下面71/75及新四失败保留为历史。该轮没有SOURCE14或原条目全量验收，不产生新的总体百分比。

当前目标是完整完成原七十项、P0–P10及有效修订中的前端部分，排除全部后端；不扩展成完成整个仓颉编译器所有前端语义。唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原总账、全部历史修改、正确fixture/期望保留，不擅自归档或提交。

## 范围与真实依赖

固定源码逐项映射已落盘：[Kotlin模块/consumer/gate](../build/frontend-semantics-20261117/kotlin-cjmp-plan-frontend-mapping.md)与[官方v113原ID/阶段/前端后端边界](../build/frontend-semantics-20261117/native-cjmp-plan-frontend-mapping.md)。两报告均只读取证，70原ID映射不代表实施或行为完成，全部原验收条件保留。

依据[原计划](cjmp-framework-review-and-fix-plan-20260928.md)每个原条目、子要求及验收条件实施。原A–J七十个问题ID均含前端工作，保留70/70；H6为混合要求，只排除其历史后端余额，其加载/CLI/模式/版本前端门保留。P0/P10中的后端转换、codegen/link/机器码运行不实施，官方cjc取证与前端模式/模块元信息校验仍可执行。

CJO/SOURCE读写不能概括删掉：原H1/H9的完整前端声明、类型、FullId、默认/注解/编译期内容和common依赖重导出，以及H10/H11/H13来源位置仍须完成；属于这些前端交付的正文恢复按原完整条件验收。纯后端消费不实施。当前十个正确SOURCE失败保留，其4/14是产物切片结果，不等同于前端总完成率，也不要求先扩整个编译器才能推进其它原条目。

R5不是原七十个问题ID。Raw helper的owned-copy、绑定/正常BODY/CFG是C5/E6/G1及原源码helper附项的必要前端依赖，继续修；通用解析态copy后的全框架重建必须先证明实际前端消费者或原交付依赖，不能仅因旧S2审查列了它就把全K2 DFA或无关语言特性作为新目标。保留当前实现和拒绝边界，不能将它算完成。

## 前端子集核算

[逐ID机器账](cjmp-frontend-subset-20261117.json)以磁盘原七十行与原标题生成，没有删ID：范围映射70/70，纯后端问题ID排除0，混合ID1（H6后端子要求排除）。旧原账仍57历史整项、12部分、C3零；该计分和旧91.22%不作为现在前端子集完成率。

确认仍待实施或完整验收的前端原ID为C3/C4/D5/E6/E7/E8/E9/E12/F4/G1/G3/G5；H1/H9/H10/H13后续前端产物附项继续开放，H6前端版本门需其完整原矩阵复验。P0/P10、全量CFIR/LL/API/IDE前端与v10比较也未被局部测试覆盖。历史分子没有覆盖这些附项，当前没有可验证的新整体百分比；按每个原前端子要求实际关闭后核算，不能仅扣一条后端余额就发布新进度。

当前default/helper两个parser十四生命周期曾14/14通过，copy19/S036及原负例2/2有独立同代证据；新增四late/OBL/PCLA用例目前失败，当前scope中继续修复。它们是必要依赖的定向证据，不计作整个原条目闭合。[实际接续和ledger](cjmp-source-helper-lifecycle-20261117.md)保留每个独立窗口、失败与编译限制。

原前端必要依赖定向集实际71/75（copy19、同代metadata36、旧lifecycle14、原负例2通过，新late/OBL/PCLA四失败），6826输入前后SHA一致，6原XML真窗口：[inputs](cjmp-source-helper-20261117/cjmp-front-default-75-inputs.json) / [results](cjmp-source-helper-20261117/cjmp-front-default-75-results.json)。该94.67%的切片比率也不是原计划前端总完成率。当前已继续修参数列表的实际表达式阶段闭包及已有initializer错误的目标候选失败；新四回归尚未计为通过。

## 原条目前端产物和验收索引

下表是范围/状态索引；每项保留原计划全文的红测试、模块、consumer、跨版本和P10验收条件，不能用此短索引缩减。

| 原ID | 原问题 | 前端实施层/产物 | 当前验收边界 |
| --- | --- | --- | --- |
| A1 | 配对阶段排在 IMPLICIT_TYPES 之后 | P1：配对阶段与读取 | 历史实现闭环；当前全门需复验 |
| A2 | 读取配对存储时不先推进阶段，并依赖反向查表 | P1：配对阶段与读取 | 历史实现闭环；当前全门需复验 |
| A3 | 同名组被整组重跑，失配记录不幂等（中） | P1：配对阶段与读取 | 历史实现闭环；当前全门需复验 |
| B1 | 反序列化把 FROM_COMMON_PART 当成 common | P2：common/来源资格 | 历史实现闭环；当前全门需复验 |
| C1 | specific 类的成员 scope 不含 common 成员 | P3：合并成员、遮蔽、默认读取 | 历史实现闭环；当前全门需复验 |
| C2 | 泛型实例化检查器在 checker 内部自建 common 投影 | P3：合并成员、遮蔽、默认读取 | 历史实现闭环；当前全门需复验 |
| C3 | 跨 session 成员重载冲突按 fixture 形状打补丁 | P3：合并成员、遮蔽、默认读取 | 前端余额或完整验收未闭 |
| C4 | 遮蔽只在调用解析里做后置过滤，且判据有三套 | P3：合并成员、遮蔽、默认读取 | 前端余额或完整验收未闭 |
| C5 | 默认值读穿只接到了调用解析 | P3：合并成员、遮蔽、默认读取 | 历史实现闭环；当前全门需复验 |
| D1 | owner 组的泛型视图用错了 receiver，靠补回原始声明来修补 | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D2 | 早退守卫按 fixture 形状添加，掩盖了补签名绕过 CJMP 遮蔽 | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D3 | 接口 default peer 判定必须使用有方向的 owner/peer 关系 | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D4 | 两段式测试的期望被翻转 | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D5 | scope 按实例化后的签名判定实现关系，`memberOwnerExtend` 只在 owner 模式下修补（中） | P4：声明级继承/owner/可见性 | 前端余额或完整验收未闭 |
| D6 | 内建目标（CPointer）路径新增的可见性过滤丢掉了 owner 自己的 private（中） | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D7 | 跨包的 EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE 挂在实例化检查器上（中） | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D8 | CJO owner 的可见性用手写规则近似（低） | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| D9 | 共享 scope 带了只为一个检查器服务的专用开关（低） | P4：声明级继承/owner/可见性 | 历史实现闭环；当前全门需复验 |
| E1 | 抽象类成员检查的条件与官方不一致 | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E2 | 注解匹配是单向的，漏掉 nominal，排除表取自 origin/main | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E3 | 超类型只比较个数 | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E4 | 修饰符冲突表不区分作用域，还在 extend 处打了补丁 | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E5 | common let 的不可变赋值被拆成一个独立检查器（中） | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E6 | PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION 缺少 SPECIFIC 豁免（中） | P6：匹配/成员/注解检查 | 前端余额或完整验收未闭 |
| E7 | COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED 的禁止集不是官方的，另有跨检查器抑制（中） | P6：匹配/成员/注解检查 | 前端余额或完整验收未闭 |
| E8 | 泛型约束比较没有用上存储里的外层类型参数映射（中） | P6：匹配/成员/注解检查 | 前端余额或完整验收未闭 |
| E9 | `CfirCommonSpecificExtendChecker` 按名字判重（中） | P6：匹配/成员/注解检查 | 前端余额或完整验收未闭 |
| E10 | 遗留的启发式判断与死代码（中） | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E11 | property 的 mut 不一致被报成修饰符诊断（中） | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E12 | K2 结构偏离：AbstractCjmpChecker 与 MppCheckerKind 都没有落地（中） | P6：匹配/成员/注解检查 | 前端余额或完整验收未闭 |
| E13 | 隐式 abstract 的例外只覆盖函数，同一规则在三处各写一份（中） | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| E14 | 检查器的零散问题（低） | P6：匹配/成员/注解检查 | 历史实现闭环；当前全门需复验 |
| F1 | 形参类型等价用手写的结构比较，最后退回 `==` | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| F2 | 模式变量走一条绕开共享 first-fit 的专用路径 | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| F3 | enum 构造器配对绕开共享 matcher（中） | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| F4 | 模块同一性按名字判定，DAG 只做了传递过滤（低） | P5/P1：共享匹配/模块/类型等价 | 前端余额或完整验收未闭 |
| F5 | nominal 泛型参数个数的规则比官方判据宽（低） | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| F6 | 外围容器有四个来源，含 ThreadLocal 和一条绕开不变量的死路径（低） | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| F7 | 种类判别用字符串并带兜底；MISSING_BODY 与第二次绑定的判定顺序和官方相反（低） | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| F8 | 配对存储没有区分读接口和写接口（低） | P5/P1：共享匹配/模块/类型等价 | 历史实现闭环；当前全门需复验 |
| G1 | 生产 LL 会话拿不到 CJMP 模式和 dependsOn 关系 | P7：LL/Analysis API生命周期 | 前端余额或完整验收未闭 |
| G2 | 门关闭时不推进阶段，BODY_RESOLVE 的前置阶段随 session 变化（中） | P7：LL/Analysis API生命周期 | 历史实现闭环；当前全门需复验 |
| G3 | "按文件真实归属找模块"只修了测试 provider（中） | P7：LL/Analysis API生命周期 | 前端余额或完整验收未闭 |
| G4 | LL extend provider 合并漏掉了常规源码依赖（中） | P7：LL/Analysis API生命周期 | 历史实现闭环；当前全门需复验 |
| G5 | LL 的 TYPES 阶段容器配置是手抄的副本（中） | P7：LL/Analysis API生命周期 | 前端余额或完整验收未闭 |
| G6 | 多 common 模块的 first-fit 固化了 v1.1.3 不支持的语义（中） | P7：LL/Analysis API生命周期 | 历史实现闭环；当前全门需复验 |
| G7 | `isReanalyzableContainer` 照搬 Kotlin，推断返回类型下会保留过期的签名和配对（中，既有问题被本批放大） | P7：LL/Analysis API生命周期 | 历史实现闭环；当前全门需复验 |
| G8 | 覆写测试调换了求值顺序（低） | P7：LL/Analysis API生命周期 | 历史实现闭环；当前全门需复验 |
| G9 | LL 按包枚举文件时借用了 facade 的契约（低） | P7：LL/Analysis API生命周期 | 历史实现闭环；当前全门需复验 |
| H1 | `_CNat3AnyE` 硬编码：为了让合成的默认上界能往返而打的点补丁 | P8：CJO/CLI/诊断前端产物 | 前端余额或完整验收未闭 |
| H2 | common part 按"父目录 + 包名搜索"加载，而不是按显式路径 | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H3 | CLI 路径下的 common 编译不产出 CJO（原审查问题，2026-10-13 已闭环） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H4 | `CjmpDeserializedCommonSideReporter` 是 CLI 层的字符串旁路（中） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H5 | features/options 门按内容启发式决定作用对象（中） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H6 | 加载门、CLI 形态和模式判据只实现了 origin/main 的行为，没有语言版本门禁（中） | P8：CJO/CLI/诊断前端产物 | 保留加载/CLI/模式/版本前端门；排除旧后端余额 |
| H7 | 门的比对输入：options 只有测试能设，LightTree 下 features 恒为空（中） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H8 | 加载门诊断只在 CLI 中外显（中） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H9 | specific 编译写出的 CJO 不包含 common part 的声明（中） | P8：CJO/CLI/诊断前端产物 | 前端余额或完整验收未闭 |
| H10 | identifierPos 另起了一套定位（低） | P8：CJO/CLI/诊断前端产物 | 前端余额或完整验收未闭 |
| H11 | CJO 位置到诊断位置没有统一的 owner（低） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H12 | 排序修补放错了层（低） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| H13 | `recordCjoDeclarationPosition` 忽略了 `Position.pkgId`（低） | P8：CJO/CLI/诊断前端产物 | 前端余额或完整验收未闭 |
| H14 | 生产路径里残留了调试探针（低） | P8：CJO/CLI/诊断前端产物 | 历史实现闭环；当前全门需复验 |
| I1 | 推断返回类型时的错误过滤只看根类型（中） | P9/P1：resolve/import/类型前端依赖 | 历史实现闭环；当前全门需复验 |
| I2 | 歧义包限定符靠回滚错误引用来触发重新解析（中） | P9/P1：resolve/import/类型前端依赖 | 历史实现闭环；当前全门需复验 |
| I3 | `CfirTypeResolver` 里按 `CjBinarySourceElement` 分出的分支只有单测能走到（低） | P9/P1：resolve/import/类型前端依赖 | 历史实现闭环；当前全门需复验 |
| I4 | import binding 按 session 存储，LL 需要为外部 session 的文件补录（低） | P9/P1：resolve/import/类型前端依赖 | 历史实现闭环；当前全门需复验 |
| I5 | `coneTypeSafe` 为一个从未被构造的节点加了分支（低） | P9/P1：resolve/import/类型前端依赖 | 历史实现闭环；当前全门需复验 |
| I6 | 测试辅助的阶段顺序与生产相反（低） | P9/P1：resolve/import/类型前端依赖 | 历史实现闭环；当前全门需复验 |
| J1 | `runCatching` 静默兜底（中） | P1/P10：组件与不变量 | 历史实现闭环；当前全门需复验 |

## 当前实施顺序

1. 完成当前C5/E6/G1相关default/helper/lambda生命周期必要修复：保留PRIVATE/实际receiver/正确source范围、前序fresh参数及泛型，正式候选提交和最终signature/body/CFG/phase一致；原十四和新增late/OBL/PCLA四例保留严格断言。现有问题为late nested phase仍IMPORTS及实际OBL choose歧义，禁止用phase硬写或改正确源码/期望处理。
2. 按C3/C4/D5的原共享PreCheck、Names过滤、actualizing scope、声明级继承/extend与真实cache epoch消费者完成余额；E8/E9/F4的泛型映射、原始signature/目标/接口/约束key与模块/version/DAG跟随其真实依赖。前端实例化后成员/签名检查继续，后端实例化不实施。
3. 继续E6/E7/E12及H1–H14的前端交付：source/macro/CJO默认实现和注解规则一致，完整元信息/CJO重导出/位置/编译期必要内容，所有CLI/LL前端错误通过统一诊断。保留原SOURCE前端产物测试，S3–S6中确属原前端交付的工作完整实施。
4. 完成G1/G3/G5及原G组LL/AnalysisAPI/standalone/LSP/IDE前端正常生命周期、文件归属、phase-lock、重建/取消/失效及source/CJO对照。没有真实consumer证据不闭项。
5. P0/P10前端收口：固定Kotlin/官方源码、两parser、source/ordinary/common/depends-on库和原版本矩阵、实际testcase-key无新增失败、前端API/IDE/document检查。v10待办保留但暂缺不阻断其它授权工作；后端排除不计为通过。

所有Gradle使用gradlew-queue.bat；PowerShell7和UTF8读取；仅apply_patch写文件。主树及external只读，不联系停止会话，不回退全部历史修改。目标仍active，未完成、未归档。
