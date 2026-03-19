# CFIR 语义分析实现状态详细报告

根据 `docs/cfir-semantic-analysis-gap.md`（2026-03-13）与当前项目代码逐项核对

日期：2026-03-14（Phase 5: Checker 类型检查 完成后更新）

---

## 一、总体结论

**骨架完备 → 肌肉 80% 就位 → 类型检查闭环。**

差距分析文档写于 2026-03-13，反映的是 Phase 1 实施前的基线。此后连续完成 5 个 Phase 的实现，语义分析能力已从"10% 表达式解析"提升到覆盖完整调用解析管线、泛型推断、模式匹配、extend 集成，**并完成 CHECKERS 阶段的 5 个核心类型检查器**。

| 维度 | 差距文档评估 | 当前实际 | 变化 |
|------|------------|---------|------|
| 编译管线框架 | 90% | 100% | +10% CHECKERS 处理器注册 |
| 类型系统（Cone） | 85% | 90% | +5% 子类型检查器完善 |
| 符号/作用域系统 | 75% | 95% | +20% 8 种 Scope 全部实现 |
| 声明级解析（Phase 1-6） | 70% | 92% | +22% extend scope 集成 |
| 表达式级解析（Phase 7-8） | **10%** | **85%** | **+75% Phase 1-4 主力突破** |
| 检查器框架 | 80% | **92%** | **+12% 5 个类型检查器 + 管线集成** |
| 诊断系统 | 95% | 95% | — |

---

## 二、实现路径回顾

差距文档推荐 4 Phase 递进路径，全部已完成：

| Phase | 主题 | 状态 | 核心交付 |
|-------|------|------|---------|
| Phase 1 | 子类型系统 + Scope | ✅ 完成 | ConeSubtypeChecker（16 规则）、8 种 Scope 实现 |
| Phase 2 | 表达式类型合成 | ✅ 完成 | CfirExpressionsResolveTransformer（字面量/变量/属性/if/块/元组/数组/插值） |
| Phase 3 | 调用解析 + 重载 | ✅ 完成 | Tower 候选收集、4 阶段验证管线、重载冲突解析器 |
| Phase 4 | 泛型约束 + 模式匹配 + extend | ✅ 完成 | 约束系统、类型参数推断、match 解析 + 穷尽性、extend provider |
| Phase 5 | **Checker 类型检查** | ✅ **完成** | **5 个核心类型检查器、3 个诊断 ID、CHECKERS 处理器注册** |

---

## 三、基础设施层（11 项全部完备）

| # | 组件 | 状态 | 关键文件 | 行数 |
|---|------|------|---------|------|
| 1 | CfirResolvePhase | ✅ | cfir-tree/src/.../CfirResolvePhase.kt | ~20 |
| 2 | CfirTotalResolveProcessor | ✅ | resolve/src/.../CfirTotalResolveProcessor.kt | ~10 |
| 3 | CfirResolveProcessor 层次 | ✅ | resolve/src/.../CfirResolveProcessor.kt | ~200 |
| 4 | CfirSession + ComponentArrayOwner | ✅ | cfir-common/src/.../CfirSession.kt | ~300 |
| 5 | CfirSymbol 体系 | ✅ | cfir-tree/src/.../CfirSymbol.kt（25 种） | ~500 gen |
| 6 | ConeCangjieType 体系 | ✅ | cfir-cones/src/.../Cone*.kt（14 种） | ~1000 |
| 7 | CfirSymbolProvider 抽象 | ✅ | symbols/src/.../CfirSymbolProvider.kt | ~200 |
| 8 | CfirScope 抽象 | ✅ | symbols/src/.../CfirScope.kt | ~100 |
| 9 | 诊断全链路 | ✅ | diagnostics/src + gen（Factory→Reporter→Collector→Renderer） | ~2100 |
| 10 | 检查器框架 | ✅ | checkers/src + gen（Declaration/Expression/Type 三类 + 5 个类型检查器） | ~1400 |
| 11 | Raw CFIR 构建 | ✅ | raw-cfir/psi2cfir/src + light-tree2cfir | ~2000 |

---

## 四、P0 核心组件实现状态

差距文档列出 5 个阻塞性缺失组件，全部已有实质性实现：

