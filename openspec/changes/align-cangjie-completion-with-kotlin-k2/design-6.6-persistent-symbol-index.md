# 6.6 持久化符号索引：实施方案

> 对位 `design.md` §4.8（位置）与 §6.9（交付物）。本文件是把 §4.8/§6.9 落到「可执行步骤」的那一层：
> 索引键、写侧、读侧、注册、版本、验证与回滚。**不改变 6.6 的验收口径**，只是把它变成能开工的方案。
>
> 撰写于 2026-10-11；所有「现状」条目都带仓内证据（文件+符号），实施时应先复核这些引用是否仍成立。

---

## 1. 目标与验收（照抄 6.6 / §6.9，不得放宽）

1. **反向查询**：`Name（前缀/子串过滤） + analysisScope` → 声明集合，返回**符号身份**（`CaSymbolIndexEntry`）而不是索引键。
2. **IDE 实现跨会话持久**，随 IDE 索引增量更新（`CaSymbolIndexPersistence.PERSISTENT`）。
3. **无平台实现**保持会话内快照语义（`SESSION_ONLY`），两者在同一查询下**返回同一符号集合**。
4. `.cjo` / SDK 的库侧名字**统一走 CFIR 只读名字视图**，**不进宿主索引**（库侧候选不依赖宿主索引，是 13.1 已锁定的行为）。
5. 记录查询次数与耗时并设预算；**禁止每次键入扫描全工作区**。

---

## 2. 现状核实（2026-10-11 实测）

### 2.1 契约（①，已落地，不需改动）

| 符号 | 位置 | 作用 |
| --- | --- | --- |
| `CaSymbolIndexQueryService` | `analysis/analysis-api-platform-interface/.../platform/index/CaSymbolIndexQuery.kt:147` | `createSymbolIndexQuery(analysisScope: GlobalSearchScope)`；`project.service()` 取用 |
| `CaSymbolIndexQuery` | 同上，`:112` 附近 | `findDeclarations(filter): Collection<CaSymbolIndexEntry>` + `indexPersistence` |
| `CaSymbolIndexEntry` | 同上，`:30` 附近 | `packageFqName` / `name` / `kind` / `ownerClassId` |
| `CaSymbolIndexEntryKind` | 同上 | `CLASS_LIKE` / `TYPE_ALIAS` / `CALLABLE` / `ENUM_CONSTRUCTOR` |
| `CaSymbolIndexNameFilter` | 同上 | `matches(name)`：前缀 ≤3 且非重复触发→`startsWith`，否则 `contains`（**分级口径已有实现，读侧必须复用它**） |

### 2.2 无平台实现（③，已落地，作为等价性基准）

`analysis/analysis-api-standalone/.../base/index/CaStandaloneSymbolIndexQueryService.kt`

- 以 `CaModificationTracker.modificationCount` 为版本键缓存 `SourceIndex`（`entriesByName` + 每条的 `files`）。
- `findDeclarations(filter)`：`prefix` 粗筛 → `filter.matches(entry.name)` 精筛 → `analysisScope.contains(file)` 限定 → 去重。
- `indexPersistence = SESSION_ONLY`，并自记 `queryCount`。

### 2.3 消费方（已落地）

- `analysis/analysis-api-cfir/.../symbols/CaCfirSymbolProvider.kt:314`：`…findDeclarations(filter)`，随后**按 kind 恢复符号**（`:372-380`：CLASS_LIKE→`getTopLevelClassLikeSymbols`、CALLABLE→`getTopLevelCallableSymbols`、ENUM_CONSTRUCTOR→枚举构造器查找）。
  ⇒ **读侧只需产出正确条目，符号恢复与可见性全在分析层**，宿主实现不碰符号。
- `code-insight/completion/impl-cfir/.../sections/CfirIndexCompletionSection.kt`：索引召回区段（已存在并按 `CaSymbolIndexNameFilter` 的重复触发语义放宽前缀）。

### 2.4 写侧接缝（**已预留，且没有任何实现**）

- `psi/src/org/cangnova/cangjie/psi/stubs/elements/StubIndexService.kt`：`open class StubIndexService`，**每个 `indexXxx` 都是空实现**；`getInstance()` = `ApplicationManager…getService(StubIndexService::class.java) ?: NO_INDEX`。
- 每个 stub 元素类型的 `indexStub(stub, sink: com.intellij.psi.stubs.IndexSink)` 都调用 `getInstance().indexXxx(stub, sink)`（如 `CjClassElementType.kt:130`）。
- 全仓（上游 `psi`/`analysis` 与宿主 `modules`/`product`）**没有任何** `StubIndexKey.createIndexKey`、`com.intellij.stubIndex` / `StubIndexExtension` 注册，也没有注册 `StubIndexService` 实现的 XML。
  ⇒ **写侧的点已接好，缺的是「谁来写、写进哪个键、谁注册」**。

