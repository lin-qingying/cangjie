# 导入 PSI 分组迁移验收台账

## 不可缩减的验收范围

`CjImportDirective` 拥有语句修饰符和条件，`CjImportGroup` 拥有组织限定、共享包前缀及花括号，`CjImportItem` 拥有实际局部路径、星号和别名。组织限定不属于包名片段；源码引用和完整语义路径分别表示，不构造合成 PSI 表达式。

普通语法以 `external/cangjie_compiler` v1.0.0 为证据；组织语法以当前官方 v1.2 beta 文档及现有组织语法测试为证据，不声称由 v1.0.0 或本机 v1.1.3 编译验证。

| 要求 | 验证入口 | 当前证据/待办 |
|---|---|---|
| 真正的 group/item 树、源码顺序、组织边界 | `ImportGroupParsingTest` | 定向通过；保留上下文关键字、尾逗号及错误恢复用例 |
| 局部路径与完整路径、连续父链 | `ImportPathTest`、`FqNamePathCompositionTest` | 已验证；修复了多段 child 错误缓存直接父级的问题 |
| group/item 真实 Stub、无 AST 读取、visitor | `ImportGroupStubTest`、`ImportPathFactoryTest` | 已验证；导入路径下的完整 DOT Stub 不得被压平 |
| 工厂、别名转义、删项分隔符、移动与失效 | `ImportPathFactoryTest`、`ImportGroupParsingTest` | 已验证的场景继续纳入最终聚合运行 |
| PSI/LightTree、源码范围、条件和可见性 | `ImportGroupRawCfirTest` | 可见性由 `CfirImport.visibility` 传递，已移除源码和 token 调试字符串反查 |
| item 唯一 CFIR 映射、group/directive 文件容器 | `AnalysisApiImportGroupReferenceTest` | 定向通过 |
| 组织隔离、命名空间重导出、包/星号 scope | `CfirOrganizationImportBindingTest`、`CfirOrganizationReexportTest` | 已验证目标仍纳入最终回归；不按最终声明组织错误筛掉跨组织重导出 |
| 包前缀实际 PSI 导航、目录聚合、组织身份 | `AnalysisApiImportPackageNavigationTest` | 已通过真实 VFS 多目录、祖先包、组织隔离及可编辑 PSI 副本重命名验证 |
| CJD 注解导入上下文 | `CjdSidecarParserTest` | 已通过 12 项 |
| CJO Stub 与渲染源码同构 | `CompiledStubShapeTest` | 已通过真实 std.core 全树比较；原差异为 import 的 DOT Stub 缺失 |
| 独立 IDE 接线与折叠 | `intellij-ide :modules:ide:base:test --tests '*CangJieFoldingBuilderTest'` | 4 项通过 |
| 产品插件真实导航 | `intellij-ide :product:idea-plugin:test --tests '*CangJieImportGroupNavigationTest'` | 1 项通过；加载真实 plugin.xml、文件类型、SDK、模块及 Analysis 服务，后台智能读动作执行解析 |
| DevEco 消费者与依赖 | `deveco :modules:ide:base:compileKotlin :product:processResources` | 通过；同步共享包 PSI 服务与旧节点名，补齐工具链 common 依赖并对齐当前语言设置构造接口；未声称 DevEco UI 自动化通过 |
| 全量 CFIR 与相关低层 API 回归 | `:cfir:analysis-tests:test` 和低层 Source/Import 测试 | 2026-09-29 最终全 CFIR 8,930 key 零失败；低层 55 项零失败 |

## 回归证据

- 首轮全 CFIR 台账：`cfir/analysis-tests/build/ffi-annotation-verification/import-group-20260928-v1`，8,930 个 XML testcase-key，10 失败，387 跳过。
- 两个问题族：PSI 分组完整路径的父链错误导致 internal 导入不再包可见；checker 用子包存在性重新判定真实导入，导致 unresolved star import 被误报 unused 并产生级联错误。
- 修复后 Linkage、UnusedImport004、DefaultParameterPkg02 的 12 个定向用例通过；v2 台账同样为 8,930 个 key，8,543 通过、387 跳过、0 失败，相对 v1 修复 10 项、无新增/删除/回归。
- 最近一次聚合运行（Gradle session 22823）成功：Analysis API 40 项、低层 API 55 项、绑定/可见性 5 项、双源导入 11 项、PSI 导入 21 项、名称模型 9 项，全部零失败。
- 后续 Analysis API 运行补入不同组织同名导入的规划测试，共 41 项零失败；最终 CFIR 台账为 `import-group-20260929-final`，与 v2 一致。CJD 12 项、std.core 二进制/source Stub 全树比较 1 项、格式化 1 项也通过。
- 产品级测试同时验证了文件类型注册：声明文件须覆盖独立 displayName；生产分析仍禁止 EDT，测试复用后台智能读动作，不放宽权限。
- 低层额外闭环：setter 无类型参数从属性签名继承类型；lambda golden 按官方目标函数类型推断记录 Int64 与已解析符号；持久上下文采集显式区分声明入口与内部边界，测试同时验证两者。
- 完整 PSI 曾有一项无导入输入的平台注解测试失败：`ForeignAndAnnotationParsingTest.platformAnnotationSurfaceIsRetainedWithoutBuiltinKindFacade`。未修改其期望或注解实现，不以导入测试绿色声称整个 PSI 门禁通过。

所有 Gradle 调用使用根仓 `gradlew-queue.bat`。独立 IDE 通过透传 `-pintellij-ide` 共用根队列。C 盘缓存写入曾因空间不足失败，后续验证使用 `--no-build-cache`；源码与测试结果未删除。

## 最终验收

上表中的导入重构要求均已完成验证，最终结构/消费者审查通过。产品烟测位于 `product/idea-plugin`，没有把未加载完整插件的模块级 light fixture 当成产品运行环境。已知无导入输入的平台注解测试失败单独记录，不宣称整仓所有功能的测试全部通过。
