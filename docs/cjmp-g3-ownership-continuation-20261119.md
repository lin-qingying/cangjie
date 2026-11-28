# G3 文件真实归属接续（2026-11-19）

按[原G3/P7](cjmp-framework-review-and-fix-plan-20260928.md)及[交付板](cjmp-source-delivery-board-20261119.md)实施；原70前端/P0–P10、正确源码和全部历史修改保留。此轮只交付standalone生产文件owner的候选消歧子要求，G3整项继续OPEN。

固定源码与真实剩余写集见[并行G3报告](../build/cjmp-parallel-20261119/g3-module-ownership-map.md)。Kotlin固定提交的standalone实现是首个contentScope匹配，没有原计划新要求的use-site多候选消歧；本轮按用户原计划增加该本地合同，不冒称原样复制Kotlin。native文件curFile/curPackage不可被查询方改写，语言包名不替代Analysis模块对象。

生产CaStandaloneProjectStructure现在先按实际根覆盖、根深度/kind优先级及最终contentScope得到真实候选；唯一个owner不受use-site覆盖，多候选只允许同一候选模块对象消歧，无有效偏好明确拒绝。不会按名称、来源查询方或列表首个强行归属。既有builtins/special/dangling/outside-content模型保持其真实缺失/上下文合同。

首编译56271ada因必要decompiler依赖遗漏CfirParenthesizedTypeRef分派而失败；已在TypeCjoStubBuilder保留真实PARENTHESIZED_TYPE和独立inner TYPE_REFERENCE，不加else/fallback或删除括号。第二7041b884完整standalone及依赖编译退出0。

真实定向窗口a35c5d7a于2026-11-19T09:06:31.737Z至09:11:19.567Z结束，7/7，0失败/错误/跳过。覆盖四个新candidate/scope-exclusion场景、原深根优先、两源码依赖归属、common/specific显式模式及配对调用。9904输入前后同SHA AF3282E573735B288539C37623DA6D1F2D8D1AD0B9D8787E4CC86EF07CC89C2B；原XML2353字节真实窗口且gzip/SHA往返核验。[事前输入](../build/cjmp-parallel-20261119/g3-standalone-inputs.json)与[逐case/原XML](../build/cjmp-parallel-20261119/g3-standalone-results.json)独立保存，不与SOURCE18或旧窗口拼计数。

实际新增fixture为standalone/testData/sessionBuilder下sameRootEqualPriorityOwner、sameRootEqualPriorityOwnerRejectsNonCandidatePreference、contentScopeExcludedOwnerCannotOverrideUniqueOwner、contentScopeExcludedOwnerCannotDisambiguateRemainingOwners；既有测试期望未降低。

下一G3完整关闭仍需LSP生产registered/overlay/disk最终contentScope一致性、同包重名配置模块的独立身份与无关深根排名、真实LL compilation target/origin模块路由及错误moduleComponents拒绝、combined provider保持真实owner/classpath顺序，以及原P7与完整parity审查。未以standalone7项替代这些门，也未关闭G1/G5或原70项。