### 2.5 宿主描述符现状（决定注册落点）

`intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-decompiled.xml`：

- **已经注册** `stubElementTypeHolder class="org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes" externalIdPrefix="cangjie."`
  ⇒ stub 类型的持久化注册**已就位**（索引能序列化这些类型）；
- 同一文件里已有 `applicationService` / `projectService` 的注册范式（`BuiltinsVirtualFileProvider`、`CjoModuleDataProvider`、`CaDecompiledBinaryIndex` 等）
  ⇒ **6.6② 的四处注册全部落在宿主 `modules/ide/base`，不新建 psi 描述符**（`psi/resources` 只有 messages bundle，没有也不该有 plugin XML）。

### 2.6 结论（与上一轮记录的差异）

- 之前记「仓内没有现成索引可挂，② 是从零建索引」——**更精确的说法**：索引的**写侧钩子已就位**（空实现 + 全元素类型已调用 + `NO_INDEX` 兜底），缺的是 ①索引键与扩展、②写侧实现、③读侧实现、④四处注册。工作量从「从零探索」降为「照骨架填」。
- 仍然**不含**：`ownerClassId` 的成员级索引（本轮只做顶层四类，见 §3.1 的取舍），以及任何库侧索引（明确不做，§1.4）。

---

## 3. 设计

### 3.1 索引键与粒度

**四个键，与 `CaSymbolIndexEntryKind` 一一对应**（读侧拿到命中元素就能定 kind，不必再判）：

| 键（建议名） | 写入者（`StubIndexService` 钩子） | 键值 |
| --- | --- | --- |
| `cangjie.toplevel.classlike` | `indexClass` / `indexStruct` / `indexEnum` / `indexInterface`/`indexExtend`? → 见下 | 声明的**短名** |
| `cangjie.toplevel.typealias` | `indexTypeAlias` | 短名 |
| `cangjie.toplevel.callable` | `indexFunction` / `indexMainFunction` / `indexMacroFunction` / 顶层 `let`/`var`（`indexTopLevelVariable`，若钩子缺失则复用 `indexFieldVariable` 的顶层分支） | 短名 |
| `cangjie.enumconstructor` | `indexEnumConstructor` | 构造器短名 |

取舍与理由：

- **不建成员级键**：`ownerClassId` 在条目里是可空字段；成员候选（receiver 成员、继承成员）由作用域路径出，不由索引召回（§6.9 只说「跨模块的工作区级召回」）。成员级索引是另一件事，留待有消费方时再开。
- **键值只放短名**：包名、owner、kind 都能从命中元素的 **stub/PSI** 求出（`containingCjFile.packageFqName`、`stub.parentStub`），放进键值只会让索引变大并引入失效面。
- **扩展类型**：`StubIndexService.indexExtend` 是给 extend 块用的；extend 里的声明归属要看「扩展声明本身能否成为顶层名字」的语义——**默认不写**，实施时用一个用例钉住（`extend` 内声明不进索引 ⇒ 不会凭索引召回 extend 成员，符合 §6.4 的作用域语义）。

### 3.2 写侧（宿主）

```kotlin
// modules/ide/index/.../CangJieStubIndexService.kt（模块落点见 §3.5）
class CangJieStubIndexService : StubIndexService() {
    override fun indexClass(stub: CangJieClassStub, sink: IndexSink) =
        sink.occurrence(CangJieStubIndexKeys.TOP_LEVEL_CLASS_LIKE, stub.name)
    // indexStruct / indexEnum / indexTypeAlias / indexFunction / indexMainFunction /
    // indexMacroFunction / indexEnumConstructor 同形
}
```

要点：

1. **只写顶层**：宿主索引天然只覆盖项目源码文件（库文件不在 `StubIndex` 的可索引范围），但仍要显式判「是否顶层」（`stub.parentStub` 是文件桩）——嵌套/局部声明不进索引，避免把成员当成跨包候选召回。
2. **名称判空**：`stub.name == null` 或 `Name.isSpecial` 直接跳过（与 `CaSymbolIndexNameFilter.matches` 的排除口径一致）。
3. **不吞异常**：索引建立阶段的异常必须显形（`LOG.warn` + 跳过该元素），不得让整文件失去索引而没有任何记录。
4. 钩子的签名已经是平台 `IndexSink` ✔，无需改 psi。

### 3.3 读侧（宿主）