| # | 组件 | 差距文档 | 当前状态 | 完成度 | 关键证据 |
|---|------|--------|---------|-------|---------|
| 1 | **表达式类型合成器** | "完全缺失" | ✅ 已实现 | **90%** | CfirExpressionsResolveTransformer.kt（617 行），覆盖 13 种表达式：字面量、变量引用、属性访问、函数调用、if、块、return、赋值、元组、数组、插值、错误、**match** |
| 2 | **调用解析器** | "完全缺失" | ✅ 已实现 | **80%** | CfirCallResolver.kt（162 行）+ CfirTowerResolver.kt（182 行）：Tower 遍历 + 候选收集 + 4 阶段验证 + 冲突解析 |
| 3 | **重载解析器** | "完全缺失" | ✅ 已实现 | **75%** | CfirOverloadConflictResolver.kt（115 行）：FlatSignature 提取 + specificity 比较 + 非泛型优先 |
| 4 | **类型兼容性检查** | "完全缺失" | ✅ 已实现 | **95%** | ConeSubtypeChecker.kt（194 行）：16 条子类型规则完整覆盖 |
| 5 | **返回类型推算器** | "完全缺失" | ✅ 已实现 | **90%** | CfirReturnTypeCalculatorWithJump.kt（108 行）：完整实现 + 递归保护 |

---

## 五、P1 功能性组件实现状态

| # | 组件 | 差距文档 | 当前状态 | 完成度 | 关键变化（Phase 4） |
|---|------|--------|---------|-------|-------------------|
| 1 | **具体 Scope 实现** | "12 种待实现" | ✅ 已实现 | **95%** | 8 种 Scope 全部实现（Class/Package/Local/TypeParam/SimpleImport/StarImport/Extend/Composite） |
| 2 | **泛型约束系统** | "完全缺失" | ✅ **Phase 4 新增** | **80%** | CfirConstraintSystem 接口 + Impl（168 行）、CfirTypeVariable、CfirConstraint、保守策略（上下界收集 → 变量固定） |
| 3 | **泛型实例化（替换器）** | "完全缺失" | ✅ 已实现 | **85%** | CfirTypeSubstitutorByMap（97 行）：Map-based 显式替换 + Phase 4 约束系统构建替换器 |
| 4 | **跨模块符号加载** | ".cjo 未实现" | ⚠️ 部分 | **30%** | 接口 + Pipeline 注册已有，.cjo FlatBuffers 反序列化未实现 |
| 5 | **extend 成员查找** | "框架已有" | ✅ **Phase 4 完善** | **95%** | CfirSessionExtendProvider（102 行）：文件扫描 + ClassId 索引；CfirDeclarationsResolveTransformer 自动推入 extend scope |

---

## 六、P2 增强性组件实现状态

| # | 组件 | 差距文档 | 当前状态 | 完成度 | 备注 |
|---|------|--------|---------|-------|------|
| 1 | **模式匹配穷尽性** | "完全缺失" | ✅ Phase 4 新增 | **70%** | CfirMatchExhaustivenessChecker（159 行）：简化 Maranget 算法，覆盖通配符/布尔/枚举检查 |
| 2 | **模式匹配类型推断** | "完全缺失" | ✅ Phase 4 新增 | **70%** | transformMatchExpression：6 种模式解析（Wildcard/Const/Binding/Tuple/Enum/Type） |
| 3 | **类型检查器（Check 阶段）** | "框架已有，缺规则" | ✅ **Phase 5 新增** | **70%** | 5 个核心 checker：变量/属性初始化、赋值、return、参数类型检查；CHECKERS 处理器已注册 |
| 4 | 数据流分析 / Smart Cast | "可后置" | ❌ 未实现 | 0% | CfirDataFlowAnalyzerContext 接口已有，具体分析逻辑缺失 |
| 5 | Spawn 表达式 | "仓颉特有" | ❌ 未实现 | 0% | — |
| 6 | const 求值 | "可后置" | ❌ 未实现 | 0% | — |
| 7 | 操作符重载解析 | 差距文档未独立列出 | ❌ 未实现 | 0% | 需将操作符转换为函数调用 |

---

## 七、CFIR Resolve Phase 实现状态

