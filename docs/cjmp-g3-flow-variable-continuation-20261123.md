# CJMP G2 续验与 G3 flow 变量基础（2026-11-23）

> 后续公共analyzer/default/finally/闭包/loop基础已应用，旧VariableStorage退休，实际43/43及SOURCE联合399/399；最新代码/52原XML和完整未闭tuple/value/payload/正式消费见[生命周期接续](cjmp-dfa-lifecycle-continuation-20261123.md)。下文“公共analyzer尚无真实flow”是此前时点，不覆盖新实现；完整G3与原70不关闭。

唯一实现树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。原70项/P0–P10及完整SOURCE交付继续；H6排除余额保留分母，v10待办不阻断其它授权工作。全部历史未提交修改、正确断言、旧失败及审查保留；没有提交、关闭目标或联系停止的历史会话。

## 实际窗口

| 队列 | 真实终态 | 边界 |
| --- | --- | --- |
| 904501b7 | 20项18通过/2失败，DONE1 | 正常调用参数入口与源码会话Library依赖驱动缺失。原XML文本无损归档，SHA复算。 |
| 678b2b36 | 20项19通过/1失败，DONE1 | 补全调用生命周期后，terminal selector错误地选全部同fir节点。 |
| 7cc90483 | 20/20，DONE0 | For execution14/projection6；修driver与terminal节点选择，路径、死亡、存储及身份断言不改。 |
| 275cbe70 | 349项344通过/5跳过，DONE0 | 缺显式SDK环境导致5项跳过，不算349/349。 |
| 1c59a7ec | SDK5/5，DONE0 | 显式真实SDK后的独立补验，不与上一窗口拼成一次全绿。 |
| 74ccc270 / 90910b0e | 前者semantics编译失败，后者DONE0 | Set.minus丢失PersistentSet类型，改用PersistentSet.remove；早于后续factory修订。 |
| f8bea6be | 36项34通过/2失败，DONE1 | 新ownership16中两项在单向别名builder边遍历边修改时抛CME，原For20通过。 |
| 3a25a5f9 / 2753c245 | 前者编译失败，后者DONE0 | TypeRef扩展import补齐；后者早于最终单步origin修订。 |
| 6f6a0c7f | 生产/resolve/frontend编译通过，测试编译失败，行为0 | 异类变量assertNotEquals泛型推断；仅显式共同类型，原比较对象与期望不改。 |
| 7405bc3a | 48/48，DONE0 | For20＋ownership16＋factory12，0失败/错误/跳过，4原XML无损核验。 |
| ab20f5a2 | 377/377，DONE0 | serialization349＋analysis28＝SOURCE18＋canonical8＋unexported negative2；SDK5也实跑。 |

最新377的[逐case结果](../build/cjmp-for-execution-20261122/ab20f5a2-results.json)与[输入窗口](../build/cjmp-for-execution-20261122/ab20f5a2-input-window.json)保存实际开始/结束及全部classname#testcase。10028选定输入SHA `7827910AC48ADD0F1221FAE44E31A96ECB0D9BCED25BF7ADB892D1F41CE0A039` 与SDK84 SHA `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`均前后相同。48原XML ZIP SHA为 `D7F860B30CD9C43C85853D967AABCF4AFA0884850D76A37BD2A60FBC24D931FF`，三份JSON无损保存；[逐XML证明](../build/cjmp-for-execution-20261122/g3-original-xml-proof.json)也核验f8bea6be与7405bc3a。旧XML和未执行测试不计入新窗口。

## 本批实际修订

Flow按固定K公共owner保存八表：approved statements、implications、assignment indices、direct/backwards/one-way aliases、real variables、member variables。fork/freeze携带全部表；join先合变量/receiver知识，再处理赋值和分支事实；新赋值完整迁移或清除别名、成员、陈述与蕴含。SourceVisitor访问全部表和历史。CME修订读取本次持久表再写builder，没有catch、跳过或清空关系。

DataFlowVariable.of与Flow表达式入口保持实际SessionHolder。参数、局部、模式绑定、property及实际名义qualifier使用真实symbol；alias按真实类型与session展开后仍执行同次映射。fake-origin每条original→effective map，覆盖中间wrapper及receiver；原reference/attr/type不写回。optional实际写不产生值变量，读链Option层及checked payload保持独立；非local可变字段/property不能获得稳定批准。

新增16项ownership和12项factory测试调用公开入口、真实绑定声明和生产fake-field复制器。Struct/Enum/interface typed fixture按实际Cone kind补齐；SessionHolder和assertNotEquals共同类型仅迁移driver/API，原断言对象和值不改。本批不将primitive测试称为完整BODY/replay或SOURCE roundtrip。

固定Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`、官方compiler `14276740ffec1262b81b6b5236b1d840af270b37`、runtime `4243106ba19d4885831a5868b1522bbdb757dee1`；外部只读git对象。所有Gradle经gradlew-queue.bat，PowerShell7/UTF8读/apply_patch写，没有修改Gradle配置或项目toolchain。

## 审查和下一实施

[G2限定行为gate](../build/cjmp-for-execution-20261122/g2-fixture-behavior-gate.md)接受R1/R2-S/R2-W及20项行为，完整G2端到端覆盖仍未证明。G3的[first](../build/cjmp-for-execution-20261122/g3-flow-variable-first-gate.md)/[second](../build/cjmp-for-execution-20261122/g3-flow-variable-second-gate.md)拒绝原文保留；[third](../build/cjmp-for-execution-20261122/g3-flow-variable-third-gate.md)接受修订源码，[48行为gate](../build/cjmp-for-execution-20261122/g3-flow-variable-behavior-gate.md)只接受所列primitive与driver。

[377独立复验gate](../build/cjmp-for-execution-20261122/g3-flow-variable-behavior-second-gate.md)已核验serialization349＋SOURCE18＋canonical8＋negative2，以及完整ZIP/原XML和同SHA窗口，限定ACCEPT。48窗口单独保留，不重复累加，不扩大为整个SOURCE或原方案完成。

**完整G3仍NOT READY；完整G2/SOURCE/S2/R5/S6及原70/P0–P10未关闭。** 公共CfirDataFlowAnalyzer仍缺真实incoming→transfer→freeze，Context旧VariableStorage须随公共consumer整批退休，不能保留第二套变量世界或在rebuilder末尾填空flow。按[固定K映射](../build/cjmp-for-execution-20261122/g3-kotlin-replay-map.md)与[native语义边界](../build/cjmp-for-execution-20261122/g3-native-flow-authority.md)继续file/class/function/default/block、初始化/赋值/访问、repeat/Boolean/finally/capture及enum F生命周期。补齐builder全部中间节点返回与guard前时序，再验正式sparse replay、取消/epoch与真实消费者。

正式CLI另有92项窗口的16个失败，377不覆盖该矩阵；[type projection实施映射](../build/cjmp-formal-consumer-20261122/type-projection-implementation-map.md)保留独立XML和证据等级。类型比较须由实际declaration/provider/family证明同代投影，保持严格constructor；不得按ClassId全局等同、跳supertype或按名字隐藏overload。完整SOURCE→ordinary/common/depends-on→CLI/LL及原余额继续。

本轮只读确认cjmp定时任务为ACTIVE、每10分钟，没有删除、暂停或修改它。目标仍active。