```kotlin
class CangJieSymbolIndexQueryService(private val project: Project) : CaSymbolIndexQueryService {
    override fun createSymbolIndexQuery(analysisScope: GlobalSearchScope) = object : CaSymbolIndexQuery {
        override val indexPersistence = CaSymbolIndexPersistence.PERSISTENT
        override fun findDeclarations(filter: CaSymbolIndexNameFilter): Collection<CaSymbolIndexEntry> = ...
    }
}
```

`findDeclarations` 的步骤（每一步都对位 ③ 的语义，便于等价性断言）：

1. `DumbService.isDumb(project)` → **返回空集合**并记一次「不可用」观测。索引在 dumb 期间不可读；**禁止**任何扫描兜底（13.1）。
2. 对每个键：取候选键名集合（`StubIndex.getInstance().getAllKeys(key, project)` 的缓存视图，按 `IndexModificationTracker`/索引版本失效），用 `prefix` 粗筛（≤3 字符口径同 `CaSymbolIndexNameFilter`）。
3. 对每个候选键名：`getElements(key, name, project, analysisScope)` → 逐个取 `CjDeclaration`。
4. 组成 `CaSymbolIndexEntry`：`packageFqName = containingCjFile.packageFqName`；`name`；`kind = 键对应的 kind`；`ownerClassId = null`（顶层）。
5. 精筛与去重：`filter.matches(name)`；同一 `(package, name, kind)` 只留一条（多个同名声明的**符号合并**由分析层的 `getTopLevelClassLikeSymbols` 负责，索引层不合并、也不丢）。
6. 统计：`queryCount` / `queryNanos` / `keysScanned` 写进 `CompletionStatisticsSink`（13.2 的 INDEX_QUERY 阶段已有埋点，读侧补「键扫描量」这一项，用于断言「不是全工作区扫描」）。

线程与生命周期：`findDeclarations` 在分析会话的 read action 内被调用（`CaCfirSymbolProvider`）；实现里不得持有 PSI 越出调用范围——条目是纯数据 ✔。

### 3.4 注册与版本

| 注册 | EP | 位置 | 说明 |
| --- | --- | --- | --- |
| 4 个 `StubIndexKey` + 扩展 | `com.intellij.stubIndex` | **宿主** `modules/ide/base/src/main/resources/META-INF/cangjie-decompiled.xml`（已含 `stubElementTypeHolder` 与同类服务注册） | `StringStubIndexExtension` 子类；**version 常量集中在 `CangJieStubIndexKeys.VERSION`** |
| 写侧 | `com.intellij.applicationService` | 同上 | `StubIndexService` 实现（`serviceInterface` 指向前端开放类，`serviceImplementation` 为宿主实现） |
| 读侧 | `com.intellij.projectService` | 同上 | `CaSymbolIndexQueryService` 实现 |

- **版本升级 = 索引全量重建**：任何影响「键 → 元素」映射的改动（钩子语义、短名口径）都必须 `VERSION + 1`，否则旧索引会与本实现期望不符；这条要写进模块 README，避免下一个人「改了忘了升」。
- 不用自定义 EP 暴露实现（§5 D3 已否决）：只走平台的 `applicationService`/`projectService` ✔。

### 3.5 模块落点（决策）

| 件 | 落点 | 理由 |
| --- | --- | --- |
| 索引键 + `StubIndexExtension` 子类 | **宿主 `modules/ide/base`**（`.../ide/base/index/`） | 键的读写双方都在宿主（写侧 `StubIndexService` 实现、读侧 `CaSymbolIndexQueryService` 实现）；`psi` 侧只有钩子，不需要也不该新增描述符（§2.5） |
| 写侧 `CangJieStubIndexService` | **宿主**同模块 | §4.8「IDE 实现宿主侧」；它把 `psi` 的钩子接到平台 `IndexSink` |
| 读侧 `CangJieSymbolIndexQueryService` | **宿主**同模块 | 需要 `GlobalSearchScope`/`DumbService`/项目索引，且 §4.8 指定宿主 |
| 资源 XML 注册 | `modules/ide/base/.../META-INF/cangjie-decompiled.xml`（或同目录新 XML 并被主描述符 include） | 该文件已注册 `stubElementTypeHolder` 与同类服务，是宿主的装配点 |

> 备选：读侧放 `analysis-api-cfir`（前端内），宿主只注册写侧。**不选**的理由：读侧要按宿主索引可用性（dumb）与 scope 语义走平台 API，属「宿主装配面」；放前端会让无平台宿主误以为存在持久实现。

---

## 4. 落地步骤（每步都有可执行验证）