| Phase | 中文 | 完成度 | Phase 4 变化 | 关键内容 |
|-------|------|-------|-------------|---------|
| IMPORTS | 导入绑定 | ✅ 95% | — | 导入符号解析✅、冲突检测✅ |
| SUPER_TYPES | 超类解析 | ✅ 95% | — | 类型引用解析✅、循环检测✅、诊断✅ |
| TYPES | 类型解析 | ✅ 90% | — | 显式类型引用✅、scope 栈✅ |
| STATUS | 状态解析 | ✅ 90% | — | 修饰符解析✅、可见性检查✅ |
| EXTENSIONS | 扩展解析 | ✅ 95% | extend scope 自动推入 | extend 声明✅、成员注入✅、**scope 集成✅** |
| IMPLICIT_TYPES | 隐式推断 | ✅ 90% | — | 返回类型推断✅、变量类型✅、递归保护✅ |
| **BODY_RESOLVE** | **函数体解析** | ✅ **85%** | **+15%** | 表达式合成✅、调用解析✅、重载✅、**泛型推断✅**、**match✅** |
| CHECKERS | 诊断检查 | ✅ **75%** | **+20% Phase 5 类型检查器** | 框架✅、穷尽性✅、**类型不匹配✅**、**return 类型✅**、**参数类型✅**、**赋值类型✅**、管线集成✅ |

---

## 八、Body Resolve 深度剖析

### 核心文件

| 文件 | 功能 | 完成度 | 行数 | Phase 4 变化 |
|------|------|-------|------|-------------|
| CfirExpressionsResolveTransformer.kt | 13 种表达式类型合成 | **90%** | **617** | **+130 行**（match 解析 + 模式匹配） |
| CfirAbstractBodyResolveTransformer.kt | dispatcher + 组件注入 | **85%** | **286** | **+20 行**（inferenceComponents + extendProvider + match 委托） |
| CfirDeclarationsResolveTransformer.kt | 声明处理 + scope 管理 | **90%** | **239** | **+20 行**（extend scope 自动推入） |
| CfirTowerResolver.kt | Tower 名称查询 + 候选收集 | 80% | 182 | — |
| CfirCallResolver.kt | 调用解析（Tower + 验证 + 冲突） | 80% | 162 | — |
| CfirOverloadConflictResolver.kt | specificity 重载冲突解析 | 75% | 115 | — |
| CfirReturnTypeCalculatorWithJump.kt | 返回类型推算 + 递归保护 | 90% | 108 | — |
| CfirCandidateCollector.kt | 候选收集 + 排序 | 70% | 92 | — |
| CfirImplicitAwareBodyResolveTransformer.kt | 隐式类型推断 | 90% | 93 | — |

### Phase 4 新增组件

| 文件 | 功能 | 行数 |
|------|------|------|
| CfirConstraintSystemImpl.kt | 约束收集 + 变量固定 + 替换器构建 | 168 |
| CfirInferTypeArguments.kt | 泛型类型参数推断阶段 | 160 |
| CfirSessionExtendProvider.kt | session 文件扫描 + ClassId 索引 | 102 |
| CfirConstraintSystem.kt | 约束系统接口 | 51 |
| CfirTypeVariable.kt | 类型变量模型 | 47 |
| CfirConstraint.kt | 子类型/等价约束 | 43 |
| CfirConstraintPosition.kt | 约束来源位置 | 31 |
| CfirInferenceComponents.kt | 推断组件工厂 | 22 |
| CfirMatchExhaustivenessChecker.kt | 穷尽性检查（简化 Maranget） | 159 |
| **小计** | | **783** |

### Phase 5 新增组件（Checker 类型检查）

| 文件 | 功能 | 行数 |
|------|------|------|
| CfirTypeCheckUtils.kt | 子类型检查工具（BasicConeTypeContext） | 35 |
| CfirPatternVariableInitializerTypeMismatchChecker.kt | 变量初始化类型检查 | 33 |
| CfirPropertyInitializerTypeMismatchChecker.kt | 属性初始化类型检查 | 33 |
| CfirAssignmentTypeMismatchChecker.kt | 赋值类型检查 | 31 |
| CfirReturnTypeMismatchChecker.kt | return 表达式类型检查 | 34 |
| CfirArgumentTypeMismatchChecker.kt | 函数参数类型检查 | 44 |
| CfirCheckersResolveProcessor.kt | CHECKERS 阶段处理器 + 递归 Walker | 87 |
| **小计** | | **~297** |

### 调用解析 4 阶段验证管线

