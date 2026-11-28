# CJMP classifier／泛型约束接续（2026-11-24）

原70项/P0–P10、完整SOURCE与92项前端集成门保持原范围。唯一写树仍是 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`；所有历史未提交修改、原正确断言、定时任务保留。CLI程序/包装/发布后置，不后置92项；H6排除余额保留分母，v10待办不阻断授权实施。

## 实际生产修改与验收

`CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices` 已新增必填第四参 `childSubstitutor: ConeSubstitutor`，无默认值。私有 `effectiveDomain` 仍先收集真实传递bounds，再替换和求交；子侧不再在公共算法内硬编码Empty。普通override与CJMP调用均已显式迁移，测试helper也显式传生产第四参。

**当前CJMP调用仍为显式Empty，只表示调用合同迁移，不证明classifier actualizer或原owner信息已接入。** 父侧own/parent TP映射保持原行为，没有反向child→common映射、没有改变全局tag/type constructor equality，未用本步关闭原10项失败。

原 `CfirGenericConstraintCompatibilityTest` 三个方法与六个正确旧断言全部保留；新增五个方法使用真实fixture声明和typeContext，验证同一个classifier的general/fixed两端、不同声明同ClassId拒绝、完整传递outer bounds、第二个传递bound不能丢失，以及匹配一个父域仍不能收窄另一个父域。fixed比较头明确携带真实symbol；没有按ClassId重新选择另一个声明。

固定K=`3c7a5041f238102285764d370665f56f587f9dc4` 与N=`14276740ffec1262b81b6b5236b1d840af270b37` 的证据、测试事实和静态源码门在 `build/cjmp-frontend-integration-20261124/child-constraint-{kotlin-map,native-authority,test-facts,code-gate}.md`。源码门只接受API前置，不授完整classifier/92/70验收。

| 独立窗口 | 实际结果 | 证据边界 |
| --- | --- | --- |
| de9e91e0 | checkers与low-level-api-cfir生产编译成功，DONE/退出0 | 只编译，不产生行为case；编译时测试文件正在并行编写，未声称整树输入冻结。完整terminal已保存。 |
| aefdd421 | 23/23，0失败/错误/跳过：约束8、function-plan10、return-state5 | 10034输入两端SHA `E69901709DEFE6FBD09558D4A53B735DA38C211377A851F63F1077018C566F23`，SDK84两端SHA `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`；三原gzip XML和全部case均在真实窗口。 |

对应 `aefdd421-{results,input-window,terminal}.json` 与 `de9e91e0-terminal.json` 位于上述build目录。当前静态源码SHA与源码gate记录相同；未提交、清理或回退历史修改。

## 不以旧绿替代新代码

上一生产字节的完整前端 `6a4e4995` 为92项82通过/10失败，SOURCE相关 `7f9572d4` 为28/28：双parser SOURCE14、canonical8、未导出签名负例2、冻结类型4。这些真实独立窗口保留在 `build/cjmp-frontend-integration-20261123/`；不能和本23项拼成一次，不能声称新API已重新完整跑92或SOURCE矩阵。

## 接下来完整实现

继续按 `build/cjmp-frontend-integration-20261123/classifier-comparison-revised-contract.md` 和原设计gate R1–R4实施，不停在本API前置：

1. 原结构准备输入的actual session／module refinement／raw candidate order／physical owner与真实selected nominal seed的可审计归属。Accepted必须是实际结构产品的nominal边，不能将字段/extend或同名任意声明作为classifier关系。
2. nominal结构seed先行，不要求当前或互依nominal的完整bounds／comparison／family；完成的结构再供member、extend和后置比较使用。锁外准备、锁内核对原root/state并发布，cache以真实准备产品／生命周期证明同代，不能临时空组或raw equality重试。
3. 每个bound、alias RHS/实参、属性内类型occurrence保留其真实原owner域，在丢弃owner信息前完成真实classifier绑定和冻结。公共childSubstitutor本步不能恢复裸type集合已丢失的域；完整parent/child投影必须消费完成的原owner证明。
4. 原MatchingContext私有actualize覆盖双方每层classifier与原实参，保持class/struct/enum／This／RawArray／ref-enum／attributes；然后以原strict equality/subtype比较。根facts、Resolver/Runner、完整extend分组/preparer、CLI/LL及后置checker同步接入同一pair输入。
5. 用原92项验证扩展配对/ordinary overload、库成员、common初始化/const/tuple、属性缺项与generic enum constructor；原正确断言不改。private普通泛型owner与typed constant产品继续沿已考证的完整前端合同实施，不伪造Frozen、值或body=0，不切测试专用协议。

本次有真实生产API修改和编译/行为验证；完整classifier、SOURCE和原整项仍OPEN。已经停止或不存在的代理不恢复，不联系旧会话。
