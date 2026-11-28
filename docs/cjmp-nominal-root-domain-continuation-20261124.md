# CJMP nominal声明域／原物理根接续（2026-11-24）

原70项/P0–P10与完整SOURCE、92项前端集成验收保持原范围；本次交付不关闭整项。唯一写树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，历史未提交修改、正确断言和定时任务全部保留。CLI程序/包装/发布后置，不后置92项；H6排除余额保留分母，v10待办不阻断授权实施。

## 实际生产交付

tree/session 的 `CfirCjmpClassifierDomain` 进入原 `CfirCjmpPreparedGroup` 和 `CfirCjmpStructureSnapshot`，必填、非空、无默认。原Batch准备入口捕获实际session/module、原refinement闭包对象及其顺序、实际classifier symbol→declaration/module，不从Name/ClassId/path或发现组推导身份。保留记录对象的绑定检查与TYPES header门，不修改阶段。

物理root证明分两类：Source只保存所属原 `CfirFile.declarations` 的实际对象/槽、file绑定/module/package；非Source只保存原CJO声明header/origin对象及其实际 `isTopLevel` 位。CJO origin新增必填该位，原DD按 `decl.isTopLevel` 发布，wire schema及默认值不改。缺失loaded origin不改走Source文件；Source缺文件槽也不借CJO origin。重复同对象file可枚举，异对象同symbol拒绝，不静默first选择。

原Snapshot的 `classifierRelationFor` 只提取其实际selected nominal seed：同时验证两端原root、原包/模块域、实际Class/Interface/Struct/Enum种类、原角色和TP symbol顺序、绑定/header仍属原对象。合法无seed返回关系缺失；未知/失效输入失败，不产生identity。嵌套声明即使被追加到发现组并由原纯merge产生边，也不能据列表资格升为root。元数不等的原nominal结构规则及空TP mapping保留；不在本接口判完整泛型兼容。

六个既有测试文件、七条PreparedGroup装配路径已迁移真实session与本组原声明，没有新建dummy session、扩refinement或翻旧断言。`CfirCjoDeclarationOrigin` 的现有构造点也显式迁移：生产来自实际wire位，回读断言保留原path/index并观察对应wire事实；Source snapshot变更标记不是loaded header授权。

## 独立证据与全部失败保留

证据位于 `build/cjmp-frontend-integration-20261124/`。各id的results包含完整gzip原XML、长度/SHA、逐key结果和实际窗口；terminal与input-window单独保存。不同窗口不累计。

| 窗口 | 实际独立结果 | 不冒称的范围 |
| --- | --- | --- |
| b994f624 | 生产编译失败：新文件缺resolvePhase扩展import；行为0 | 原终端保存；随后已修import。 |
| cd44b296 | 早期tree/providers编译成功 | 早于最终root witness/source分流及包守卫，不替代最终字节。 |
| 995f7e8e | 早期serialization/providers编译成功 | 编译期间及之后仍有源码修订，不声称整体输入冻结或行为验收。 |
| 85a0c53e | 实跑464项454通过/10失败：serialization370/370、raw2/2、完整前端92为82通过/10失败；resolve模型因测试API编译失败，行为0 | 44原XML均在真实窗口。窗口内仅修new model的Kotlin-only createNestedClassId拼写，整体输入有漂移，不声称same-input联合验收。 |
| 614cac3e | 独立模型／enum43项42通过/1失败 | 两原XML，10036输入两端SHA `30BF519C1BB98C2E84B2072E38C2A7BEAA45A77A4A3B8A6F840FBD009E006C14`。失败为反例装配顺序未实际生成其声明的raw nested seed，未改生产。 |
| f6102f0b | 最终联合71/71，0失败/错误/跳过：root模型23、enum计划20、SOURCE相关28 | 10036输入两端SHA `2B59FDC90C619E5554DF9E5F3F1CF72EB567CB0AFD59A63EA63C286B20581109`、SDK84两端SHA `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`。十原XML均在真实窗口。 |

最终SOURCE28为两parser SOURCE14、canonical8、未导出签名负例2、冻结类型4。模型只构造真实模块/声明/PSI文件与原纯evaluator产品，不能用其TYPES模型头宣称执行了SOURCE正常生命周期。非法嵌套位置反例使用另一个平面ClassId的实际声明放进成员槽，不向仓颉引入Kotlin层次ClassId。原firstByName算法保持；specific在反例输入中先出现，确保随后两个common确实产生原断言所要求的raw边，所有原拒绝/outer proof断言保留。

## 源码gate与仍未完整交付的链路

`nominal-domain-source-gate.md` 限定接受API放置并拒绝列表充当顶层证明；`nominal-domain-root-second-gate.md` 接受文件槽/原header与冲突检查但拒绝nullable转Source；`nominal-domain-root-third-gate.md` 接受修复后的明确来源分流和记录/selected-seed有界守卫。三份历史报告保留，不改旧结论。本门不批准完整actualizer、provider/revision/epoch/cache、alias/传递bounds及type closure。

当前 `classifierRelationFor` 的生产消费尚未接入，不能把根记录和71绿记为完整classifier比较完成。继续在原MatchingContext/Resolver/Runner、根facts、extend分组/preparer、CLI/LL与后置checker接入同代输入。Root/domain是所需的结构证明之一；普通classifier仍须由其原owner域绑定，alias RHS/实参/attributes/传递bounds分别保留真实原域，完整参数与classifier投影后才使用strict equality/subtype。

原 `85a0c53e` 的10个真实前端失败继续OPEN：extend配对/未匹配ordinary overload4、普通泛型库private1、common初始化/ConstValue/tuple3、common属性缺项1、generic enum constructor1。保持原正确期望，不开全private、不伪造Frozen/value/body=0，不切测试专用协议、不按ClassId放宽equality或按名字整体隐藏overload。SOURCE/原70、P0/P10与正式LL完整生命周期也未关闭。
