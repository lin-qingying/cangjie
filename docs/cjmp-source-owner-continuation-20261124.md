# CJMP SOURCE 原声明文件归属接续（2026-11-24）

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10、H6排除余额保留分母、v10待办及完整SOURCE验收不缩减；全部历史未提交修改保留，未提交或归档，目标保持active。

## 实际生产修改

`semantics.api/CfirProvider` 的 nullable classifier symbol 文件查询为必填abstract职责；nonnull open 只检查该symbol查询结果。名称查询与原symbol归属各自保留。`CfirCompositeProvider` 逐子provider传原symbol聚合，不继承ClassId转发。Empty/Library/LLLibrary/LLBuiltins明确无源码文件；Source/LL精确原索引保留。三个resolve测试provider按真实symbol文件登记或明确null迁移。`CfirSourceGraphProducer.sourceFile` 改用声明自身session的symbol overload，不借名称首命中文件。

原 Source bounds lifecycle 十四方法保留，新增四个双parser方法实际使用正常frontend产出的异session同ClassId声明。回归覆盖嵌套/反序聚合、陌生symbol、Empty/Library无源码、原owner文件缺失、已捕获/派生bounds失效与缺少原provider；未造dummy session/bounds/源码对象。测试中的组件替换只检查聚合查询接缝，不授完整LL换代保证。

固定K `3c7a5041f238102285764d370665f56f587f9dc4` 的 FirProvider 实际有ClassId默认转发，且固定对象无FirCompositeProvider。本次明确为CJ原owner产品需要的合同收紧，保留K的provider/LL缓存职责，不声称原样照搬不存在的类。固定N `14276740ffec1262b81b6b5236b1d840af270b37` 的节点文件/outer事实与固定K原TP containingDeclarationSymbol文件路径分别核验。新建四个skill角色考证及限定源码PASS记录见 `build/cjmp-source-owner-20261124/code-gate.md`。

## 独立真实验证窗口

| 队列 | 实际终态 | 范围与证据界限 |
| --- | --- | --- |
| 32a34022 | generateTree实际执行，DONE0 | 正式生成成功；终端曾截断，不声称完整终端。queue原状态保存在.gradle/queue/tasks/32a34022.json。不能按11月17日历史生成失败重新覆盖新候选。 |
| 67b643eb | 57/57，DONE0 | frontend owner18、resolve pattern8/organization3、SOURCE相关28；LL生产编译成功。11原gzip XML和逐key全部属该窗口。9,991选定输入两端SHA `AA19848C0A79325F93D8823A5E1E01300D5812F467852F105BF5FB8F0BEBA92F`。filter误用了accessibility文件名，该类没有实跑，不能计为通过；原StoredMember两方法也未包含。 |
| b1bdea01 | 实跑140项95通过/45失败，DONE1 | 完整frontend92为47通过/45失败；accessibility16/16；SOURCE相关32/32，包含原StoredMember两方法双parser四项，原SOURCE14必要集合真实包含。12原gzip XML和全部逐key属真实窗口。同9,991选定输入两端SHA，无漂移；该窗口不与57合并成一次结果。 |
| 4f723f48 | 配置真实SDK实跑169项159通过/10失败，DONE1 | 完整frontend92为82通过/原10失败，owner18、accessibility16、pattern8、organization3、SOURCE32全部通过。15原gzip XML独立保存，全属窗口。9,991选定输入两端SHA不变；SDK84的前次观察/终态观察SHA均为原527487…，明确配置CANGJIE_STDLIB_MODULE及CANGJIE_CJD_SDK_DIR。未与旧窗口累计。 |

两窗口各自 `{id}-{results,input-window,terminal}.json` 保存到 `build/cjmp-source-owner-20261124/`。输入算法复用read-current-evidence.ps1的明确选定roots，不声称覆盖generators/top-level脚本或全仓。SDK84在首轮launch后观察到原SHA `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`；不是该轮事前冻结SDK的证明。完整原XML逐字节gzip保存，未复用旧报告。

## 本轮完整前端失败与下一真实阻塞

未配置SDK的b1bdea01为47/45，其失败完整保留。大量class common/ordinary输出提前失败于 `SOURCE class inheritance requires a superclass published by SUPER_TYPES`；另有block-local消费失败为 `Nominal type has no actual classifier declaration`。SOURCEproducer原继承完整性guard保留；未清空继承表或删guard通过用例。当前共享 `CfirSupertypesResolution.addImplicitObjectSuperclass` 查询真实Object为空时返回旧refs，未配置依赖时原SUPER_TYPES未形成所需Object父边。

进一步实际读取环境发现早两轮未设置 `CANGJIE_STDLIB_MODULE`；当前 `CANGJIE_HOME` 指向临时cj120，并不会自动配置CjoSearchPath的独立stdlibSearchPaths。b1bdea01没有配置所采样SDK84，不能将其45失败直接归为provider修订造成或冒称configured-SDK回归。4f723f48使用明确的已核验SDK标准库搜索根重新执行完整92、owner18、accessibility16、pattern/organization和SOURCE32，已经真实终态。最新完整92以本轮82/10为准，不用旧轮82/10代替它。此前superclass/block-local提前失败在正常SDK窗口消失，十个原失败key精确恢复；无新增/删除失败key。

固定K/N新建考证已完成并保存 `supertype-kotlin-map.md`、`supertype-native-authority.md`。下一完整batch是声明symbol的已发布supertype refs/edges产品、缺真实Object时的结构化 `sema_no_core_object`、正常父边准备/缓存协议，以及退休scope/typed查询时生成phantom Object/Any和ownerClassId-only ImplicitObject统一SKIP。实际Object→Any来自官方stdlib显式ref，普通class→Object由唯一producer生成真实COMPILER_ADDED ref并入SOURCE继承槽。SDK/CJO拥有自己的已完成声明产品，不能强求它出现在当前Source graph store；未发布与合法空产品必须区别。测试角色正在依据两份固定证据准备完整装配/原期望核验，尚未应用新的父图batch。

此前四extend与generic enum五个正确失败所需完整batch考证已经独立保存：`classifier-kotlin-map.md`、`classifier-native-authority.md`。完整nominal结构先行、原owner/header/alias/attributes/传递bounds绑定、双侧私有actualize、锁外完整extend key等价类、精确pair root产品和CLI/LL/后置checker迁移仍OPEN；不能只修context或把raw桶赋0。先闭合新superclass阻塞，之后按该批完整写集实施。

## Native初始化断言证据冲突保留

固定N新考证记录在 `native-initializer-evidence.md`：普通static/global初始化式无该Native SOURCE保存资格，Native ASTWriter不写VarInfo.value；tuple测试NPE发生于specific编译之前，不能推断binding丢失。原三断言与旧真实失败全保留，未扩大保存资格、改正确期望、切test-only schema或从literal造值。CHIR的官方运行通道仅作为考证，不据此实施用户排除后端余额。完整产品范围仍需原计划逐项验收。
