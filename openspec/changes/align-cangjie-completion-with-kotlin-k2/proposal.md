## 为什么

仓颉已有 Analysis API、CFIR 语义解析和 LSP 补全实现，但这些不能等同于 Kotlin K2 的原生 IDE 补全管线。需要以本地 Kotlin 编译器和 Kotlin 插件参考源码为基线，分别补齐 Analysis 的语义契约与插件的候选生成、展示、插入、接线，并用分层测试界定完成状态。

## 变更内容

- **Analysis 模块**：对齐按位置查询的作用域上下文、作用域来源及优先级、隐式接收者、带实例化类型的成员签名、补全副本分析和生命周期；复用现有 CFIR/LL 基础设施，不在插件中重写名称解析。
- **Analysis 模块**：区分扩展成员适用性、使用点可见性、名称可达性和导入规划。现有三态候选判定不视为 Kotlin 扩展适用性检查器的完整替代；保留已有调用兼容性并补齐契约测试。
- **插件侧**：按 Kotlin K2 的入口、参数与位置上下文、贡献者、执行器、结果工厂、排序和插入处理分层实现；共享代码放在主仓库 code-insight，宿主只承担平台接线、产品装配及 IDE 集成。
- **插件侧**：覆盖基础和智能补全、链式补全、类型/成员/包/命名实参/声明生成/文档等适用场景；逐项记录 Kotlin 特有语义与仓颉语义的差异，不直接移植 K1 描述符代码。
- **交付**：补齐源码制品、聚合制品、复合构建替换、生产/测试 XML 和真实宿主验收。**IDE 与 LSP 是同一候选生成实现的两个消费端**：共用 Analysis 与补全管线，LSP 侧只做协议适配与扩展点装配，不在 `:lsp` 重新实现候选生成。原生 IDE 补全的验收仍独立于 LSP 是否启动。
- **Analysis 模块**：补齐**持久化符号索引**（按名称/前缀过滤 + analysisScope 限定的反向查询），IDE 侧跨会话持久、无平台侧会话内，并新增索引召回区段作为其消费方。**这是本次交付物，不得降级为「以后优化」。**
- **Analysis / LL**：修复 6 项已定位的框架级缺陷（块内局部声明未进解析塔、同名重载判为同一符号、dangling 副本 PREFER_SELF 返回 null、字段成员缺失、生成器未跑、文件名漂移），**一律在 LL/Analysis 层修，禁止在插件侧绕过**。
- **Analysis 模块**：新增**代码片段（表达式片段 / 类型片段）作用域**。仓颉无脚本，故不照抄 Kotlin `ScriptMemberScope`，建模为片段自身的成员作用域。
- 本变更当前只产出计划与规格，不改实现、不运行编译测试；任务的通过状态须由后续实施时的新执行结果确认。

## 功能 (Capabilities)

### 新增功能

- `analysis-completion-support`：补全所需的位置作用域、类型签名、适用性/可见性/可达性、副本分析、代码片段作用域、持久化符号索引契约、稳定结果及生命周期契约。
- `ide-completion-support`：原生 IDE 补全入口、候选管线、排序与插入、仓颉特有场景、宿主装配和分层验收契约。
- `lsp-completion-support`：LSP `textDocument/completion` 接入——扩展点装配、CompletionItem 字段映射、命令与 resolve、会话生命周期、两端呈现一致性、与 IDE 原生补全的同源性。

### 修改功能

无。本工作树尚无已有 OpenSpec 规格；本次为上述能力建立规格，不表示现有实现为空。

## 影响

- 主仓库：`analysis/analysis-api`、`analysis/analysis-api-impl-base`、`analysis/analysis-api-cfir`、`analysis/low-level-api-cfir`、`analysis/analysis-api-platform-interface`、`analysis/analysis-api-standalone`、`analysis/stubs`、`cfir/resolve` 及必要的 standalone 适配与测试；底层 CFIR 只在无法通过现有契约满足语义时定向补充。
- 主仓库：code-insight 补全的 contracts / impl-shared / impl-cfir 三模块**已存在于实施工作树**（`settings.gradle.kts` 注册与两种制品亦已就位），**主检出待补**；`settings.gradle.kts`、模块目录文档、两种 `prepare/ide-plugin-dependencies*` 制品。
- 主仓库：`:lsp` 模块——新增补全依赖、headless 扩展点 registrar、CompletionItem 转换、会话存储，并改 `CangjieServerCapabilitiesFactory`（声明 `applyEdit`）、`CangjieLanguageServerDescriptor`（`executeCommands`）、`CangjieWorkspaceService`（`executeCommand` 路由）。
- 宿主 `intellij-ide`：`modules/ide/base`、`modules/ide/lsp`、`modules/test-support`、`product/idea-plugin`、版本兼容目录、Version Catalog、复合构建和责任域 XML。
- **第三宿主 `deveco`**：`settings.gradle.kts` substitution、`product` 模块清单、扁平 `plugin.xml` 的手工注册。该产品 jar 形态下 `xi:include` 解析失败，只能手工展开；**其过期注册是静默失效**，与 IDE 侧的构建期失败性质不同。
- 兼容性：优先新增能力与兼容桥接；不在本计划中直接删除既有候选三态 API，不把文件声明作用域无条件扩展为所有可见声明，不复制 Java/JVM、Kotlin K1 或 Kotlin 专属语法。
- **范围例外**：`PluginStructureProvider.allowedExtensionPointNames` 的改动是本变更**唯一一处「为消费点改动上游 Analysis 模块」**，它同时改变 standalone 与测试容器的行为，须在责任域文档登记并单独验收。
- 边界：不承诺 Kotlin 全语言特性的逐项等价，不把已有测试报告作为当前工作树验证结果，不自动启用并行补全或运行沙箱。
