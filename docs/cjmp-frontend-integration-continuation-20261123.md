# CJMP 两段式前端集成接续（2026-11-23）

92项 `compiler/frontend` 的 `CjmpTwoPhaseCompilationTest` 全部属于当前验收，入口直接调用 `CfirFrontendPipelinePhase` / `CjmpCfirFrontendPipeline`。完整CLI程序、命令行包装与发布验收后置；前端配置、版本和输出参数继续通过这些真实接口验证。原70项/P0–P10、SOURCE完整关闭条件、H6排除余额保留分母与v10待办均不改。

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`。本片只修改原字面量比较器、增加三个回归方法和更新接续证据，保留全部历史未提交修改及原92正确断言。没有删除、停用或改写定时任务；本片只读确认 `cjmp` ACTIVE、每10分钟。

## 已实施与验收

`CfirCjmpLiteralConstant.kt` 的原private helper现在区分真实语法source和 `CjBinarySourceElement`：PSI/LightTree继续使用既有literal normalizer；CJO来源使用loader已恢复的canonical value。二进制source对象、原位置、kind与参数比较规则完整保留，没有解析调试文本的第二路径。新增三个方法覆盖String/JString中的引号/反斜杠/美元符号、Rune的Unicode标量/换行/引号、Byte的原wire spelling，并检查value/source/位置对象不被替换。

固定K=`3c7a5041f238102285764d370665f56f587f9dc4`：`AnnotationDeserializationUtil.kt:114–176` 读取metadata值并以source=null构造literal；`annotationCompareUtils.kt:174` 直接比较literal.value。固定N=`14276740ffec1262b81b6b5236b1d840af270b37`：`ExprLoader.cpp:141–151` 从wire值恢复literal，`CheckCJMPAnnotations.cpp:68–86` / `Node.cpp:300–306` 比较stringValue而不是定位文本。独立[语义证据](../build/cjmp-frontend-integration-20261123/native-declaration-identity-authority.md)和[源码门](../build/cjmp-frontend-integration-20261123/annotation-binary-source-gate.md)只接受本slice，不批准完整92或70项。

所有新窗口保存到 `build/cjmp-frontend-integration-20261123/`，每个id各有results/input-window/terminal。results保存完整gzip原XML、字节长度/SHA和逐key账；不累加各窗口通过数。

| 窗口 | 实际独立结果 | 输入与证据边界 |
| --- | --- | --- |
| 65a24d29 | 完整92项81通过/11失败，0错误/跳过 | 修复前10034输入，两端SHA `6C00739A427B2D51F5CA3DB23918B3DA8DCC514B0B213C74801A759D34D88387`；原XML36442字节；有一段终端输出因预算截断，未冒称完整terminal。 |
| 9025d837 | 定向8/8：字面量7＋原annotation前端集成1 | 新输入10034，两端SHA `EE88DD6FFE94C0D521364AFED60A60CF0FF947421F26DE6FFAB41BDA14B69138`；两原XML各在真实窗口。 |
| 6a4e4995 | 联合107项97通过/10失败：完整前端92为82通过/10失败；canonical双parser8/8；PSI SOURCE7/7 | 同新输入SHA、四原XML各在真实窗口。SOURCE过滤只选中PSI类，不把这7项冒称两parser完整验收；随后另跑两parser完整矩阵。 |
| 7f9572d4 | 完整SOURCE相关矩阵28/28：两parser SOURCE14、canonical8、未导出签名负例2、冻结类型4 | 同10034输入/SDK84两端SHA，八原XML独立保存；没有重复运行前端92，不将两个窗口拼成一次结果。 |

SDK84文件三窗口前后均为SHA `527487DAD310E693DC3D0EA52FCB5E3CCCA5646F2CCCE7EF0E41BE990EA67D2B`。旧449、86、历史“SOURCE18”以及旧29/16失败只按各自真实时点保留，不能代替本片新代码验收。

## 当前10项真实失败与实施接续

- 扩展配对4项：普通Int64 overload的正常调用/冲突、matched common owner与unmatched peer两项泛型实例化。完整extend键的classifier输入尚未actualize，不能按ClassId放宽相等、跳supertype或按函数名隐藏整个overload组。
- 普通泛型库private direct member恢复1项。固定N的GetBody没有普通泛型owner一律保留private INTERNAL的规则，不能去掉serializingCommon条件或伪造Frozen。继续核真实引用/实例化收集和前端所需完整成员产品，原断言保留。
- common初始化/常量/tuple三项。新栈分别在common输出的call initializer、VarInfo.ConstValue与copied变量initializer处失败，尚未到后面的specific断言。固定N ASTWriter实际不生产值池，源码initializer选择与已完成typed constant产品需要分别贯通；不切测试专用schema，不凭isConst或字面量构造假值。
- common属性/缺项1项与generic enum constructor配对1项。原正确断言继续执行；enum结果的真实owner类型与泛型参数必须进入统一比较输入，不能删除return类型检查消错。

本轮已通过的泛型key冲突/HMPP重复库身份/Block local与初始化依赖重导出/common-only header/source FullId用例不重新记成失败，也不据此关闭完整H1/H9/SOURCE。

下一统一类型比较合同见[具体API与阶段输入](../build/cjmp-frontend-integration-20261123/classifier-comparison-contract.md)，这是待实施/待gate设计，不能当作生产交付。计划在原MatchingContext内完成双侧actualize，在根facts保存原声明域的不可变classifier证明，并同步Resolver/MatchRunner/extend分组/preparer/CLI与LL/后置checker/child与parent约束投影。Accepted结构证明与当前nominal候选自身关系分开；不读取正在发布的family、不使用可变currentCandidate、不提供默认raw comparator。完整框架与行为仍OPEN。