```
CfirCheckVisibility → CfirMapArguments → CfirInferTypeArguments → CfirCheckArguments
        ↓                    ↓                     ↓                      ↓
  可见性检查            参数映射             泛型类型推断            参数类型检查
                                          (Phase 4 新增)
```

### 已实现能力清单

✅ 字面量类型合成（INT→IdealInt, FLOAT→IdealFloat, STRING→String, BOOLEAN→Bool, RUNE, UNIT, NULL）
✅ 变量/属性引用解析（scope 塔查找 → 符号绑定 → 类型提取）
✅ 有接收者的成员访问（接收者类型 → 成员 scope → 名称查找）
✅ 函数调用解析（Tower 候选收集 → 4 阶段验证 → 冲突解析 → 绑定最佳候选）
✅ 重载解析（FlatSignature specificity 比较、非泛型优先、参数数量匹配）
✅ 泛型类型参数推断（约束收集 → 变量固定 → 替换器构建）
✅ 显式类型参数处理（Map-based 替换器）
✅ if 表达式类型合成（then/else 分支 LUB）
✅ match 表达式类型合成（6 种模式解析 + 分支 LUB）
✅ 块表达式（最后表达式类型）
✅ return 表达式（→ Nothing）
✅ 赋值表达式（→ Unit）
✅ 元组/数组/字符串插值合成
✅ 隐式返回类型推断（递归保护）
✅ 隐式变量类型推断（从 initializer）
✅ Tower 名称查询（local → member → extend → import → package）
✅ 参数类型检查（子类型 + 替换器应用）
✅ 穷尽性检查（通配符/布尔/枚举）
✅ NON_EXHAUSTIVE_MATCH 诊断
✅ **CHECKERS 阶段管线集成（CfirCheckersResolveProcessor）**
✅ **变量初始化类型检查（CfirInitializerTypeMismatchChecker）**
✅ **属性初始化类型检查（CfirPropertyInitializerTypeMismatchChecker）**
✅ **赋值类型检查（CfirAssignmentTypeMismatchChecker）**
✅ **return 类型检查（CfirReturnTypeMismatchChecker）**
✅ **函数参数类型检查（CfirArgumentTypeMismatchChecker）**
✅ **TYPE_MISMATCH / RETURN_TYPE_MISMATCH / ARGUMENT_TYPE_MISMATCH 诊断**

### 已知缺陷 / 未实现

❌ 操作符重载解析（`+` `-` `*` 等未转换为函数调用）
❌ 隐式转换检查（数值拓宽、IdealInt → Int64 等）
❌ 数据流分析 / Smart Cast（is 检查后类型收窄）
❌ Spawn 表达式类型推断
❌ const 求值
❌ lambda 表达式（仓颉的闭包语法）
❌ 范围表达式（Range）

---

## 九、代码量统计

### 按模块

| 模块 | src 文件数 | src 行数 | gen 文件数 | gen 行数 | test 行数 |
|------|----------|---------|----------|---------|----------|
| cfir-common | 6 | 1,061 | 0 | 0 | 0 |
| cfir-cones | 19 | 1,051 | 0 | 0 | 844 |
| cfir-tree | 18 | 1,582 | 205 | 12,300 | 0 |
| symbols | 24 | 917 | 0 | 0 | 0 |
| diagnostics | 29 | 2,104 | 2 | 56 | 0 |
| checkers | 17 | 778 | 12 | 765 | 0 |
| **resolve** | **74** | **5,755** | 0 | 0 | **712** |
| raw-cfir | 6* | — | 0 | 0 | 0 |
| **合计** | **192** | **13,161** | **219** | **13,121** | **1,556** |

*raw-cfir 的 src 文件位于 psi2cfir 和 light-tree2cfir 子模块中

### resolve 模块按子目录

| 子目录 | 文件数 | 行数 | 说明 |
|--------|--------|------|------|
| body/ | 18 | 2,537 | 表达式/声明/块解析 + scope 管理 |
| calls/ | 17 | 1,017 | 候选/验证阶段/重载/Tower |
| inference/ | 6 | 362 | **Phase 4 新增**：约束系统 |
| transformers/ | 5 | ~500 | Phase 1-6 处理器 |
| providers/ | 3 | ~250 | 符号/extend 提供器 |
| diagnostics/ | 2 | ~180 | 解析规则目录 |
| services/ | 3 | ~100 | session 扩展 |
| 其他 | 19 | ~722 | 类型引用解析、导入、工具 |