| 步 | 内容 | 验证 |
| --- | --- | --- |
| S1 | 宿主 `modules/ide/base`：4 个键 + `VERSION` + 4 个 `StringStubIndexExtension` + 在 `cangjie-decompiled.xml` 注册 `com.intellij.stubIndex` | 宿主夹具里 `StubIndex.getInstance().getAllKeys(...)` 对含顶层声明的文件返回短名（夹具会建索引） |
| S2 | 宿主：`CangJieStubIndexService`（继承前端开放类）实现全部相关钩子 + `applicationService` 注册 | 单元：给一个带注释/嵌套声明的文件，断言只有**顶层**名字进索引；`extend` 内声明不入（钉语义） |
| S3 | 宿主：`CangJieSymbolIndexQueryService` + **等价性复用 `CaSymbolIndexNameFilter`** + dumb 空集合 + 统计 | 单测：前缀/子串/重复触发的三档口径与 ③ 相同 |
| S4 | 宿主：`projectService` 注册 + 端到端 | 宿主集成用例（`CangJieLightCodeInsightFixtureTestCase`）：**未打开文件**里声明的短名可被前缀查询召回 |
| S5 | **等价性**：同一 filter + 同一 scope，宿主实现与 `CaStandaloneSymbolIndexQueryService` 返回同一 `CaSymbolIndexEntry` 集合（排序无关） | 一个用例两套实现并跑（无平台侧在 standalone 夹具里已有） |
| S6 | 增量性与新鲜度：改文件增删声明后再次查询立刻可见（索引增量更新）；重启（新 project）后仍可见（**跨会话持久**） | 用例：编辑后查询；持久性用「索引在两次会话间保留」的断言（或在测试里显式重开项目） |
| S7 | 观测与预算：`keysScanned`/`queryNanos` 进统计；在 `completion-stage-budget.properties` 的 `INDEX_QUERY` 预算下加一条「大投影下键扫描量上限」的守卫 | 13.2 的 `LspCompletionStageBaselineTest` 扩一条断言（无平台侧）；宿主侧单测断言「查询不触发全文件遍历」（扫描量为 0 的文件数） |
| S8 | 文档与治理：模块 README 写「索引键语义 + 版本规则」；`tasks.md` 6.6 行更新为「② 完成，见本文件 S1–S7」 | 文档评审 |

---

## 5. 验证清单（直接对 6.6 的验收句）

- 源码 / `.cjo` / SDK：源码走本索引；`.cjo`/SDK **不进索引**、仍由 CFIR 名字视图提供（用例断言：查询结果只含源码包）。
- 未打开文件：不在编辑器中打开的文件里的顶层声明可被召回 ✔（S4）。
- 未导入类型：召回后由分析层给 `AddImport` 计划（`CfirIndexCompletionSection` 已有）。
- 同名函数与 enum 构造器：同名同 kind 的多条声明**不得漏报**（索引层不去重，交给分析层合并）。
- **不漏报**：与 ③ 的等价性断言（S5）就是这条的机械形式。
- 查询次数与耗时预算、禁止全工作区扫描：S7。
- 语义等价性：S5。

---

## 6. 风险与回滚

| 风险 | 影响 | 处置 |
| --- | --- | --- |
| 索引版本忘了升 | 旧索引与新语义不符，表现为「候选少/错」且不报错 | `VERSION` 集中一处 + 模块 README 规则 + S1 用例断言版本常量被引用 |
| dumb 期间空结果 | 索引刚失效后短暂无索引候选 | 合规（13.1 要求「少给」而不是「扫描兜底」）；补一条观测，便于诊断「为什么没候选」 |
| 索引体积/内存 | 大项目键集合膨胀 | 键值只有短名、只收顶层四类；S7 的大投影键扫描守卫给出上界 |
| extend/嵌套语义写错 | 召回不该出现的名字 | S2 用两个钉死用例（extend 内、嵌套类内声明不入索引） |
| 回滚 | —— | 移除写侧/读侧注册 ⇒ `getInstance()` 回到 `NO_INDEX`、`CaSymbolIndexQueryService` 落回 ③（无平台实现），行为回到今天；索引扩展留在 `psi` 不影响运行（无人读） |

---

## 7. 工作量切分

- **变更 A（本方案 S1–S4）**：键 + 扩展 + 写侧 + 读侧 + 注册 + 端到端召回用例。
- **变更 A 内续（S5–S7）**：等价性、增量/持久、观测与预算。
- **不在本方案**：成员级索引、`ownerClassId` 的索引级填充、库侧索引（明确不做）。

> 与 6.6 的关系：本文件是 6.6② 的实施方案；6.6 的勾选条件仍是「② 实现 + ③ 等价性断言 + 预算」全部落地（S1–S7）。
