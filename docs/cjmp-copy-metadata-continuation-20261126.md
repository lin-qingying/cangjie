# CJMP owned-copy 元数据接续（2026-11-26）

## 本轮核对与实际修订

> 最新d窗口已终态：498实际/481通过/17失败（完整serialization434/417/17、copy/Published46/46、两parser SOURCE18/18含原SOURCE14）；51原XML逐份gzip解压/字节数/SHA核验成功，两原终端流封存，9845输入与当前std目录84 SDK文件前后同SHA。原queue `8c0040ee` DONE/exit1，包装器日志读共享问题及显式恢复收集单独记载。无活动本轮Gradle；下一直接完整B1/B2正式消费迁移，不继续把基础切片绿当整项交付。

唯一实现树仍为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，所有历史未提交修改保留。原70项/P0–P10、SOURCE S1–S7及H6排除分母不变，v10待办不阻断授权实施。

已先读取原交接、原70项计划、滚动审计、SOURCE完整清单、S2原REJECT和最新owner/R26接续。磁盘另有R27 Published候选及 `r27-published-20261126-a-results.json`：471实际/427通过/44失败、输入同SHA。不得把原交接的生成器失败和SOURCE2/14当作当前状态，也不得忽略R27历史候选。R27后来生产编译5ae6eefa已成功，原44失败仍独立保留。

本轮按 `r27-published-annotation-final-contract-gatekeeper.md` C1/C2修订四个生产文件：

- DeclarationAnnotationInfo原owner的raw、derived、registered键开放internal；原OwnedCopy收集/复制明确处理这些键及Unselected marker。raw真实null的登记资格、marker跨Published→Owned→Owned保留，未知外部键仍严格拒绝。
- annotation和interop分别由原字段owner提供值冻结，Complete与OwnedCopy共用。Java/ObjC的非空generatedDeclarationIds及全部现有集合防御复制并冻结；保留全部标量、顺序、null和空分支。完成元数据不共享输入可变集合。
- Published复制尾部仍由同memo快照取完成产品；raw与derived分开存储，不读Library正文、不修改原声明。

固定K `3c7a5041f238102285764d370665f56f587f9dc4` 的FirDeclarationAttributes/IrAttribute/IrDeepCopyBase与固定N `14276740ffec1262b81b6b5236b1d840af270b37` 的CloneDecl/CloneAnnotation用于owner及复制职责考证；本地Published/NULL登记/marker/深冻结没有上游同名机制，不冒称逐行复制。独立有界复审为 `build/cjmp-invisible-peer-20261125/r28-copy-metadata-gatekeeper.md`，仅C1/C2源码ACCEPT，不授完整B1/B2/S2/SOURCE或行为通过。

新增五个Published产品合同回归，保留全部原测试及严格attributeRemapper，覆盖二次复制、NULL登记、H/M独立集合、非空Java/ObjC ID和返回集合不可写。

测试侧三个文件补原Source正规SUPER_TYPES/RHS→STATUS生命周期前置并核验原symbol/source/目标，复用现有non-local正式入口，不直接发布假产品；保留原33测试方法和正确期待。该修订的行为仍待本轮冻结窗口确认。

## 编译和行为窗口

### 已封存终态与静态环修订

`r28-copy-metadata-source-20261126-a`实际exit1：已执行64/64通过（tree复制28、Published18、两parser SOURCE18含原SOURCE14），0失败/错误/跳过；serialization因新fixture编译问题未执行，不据旧XML计通过。六份gzip原XML逐个解压核验原字节数/SHA成功。9845输入和SDK46事前/事后同SHA。fixture的toSymbol导入及继承基类assertNotNull遮蔽已修正。

`r28-serialization-20261126-b`原native queue `adb0da38`实际DONE/exit1：434实际/361通过/73失败，0错误/跳过；9845输入和SDK46前后同SHA。native于原queue.finishedAt退出，原日志有其完成记录，但交互证据包装器未返回，已仅中断本轮包装器。通过同runner的显式CompletedQueueId模式核对原参数/终端完成、真实startedAt/finishedAt、未被后续替换的原XML，再封存完整结果；JSON明确记录wrapperInterruptedAfterNativeCompletion=true，不冒称包装器正常退出或重新执行行为。

b的73失败中61项原failure含ReturnTypeCalculator静态初始化异常；其余12项独立保留。根因是父类静态Default访问仍未初始化的子类companion。固定K两文件明确父抽象类没有Default；本轮删除父别名，并把全部9文件10caller迁移到原子类Default，保留子class/companion及原计算语义，未新增getter/lazy/null路径。独立有界PASS见 `build/cjmp-invisible-peer-20261125/r28-return-calculator-initialization-gatekeeper.md`，不授尚未迁移的Default/Status/Contract诊断或consumer全对齐。

新窗口 `r28-return-calculator-serialization-20261126-c`已冻结并开始完整serialization及resolve.compileTestKotlin；终态待核。新runner用隐藏原生进程分别重定向stdout/stderr并保存原流，防止已退出进程的交互管道阻塞证据完成；不伪造两流原始合并顺序。该修改仅属build证据脚本。

### c/d终态和剩余17实际用例