### 测试覆盖

| 模块 | 测试文件数 | @Test 数 | 行数 |
|------|----------|---------|------|
| cfir-cones | 10+ | 487 | 844 |
| resolve | 5 | 28 | 712 |
| **合计** | — | **515** | **1,556** |

---

## 十、差距文档 vs 当前实际对照

| 差距文档结论 | 当时实际 | 当前实际（Phase 4 后） |
|------------|---------|---------------------|
| "表达式解析 10%" | 确实 ~10%（仅框架） | **~90%**（13 种表达式完整覆盖） |
| "调用解析完全缺失" | 确实 0% | **~80%**（Tower + 4 阶段验证 + 冲突解析） |
| "重载解析完全缺失" | 确实 0% | **~75%**（FlatSignature + specificity） |
| "类型兼容性完全缺失" | 确实 0% | **~95%**（16 条子类型规则） |
| "Scope 6 种待实现" | 确实 0% | **~95%**（8 种已实现） |
| "泛型约束系统缺失" | 确实 0% | **~80%**（约束收集 + 变量固定） |
| "模式匹配穷尽性缺失" | 确实 0% | **~70%**（6 种模式 + 简化 Maranget） |
| "Checker 框架缺规则" | ~80%（仅框架） | **~75%**（5 个类型检查器 + CHECKERS 管线集成） |
| "extend 成员查找框架已有" | ~50%（仅 scope） | **~95%**（+ provider + tower 集成） |
| ".cjo 反序列化未实现" | ~30%（仅接口） | **~30%**（未变，独立性强） |

---

## 十一、剩余缺失项优先级

### 🔴 P0 — 影响编译正确性

| # | 项目 | 预估工作量 | 影响范围 |
|---|------|----------|---------|
| 1 | **操作符重载解析** | 中（1-2 周） | 所有使用 `+` `-` `*` `/` `==` 等的代码 |
| 2 | **隐式转换 / 数值拓宽** | 中（1-2 周） | IdealInt → Int64, Int32 → Int64 等 |
| 3 | **跨模块符号加载（.cjo）** | 大（3-4 周） | 多包项目无法编译 |

### 🟠 P1 — 影响语义完整性

| # | 项目 | 预估工作量 | 备注 |
|---|------|----------|------|
| 1 | **Checker 规则补齐** | 持续 | 5 个核心类型检查器已完成，未使用变量、不可达代码等待补齐 |
| 2 | **lambda / 闭包表达式** | 中（2 周） | 仓颉闭包语法 |
| 3 | **范围表达式（Range）** | 小（1 周） | `0..10` 语法糖 |

### 🟡 P2 — 可后置

| # | 项目 | 预估工作量 |
|---|------|----------|
| 1 | 数据流分析 / Smart Cast | 大（4+ 周） |
| 2 | Spawn 表达式 | 中（2 周） |
| 3 | const 求值 | 小（1 周） |
| 4 | 约束系统 incorporation（传递性规则） | 中（2 周） |

---

## 十二、总结

```
项目状态：骨架完备 → 肌肉 80% 就位 → 类型检查闭环

  编译管线框架        ████████████████████  100%
  声明级解析(1-6)     █████████████████░░░   92%
  表达式级解析(7-8)   █████████████████░░░   85%  ← Phase 1-4 主力突破
  泛型系统            ████████████████░░░░   80%  ← Phase 4 新增
  诊断/检查器         ███████████████░░░░░   75%  ← Phase 5 类型检查器
  模式匹配            ██████████████░░░░░░   70%  ← Phase 4 新增
  跨模块(.cjo)       ██████░░░░░░░░░░░░░░   30%

  resolve 模块: 74 文件 / 5,755 行（Phase 5 新增 ~87 行）
  checkers 模块: 17 文件 / 778 行（Phase 5 新增 ~210 行）
  全 cfir 手写代码: 192 文件 / 13,161 行
  测试用例: 515 个
```

**差距已从"框架级空缺"收窄为"特性级补齐"。** 核心调用解析管线、泛型推断、模式匹配三大能力已建立，**类型不匹配诊断管线已完成闭环**（`var x: Int32 = "hello"` 等场景现在会报 TYPE_MISMATCH 错误）。下一步重点是操作符重载、.cjo 跨模块加载、更多 Checker 规则补齐。