c原queue `7596f7ad` DONE/exit1，完整serialization434/401/33，resolve测试源码编译成功；45原XML、9845输入与SDK46同SHA。静态初始化异常消失；12个新增Iterator角色断言因实际SDK Iterator是abstract class而失败，9个用例进一步进入真实IMPORTS前置，另12个原消费者/边界失败保留。该包装器Start-Process -Wait连同长期daemon等待，native退出后仅中断本轮包装器；第一次恢复收集仅检查stdout而误拒实际stderr完成行，原JSON保留该failure及两原流、原XML和同SHA证据，不重写旧结果。原stderr末行与queue.finishedAt独立确认真实exit1；后续收集明确查两流。

SDK `std.core.cj.d:4926` 明确为 `public abstract class Iterator<T> <: Iterable<T>`，观察SHA `E02D68A6122CA0062F71C163B15FF3EFEE294AFE994B39925E78CAE1518B0EC2`；当前CJO也绑定实际class。已恢复原class类型事实，纠正本轮新错误interface断言，不改变正确旧语义目标、不为Cursor捏造Object直接边。

Source fixture现通过正常 `CfirImportResolveTransformer.recordImportBindingsForFile` 完成原文件imports并检查真实诊断；两个Unified测试Module在Source装配中明确注册正常Store/DefaultImportsHolder，没有手填空bindings、降低phase或missing-component兜底。独立有界复审核对该前置和Iterator事实通过；四文件均历史未跟踪，reviewer不能据Git比较认证全部历史断言，此限制保留。当前修订不改原SOURCE18测试源码或期待。

d为 `r28-imports-iterator-source-20261126-d`，原native于 `2026-11-26T03:06:20.895Z` 结束：**498/481/17，0错误/跳过**。两parser SOURCE18含原14、copy28、Published18均通过；完整serialization434中417通过17失败。51原XML逐份解压核SHA及原字节数，重新总计仍498/17/0/0，两个原终端流另存原字节。

d输入摘要两端 `cbcd542c5891b693b268b31e23e9d771ca474aeaff76d21e7e5bde291d72a68e`，9845文件；SDK两端 `7bbbba79757f4588ebe280b79b57c86d50740264aa090de853311dd0b07c2f6b`，本轮84文件包括真实`.cj.d`侧车，不能把此前仅46文件的选择域冒称覆盖它们。d包装器native结束后读取仍持有write句柄的原日志失败；保留原失败，改共享只读原流捕获，再按同queue参数/startedAt/finishedAt/未替换XML显式恢复封存，JSON failure=null、wrapperInterruptedAfterNativeCompletion=true，不冒称原包装器成功。

剩余17按实际case继续：

- 初始化六项：五项正式BODY driver没有调用阶段进入，另一个原external-type拒绝期待尚待核真实调用阶段；不能把提前错误当原目标拒绝通过。
- 主构造器字段一项：写出真实Object父类后，测试消费context尚未取得实际外包classifier；保留严格materializeClassLikeSymbol守卫，不能删Object或补同名symbol。
- 四项旧eager header读取／owner复用，四项正常default/annotation声明完成生命周期：随完整B1/B2消费者、缓存前授权、精确槽和Published发布修复。
- missing-target期待一项、VArray unused raw-save一项：依原absence/raw槽authority继续，不绑定ignored target或修改正确期待换绿。

所有旧a/b/c失败和原R27/R26记录独立保存，不重叠计分。原70/P0–P10、S1–S7、Owner6/alias/诊断余额及H6/v10边界保持OPEN；无活动本轮Gradle。

本轮通过PowerShell7及gradlew-queue运行tree/serialization生产compile，实际exit0，BUILD SUCCESSFUL in4m35s，83任务4executed/79up-to-date。该compile无独立全输入manifest，不能替代行为验证。

a的runner为 `build/cjmp-invisible-peer-20261125/run-r26-request-validation.ps1 -AllSerialization -PublishedTree -SourceChain`。选择域9845输入摘要 `d55f6723b254ba731b7c5b374b63524ee6c622013ad3109af84c79f41dcdf77b`，SDK当前46文件另采样，不冒称历史SDK84；b在fixture编译修订后重新冻结，摘要 `d63d9018d5d3ec93c42ba536377dc73bf0bd2b36907d62fe78d161f8494c1723`。每轮独立计数，不合并重复用例。

## 下一正式消费迁移

R26 header registry和三域request仍未完整接入normal binder/provider/loader。下一沿完整mapper实现metadata typed需求和缓存前授权、TP inverse索引、boundContexts及真实F.body、d-content、完成目录和统一alias根发布、default/annotation Published按需能力、全部真实caller。不得以C1/C2源码ACCEPT或局部tree绿关闭B1/B2、Owner6、原alias/诊断失败或原70项。

原R27多项新失败实际进入CjdSidecarParser时ApplicationManager.application为null；解析器明确要求宿主先初始化application/parser。当前磁盘Position/Factory已具备组合fixture及lazy session，Initialization/PrimaryConstructor/UnifiedExport/Review已有正常parser fixture；这些是后来历史字节，不能据旧R27异常重复添加fixture。BodyIntegration/DeclarationCompletion/OwnedClosure没有显式fixture，但不能单凭此认定当前必然NPE。b未再报旧application缺失原因，新的61项static环与12其它失败按原XML继续修，不增加生产Application兜底或跳过sidecar。

完整下一生产迁移合同为 `build/cjmp-invisible-peer-20261125/r28-normal-consumer-implementation-mapper.md`，含registry/type-cache前授权、TP逆索引、boundContexts、精确保存槽、完成目录及aliases同根发布；必须整体接通normal consumer，不以本轮复制46或SOURCE18通过关闭原项。
