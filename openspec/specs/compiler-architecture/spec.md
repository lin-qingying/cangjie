# 仓颉编译器架构全景分析

## 文档元信息

- **版本**: 1.0
- **创建日期**: 2026-03-09
- **状态**: 深度探索中
- **维护者**: Cangjie Compiler Team

---

## 1. 执行摘要

### 1.1 项目定位

仓颉编译器是一个基于 Kotlin/JVM 实现的现代编译器，架构对齐 Kotlin K2，功能对齐官方仓颉 C++ 编译器。项目采用 12 阶段编译流水线，当前处于 Raw CFIR 实现阶段。

### 1.2 核心设计决策

1. **架构对齐**: 完全对齐 Kotlin K2 的 FIR 架构
2. **双 IR 设计**: CFIR（前端 IR）+ CHIR（后端 IR）
3. **惰性解析**: 支持按需分阶段解析，适配 IDE 场景
4. **测试驱动**: Golden File 对比机制确保正确性
5. **零依赖核心**: 测试基础设施零 IntelliJ 依赖

### 1.3 项目规模

| 指标 | 数量 |
|------|------|
| 模块数 | 21 个 |
| CFIR 源文件 | 99 个 |
| PSI 源文件 | 418 个 |
| Analysis 源文件 | 45 个 |
| 测试基础设施文件 | 20 个 |
| 官方 C++ 源文件 | 552 个 |

---

## 2. 编译流水线设计

### 2.1 12 阶段编译流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 仓颉编译器 12 阶段流水线                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ [1] LOAD_PLUGINS ──→ [2] PARSE ──→ [3] CONDITION_COMPILE                   │
│ │                    │                  │                                    │
│ │                    ▼                  ▼                                    │
│ │              插件注册表            PSI Tree        裁剪后 PSI Tree         │
│ │                                                                             │
│ ▼                                                                             │
│ [4] IMPORT_PACKAGE ──→ [5] MACRO_EXPAND ──→ [6] CFIR_BUILD                  │
│ │                        │                        │                          │
│ │                        ▼                        ▼                          │
│ │                  合并包列表              展开后 AST        Raw CFIR         │
│ │                                                                             │
│ ▼                                                                             │
│ [7] CFIR_RESOLVE ──→ [8] FINALIZE ──→ [9] MANGLING                          │
│ │                      │                    │                                 │
│ │                      ▼                    ▼                                 │
│ │              完整语义 CFIR          单态化 CFIR      符号修饰名映射          │
│ │                                                                             │
│ ▼                                                                             │
│ [10] SAVE_CJO ──→ [11] CFIR2CHIR ──→ [12] CODEGEN                           │
│ │                    │                        │                               │
│ │                    ▼                        ▼                               │
│ │              .cjo 文件            优化后 CHIR      .bc / .o                 │
│ │                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 阶段详细说明

| # | 阶段 | 中文名 | 输入 | 输出 | 状态 |
|---|------|--------|------|------|------|
| 1 | LOAD_PLUGINS | 插件加载 | 插件路径配置 | 插件注册表 | 🔜 计划中 |
| 2 | PARSE | 源码解析 | .cj 源文件 | PSI Tree | ✅ 已实现 |
| 3 | CONDITION_COMPILE | 条件编译 | PSI Tree | 裁剪后 PSI Tree | 🔜 计划中 |
| 4 | IMPORT_PACKAGE | 包导入 | 裁剪后 PSI | 合并包列表 | 🔜 计划中 |
| 5 | MACRO_EXPAND | 宏展开 | 合并包列表 | 展开后 AST | 🔜 计划中 |
| 6 | CFIR_BUILD | CFIR 构建 | 展开后 AST | Raw CFIR | ✅ 核心完成 |
| 7 | CFIR_RESOLVE | CFIR 解析 | Raw CFIR | 完整语义 CFIR | 🔜 计划中 |
| 8 | FINALIZE | 语义后处理 | 完整 CFIR | 单态化 CFIR | 🔜 计划中 |
| 9 | MANGLING | 名称修饰 | 单态化 CFIR | 符号修饰名映射 | 🔜 计划中 |
| 10 | SAVE_CJO | CJO 保存 | 修饰名就绪 CFIR | .cjo 文件 | 🔜 计划中 |
| 11 | CFIR2CHIR | CHIR 生成 | 完整 CFIR | 优化后 CHIR | 🔜 计划中 |
| 12 | CODEGEN | 代码生成 | 优化后 CHIR | .bc / .o | 🔜 计划中 |

### 2.3 CFIR_RESOLVE 内部 Phase

```kotlin
enum class CfirResolvePhase {
    RAW_CFIR,        // PSI/LightTree → CFIR 转换完成
    IMPORTS,         // 解析 import 语句
    SUPER_TYPES,     // 解析父类型
    TYPES,           // 解析显式类型
    STATUS,          // 解析声明状态（修饰符）
    EXTENSIONS,      // 解析 extend 声明（仓颉特有）
    IMPLICIT_TYPES,  // 推断隐式类型
    BODY_RESOLVE,    // 解析函数体和表达式
    CHECKERS;        // 运行诊断检查器
}
```

**关键特性**：
- 惰性分阶段解析：每个声明独立跟踪已完成的 Phase
- 同一文件中不同声明可处于不同 Phase
- 按需推进（支持 IDE 场景）
- CHECKERS 是最后一个 Phase，诊断错误在此终止

---

## 3. 核心数据结构

### 3.1 PSI（语法树）

**文件数量**: 418 个 Kotlin 文件

```
PSI 层架构
├── CjFile（文件根节点）
├── 声明节点
│   ├── CjClass / CjInterface / CjStruct / CjEnum
│   ├── CjExtend（仓颉特有）
│   ├── CjNamedFunction
│   ├── CjProperty
│   ├── CjConstructor（Primary/Secondary）
│   └── CjTypeAlias
├── 表达式节点
│   ├── CjExpression（基类）
│   ├── CjBlockExpression
│   ├── CjConstantExpression
│   ├── CjCallExpression
│   ├── CjIfExpression / CjMatchExpression（仓颉特有）
│   ├── CjForExpression / CjWhileExpression
│   └── ...
└── 类型引用
    ├── CjTypeReference
    ├── CjUserType
    ├── CjBasicType（Int64, Bool 等）
    ├── CjFunctionType
    └── CjTupleType
```

**核心设计**：
- 基于 IntelliJ PSI 框架
- 支持 Stub 索引（快速查找）
- 双向映射：PSI ↔ AST
- 完整覆盖仓颉语言语法

### 3.2 CFIR（前端 IR）

**文件数量**: 99 个 Kotlin 文件

```
CFIR 层架构
├── CfirElement（根接口）
│   ├── source: CfirSourceElement?
│   └── accept(visitor: CfirVisitor)
│
├── CfirDeclaration（密封接口）
│   ├── CfirFile
│   ├── CfirMemberDeclaration
│   │   ├── CfirClassLikeDeclaration
│   │   │   ├── CfirClass（class/interface/struct/enum）
│   │   │   ├── CfirExtend（仓颉特有）
│   │   │   └── CfirTypeAlias
│   │   └── CfirCallableDeclaration
│   │       ├── CfirFunction
│   │       ├── CfirProperty
│   │       ├── CfirConstructor
│   │       └── CfirVariable
│   ├── CfirValueParameter
│   ├── CfirTypeParameter
│   └── CfirEnumEntry
│
├── CfirExpression（抽象类）
│   ├── CfirBlock
│   ├── CfirLiteralExpression
│   ├── CfirFunctionCall
│   ├── CfirIfExpression
│   ├── CfirMatchExpression（仓颉特有）
│   ├── CfirForInExpression
│   ├── CfirLambdaExpression
│   ├── CfirSpawnExpression（仓颉特有）
│   └── ...
│
├── CfirTypeRef（密封接口）
│   ├── CfirUserTypeRef（未解析）
│   ├── CfirResolvedTypeRef（含 ConeCangjieType）
│   ├── CfirImplicitTypeRef
│   ├── CfirFunctionTypeRef
│   └── CfirErrorTypeRef
│
└── CfirReference（密封接口）
    ├── CfirNamedReference（未绑定）
    ├── CfirResolvedNamedReference（→ CfirSymbol）
    └── CfirErrorReference
```

**核心特性**：
- 密封类层次（sealed class/express）：便于穷举处理
- 双层类型系统：CfirTypeRef（语法层） + ConeCangjieType（语义层）
- 与语义信息分离：类型信息存储在独立的 CfirTypeTable
- 完整携带 SourceRange（源码位置）

### 3.3 Cone 类型系统

```
ConeCangjieType（密封类）
├── ConeRigidType（密封）
│   ├── ConePrimitiveType
│   │   ├── Unit / Bool / Rune / Nothing
│   │   ├── Int8/16/32/64/Native, UInt8/16/32/64/Native
│   │   ├── IdealInt（仓颉特有）
│   │   ├── Float16/32/64
│   │   └── IdealFloat（仓颉特有）
│   ├── ConeClassLikeType（class/interface）
│   ├── ConeStructType（struct 值类型，仓颉特有）
│   ├── ConeEnumType（enum ADT，仓颉特有）
│   ├── ConeFuncType（(P1,P2)->R）
│   ├── ConeTupleType（(T1,T2,...)）
│   ├── ConeArrayType（RawArray<T>）
│   ├── ConeVArrayType（VArray<T,N>，仓颉特有）
│   ├── ConeTypeParameterType（泛型参数）
│   └── ConeErrorType
├── ConeIntersectionType
├── ConeUnionType
└── ConeFlexibleType（C 互操作）
```

**仓颉特有类型**：
- `IdealInt` / `IdealFloat`：无限精度数值
- `Struct`：值类型（Kotlin 全是引用类型）
- `VArray<T,N>`：定长数组
- `extend Type <: Interface`：类型扩展

---

## 4. 模块组织

### 4.1 模块依赖图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 模块依赖关系                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    ┌─────────────┐                                          │
│                    │ common      │（基础设施：Name, FqName, Visibility）      │
│                    └──────┬──────┘                                          │
│                           │                                                 │
│                    ┌──────▼──────┐                                          │
│                    │ util        │（通用工具）                                │
│                    └──────┬──────┘                                          │
│                           │                                                 │
│          ┌────────────────┼────────────────┐                                │
│          │                │                │                                │
│   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐                         │
│   │ psi         │  │ cfir-common │  │ dependencies│                         │
│   │             │  │             │  │ :intellij-  │                         │
│   │ (418 files) │  │ (Session,   │  │    core     │                         │
│   │             │  │  ModuleData)│  │             │                         │
│   └──────┬──────┘  └──────┬──────┘  └─────────────┘                         │
│          │                │                                                 │
│          │         ┌──────▼──────┐                                          │
│          │         │ cfir-cones  │                                          │
│          │         │ (Cone Type) │                                          │
│          │         └──────┬──────┘                                          │
│          │                │                                                 │
│          │         ┌──────▼──────┐                                          │
│          │         │ cfir-tree   │                                          │
│          │         │ (IR Tree,   │                                          │
│          │         │  Visitor)   │                                          │
│          │         └──────┬──────┘                                          │
│          │                │                                                 │
│          └────────────────┼────────────────┐                                │
│                           │                │                                │
│                    ┌──────▼──────┐  ┌──────▼──────┐                         │
│                    │ raw-cfir    │  │ analysis    │                         │
│                    │ :psi2cfir   │  │ :analysis-  │                         │
│                    │             │  │    api-cfir │                         │
│                    └──────┬──────┘  └──────┬──────┘                         │
│                           │                │                                 │
│                           └────────────────┼────────────────┐                │
│                                            │                │                │
│                                     ┌──────▼──────┐  ┌──────▼──────┐         │
│                                     │ tests:test- │  │ compiler    │         │
│                                     │ infrastructure│ │ :cli       │         │
│                                     └─────────────┘  └─────────────┘         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 模块职责矩阵

| 模块 | 职责 | 文件数 | 状态 | 对应阶段 |
|------|------|--------|------|---------|
| `common` | 基础类型（Name, FqName, Visibility） | - | ✅ | - |
| `util` | 通用工具类 | - | ✅ | - |
| `psi` | PSI 定义（仓颉语言语法） | 418 | ✅ | 阶段 2 |
| `cfir:cfir-common` | Session、ModuleData、SourceElement | - | ✅ | - |
| `cfir:cfir-cones` | Cone 类型系统 | - | ✅ | - |
| `cfir:cfir-tree` | IR 树、Visitor、Scope、Symbol | 99 | ✅ | - |
| `cfir:raw-cfir:psi2cfir` | PSI → Raw CFIR | - | ✅ | 阶段 6 |
| `cfir:raw-cfir:raw-cfir-common` | 抽象构建器基类 | - | ✅ | - |
| `analysis:analysis-api` | Analysis API 接口层 | 45 | ✅ | - |
| `analysis:analysis-api-cfir` | CFIR → Analysis API 桥接 | - | ✅ | - |
| `tests:test-infrastructure` | 测试基础设施 | 20 | ✅ | - |
| `compiler:cli` | CLI 入口 | - | 🔜 | 阶段 1-12 |
| `cfir:cfir-build` | PSI → Raw CFIR（含增量） | - | 🔜 | 阶段 6 |
| `cfir:cfir-resolve` | 多 Phase 语义解析 | - | 🔜 | 阶段 7 |
| `cfir:cfir-serialization` | .cjo 序列化 | - | 🔜 | 阶段 10 |
| `chir` | CHIR 定义和转换 | - | 🔜 | 阶段 11 |

---

## 5. 测试框架架构

### 5.1 测试框架层次

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 测试框架层次结构                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ tests:test-infrastructure（零 IntelliJ 依赖核心，20 文件）                    │
│ ┌───────────────────────────────────────────────────────────────────────┐   │
│ │ • Directive 系统                                                       │   │
│ │   - SimpleDirective（布尔开关）                                        │   │
│ │   - StringDirective（字符串参数）                                      │   │
│ │   - ValueDirective<T>（类型化参数）                                    │   │
│ │                                                                       │   │
│ │ • TestModule 模型                                                      │   │
│ │   - TestFile（源文件）                                                 │   │
│ │   - TestModule（编译单元）                                             │   │
│ │   - DependencyDescription（依赖关系）                                  │   │
│ │                                                                       │   │
│ │ • TestServices 容器                                                    │   │
│ │   - 类型安全的服务访问                                                 │   │
│ │   - 委托模式                                                          │   │
│ │                                                                       │   │
│ │ • TestConfigurationBuilder DSL                                         │   │
│ │   - useFrontendFacades()                                               │   │
│ │   - useHandlers()                                                      │   │
│ │   - defaultDirectives()                                                │   │
│ └───────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│ ↑ 继承                                                                      │
│                                                                             │
│ analysis:analysis-test-framework                                           │
│ ┌───────────────────────────────────────────────────────────────────────┐   │
│ │ • AbstractAnalysisApiBasedTest                                         │   │
│ │ • MockApplication / MockProject                                        │   │
│ │ • AnalysisApiTestConfigurator                                          │   │
│ └───────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│ ↑ 使用                                                                      │
│                                                                             │
│ 各模块测试（psi2cfir、compiler-tests、ide-plugin-tests）                    │
│ ┌───────────────────────────────────────────────────────────────────────┐   │
│ │ • AbstractRawCfirBuilderTestCase                                       │   │
│ │ • TestGeneratorForPsi2Cfir（测试生成器）                                │   │
│ │ • CfirRenderer（CFIR 渲染器）                                           │   │
│ │ • Golden File 对比机制                                                  │   │
│ └───────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 testData 组织

```
testData/
├── rawBuilder/declarations/
│   ├── emptyFile.cj              → emptyFile.txt
│   ├── emptyClass.cj             → emptyClass.txt
│   ├── topLevelFunction.cj       → topLevelFunction.txt
│   ├── classWithMembers.cj       → classWithMembers.txt
│   ├── extendDeclaration.cj      → extendDeclaration.txt
│   ├── controlFlow.cj            → controlFlow.txt
│   └── ...（16 个测试文件）
├── lazyBodiesByAst/
│   └── .keep
└── sourceElementMapping/
    └── simpleExpr.cj
```

### 5.3 测试执行流程

```
┌─────────────────────┐
│ 1. 扫描 testData/   │
│    目录             │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ 2. 生成测试代码     │
│    TestGenerator    │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ 3. 执行测试         │
│    ./gradlew test   │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐     ┌──────────────────┐
│ 4. 构建 CFIR        │────▶│ PsiRawCfirBuilder│
└─────────┬───────────┘     └──────────────────┘
          │
          ▼
┌─────────────────────┐     ┌──────────────────┐
│ 5. 渲染 CFIR        │────▶│ CfirRenderer     │
└─────────┬───────────┘     └──────────────────┘
          │
          ▼
┌─────────────────────┐     ┌──────────────────┐
│ 6. 对比 Golden File │────▶│ .txt 期望文件     │
└─────────┬───────────┘     └──────────────────┘
          │
          ▼
┌─────────────────────┐
│ 7. 验证通过/失败    │
└─────────────────────┘
```

**Golden File 更新**：
```bash
./gradlew :cfir:raw-cfir:psi2cfir:test -Dupdate.test.data=true
```

---

## 6. 与官方编译器对比

### 6.1 架构差异

| 方面 | 官方 C++ 编译器 | 本 Kotlin 实现 |
|------|----------------|---------------|
| 语言 | C++ | Kotlin/JVM |
| 平台 | 原生 | JVM |
| 解析前端 | 自研 Lexer/Parser | IntelliJ PSI |
| 前端 IR | AST | CFIR |
| 后端 IR | CHIR | CHIR（规划） |
| 代码生成 | LLVM IR | LLVM IR（规划） |
| 序列化 | FlatBuffers | FlatBuffers（规划） |
| 阶段数 | 15 个 | 12 个（合并优化） |

### 6.2 功能对齐情况

| 官方阶段 | 本实现对应 | 处理方式 | 说明 |
|---------|-----------|---------|------|
| LOAD_PLUGINS | LOAD_PLUGINS | ✅ 保留 | 完全对应 |
| PARSE | PARSE | ✅ 保留 | 本实现使用 PSI，产物等价 |
| CONDITION_COMPILE | CONDITION_COMPILE | ✅ 保留 | 仓颉特有，裁剪 @When 条件分支 |
| IMPORT_PACKAGE | IMPORT_PACKAGE | ✅ 保留 | 加载 .cjo 外部依赖 |
| MACRO_EXPAND | MACRO_EXPAND | ✅ 保留 | 完全对应 |
| AST_DIFF | → 内嵌 CFIR_BUILD | 🔀 内嵌 | 不产出独立产物，作为构建前置 Guard |
| SEMA（前半） | → CFIR_BUILD | 🔀 拆分 | 对齐 K2 FIR_BUILD |
| SEMA（主体） | → CFIR_RESOLVE | 🔀 拆分 | 对齐 K2 FIR_RESOLVE |
| DESUGAR_AFTER_SEMA | → 合入 FINALIZE | 🔀 合并 | 语义后处理流水线 |
| GENERIC_INSTANTIATION | → 合入 FINALIZE | 🔀 合并 | 语义后处理流水线 |
| OVERFLOW_STRATEGY | → 合入 FINALIZE | 🔀 合并 | 轻量标注操作 |
| MANGLING | MANGLING | ✅ 保留 | 多线程修饰 |
| SAVE_CJO | SAVE_CJO | ✅ 合并 | 吸收 SAVE_RESULTS |
| CHIR | CFIR2CHIR | ✅ 改名 | 名称更明确 |
| CODEGEN | CODEGEN | ✅ 保留 | CHIR → LLVM IR |
| SAVE_RESULTS | → 合入 SAVE_CJO | 🔀 合并 | 仅 CHIR 模式延迟保存 |

### 6.3 官方编译器关键组件

```
external/cangjie_compiler/
├── include/cangjie/
│   ├── AST/              # AST 定义（Node.h, Types.h）
│   ├── Basic/            # 基础组件（Position, Linkage）
│   ├── CHIR/             # 后端 IR
│   ├── CodeGen/          # 代码生成
│   ├── ConditionalCompilation/  # 条件编译
│   ├── Driver/           # 编译器驱动
│   ├── Frontend/         # 前端（CompilerInstance）
│   ├── Lex/              # 词法分析
│   ├── Macro/            # 宏展开
│   ├── Mangle/           # 名称修饰
│   ├── Modules/          # 模块管理
│   ├── Parse/            # 语法分析
│   ├── Sema/             # 语义分析
│   └── Utils/            # 工具
│
├── src/                  # 实现源码（552 个 C++ 文件）
│
└── schema/               # FlatBuffers Schema
    ├── PackageFormat.fbs
    ├── NodeFormat.fbs
    └── ...
```

---

## 7. 与 Kotlin K2 对比

### 7.1 架构对齐度

| Kotlin K2 概念 | 仓颉实现 | 对齐程度 |
|---------------|---------|---------|
| `FirSession` | `CfirSession` | ✅ 完全对齐 |
| `FirResolvePhase` | `CfirResolvePhase` | ✅ 8/9 Phase 对齐 |
| `FirElement` | `CfirElement` | ✅ 完全对齐 |
| `ConeKotlinType` | `ConeCangjieType` | ✅ 完全对齐 |
| `PsiRawFirBuilder` | `PsiRawCfirBuilder` | ✅ 完全对齐 |
| `FirVisitor` | `CfirVisitor` | ✅ 完全对齐 |
| `Analysis API` | `CaSession` | ✅ 完全对齐 |
| 测试框架 | test-infrastructure | ✅ 完全对齐 |

### 7.2 仓颉特有扩展

| 特性 | 仓颉实现 | Kotlin 对应 | 说明 |
|------|---------|-------------|------|
| `extend Type <: Interface` | `CfirExtend` | 无（用扩展函数） | 仓颉特有类型扩展 |
| `struct` 值类型 | `CfirClassKind.STRUCT` | 无（全是引用类型） | 仓颉值类型 |
| `enum` ADT | `CfirClassKind.ENUM` | `enum class` | 枚举定义 |
| `match` 表达式 | `CfirMatchExpression` | `when` 表达式 | 模式匹配 |
| `spawn` 表达式 | `CfirSpawnExpression` | 无（用协程） | 并发原语 |
| `@When` 条件编译 | `CONDITION_COMPILE` 阶段 | `expect/actual` | 多平台支持 |
| 宏系统 | `MACRO_EXPAND` 阶段 | 编译器插件 | 元编程 |
| `IdealInt`/`IdealFloat` | `ConePrimitiveType` | 无 | 无限精度数值 |
| `VArray<T,N>` | `ConeVArrayType` | 无 | 定长数组 |
| EXTENSIONS Phase | `CfirResolvePhase.EXTENSIONS` | 无 | 独立解析阶段 |

---

## 8. Analysis API 集成

### 8.1 CaSession 组件

```kotlin
interface CaSession : CaLifetimeOwner,
    CaResolver,                      // 解析入口
    CaSymbolRelationProvider,        // 符号关系
    CaDiagnosticProvider,            // 诊断提供
    CaScopeProvider,                 // 作用域提供
    CaCompletionCandidateChecker,    // 补全检查
    CaExpressionTypeProvider,        // 表达式类型
    CaTypeProvider,                  // 类型提供
    CaTypeInformationProvider,       // 类型信息
    CaSymbolProvider,                // 符号提供
    CaCInteropComponent,             // C 互操作
    CaSymbolInformationProvider,     // 符号信息
    CaTypeRelationChecker,           // 类型关系检查
    CaExpressionInformationProvider, // 表达式信息
    CaEvaluator,                     // 表达式求值
    CaReferenceShortener,            // 引用缩短
    CaImportOptimizer,               // 导入优化
    CaRenderer,                      // 渲染
    CaVisibilityChecker,             // 可见性检查
    CaOriginalPsiProvider,           // 原始 PSI
    CaTypeCreator,                   // 类型创建
    CaAnalysisScopeProvider,         // 分析作用域
    CaSignatureSubstitutor,          // 签名替换
    CaSubstitutorProvider,           // 替换器提供
    CaDataFlowProvider,              // 数据流
    CaSourceProvider,                // 源码提供
    CaDocProvider {                  // 文档提供
        
    val useSiteModule: CaModule
}
```

### 8.2 Analysis API 使用示例

```kotlin
// 对齐 Kotlin 的 analyze { } 模式
analyze(cjFile) {
    // 在 analyze 块内可以安全使用 CaSession
    val type = expression.type
    
    // 解析引用
    val target = reference.resolve()
    
    // 获取作用域
    val scope = scopeProvider.getResolutionScope(symbol)
    
    // 检查可见性
    val isVisible = visibilityChecker.isVisible(symbol, useSiteModule)
}
```

---

## 9. 项目演进路径

### 9.1 历史演进

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 项目演进时间线                                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ Phase 1: 基础设施（已完成）                                                  │
│ ├─ ✅ common 模块（Name, FqName, Visibility）                               │
│ ├─ ✅ util 模块（通用工具）                                                  │
│ └─ ✅ dependencies:intellij-core（IntelliJ 核心依赖）                        │
│                                                                             │
│ Phase 2: PSI 解析层（已完成）                                                │
│ ├─ ✅ psi 模块（418 个文件）                                                 │
│ ├─ ✅ CjFile / CjClass / CjExpression 完整层次                               │
│ └─ ✅ Stub 索引机制                                                          │
│                                                                             │
│ Phase 3: CFIR 数据模型（已完成）                                              │
│ ├─ ✅ cfir:cfir-common（Session, ModuleData）                                │
│ ├─ ✅ cfir:cfir-cones（Cone 类型系统）                                        │
│ └─ ✅ cfir:cfir-tree（IR 树，99 个文件）                                      │
│                                                                             │
│ Phase 4: Raw CFIR 构建（核心完成，进行中）                                     │
│ ├─ ✅ raw-cfir:raw-cfir-common（抽象基类）                                    │
│ ├─ ✅ raw-cfir:psi2cfir（PsiRawCfirBuilder）                                  │
│ ├─ ✅ 测试框架（test-infrastructure）                                         │
│ └─ ⚠️ CjBasicType 转换修复                                                   │
│                                                                             │
│ Phase 5: CFIR 解析（计划中）                                                  │
│ ├─ 🔜 cfir:cfir-resolve 模块                                                 │
│ ├─ 🔜 8 个 Phase 处理器实现                                                   │
│ └─ 🔜 诊断检查器（CHECKERS）                                                  │
│                                                                             │
│ Phase 6: 后端流水线（长期）                                                    │
│ ├─ 🔜 FINALIZE 阶段                                                          │
│ ├─ 🔜 MANGLING 阶段                                                          │
│ ├─ 🔜 cfir:cfir-serialization                                                │
│ └─ 🔜 chir 模块                                                              │
│                                                                             │
│ Phase 7: IDE 集成（长期）                                                      │
│ ├─ 🔜 Analysis API 完善                                                      │
│ ├─ 🔜 IDE 插件开发                                                            │
│ └─ 🔜 增量编译支持                                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 intellij-cangjie 演进

```
external/intellij-cangjie/ (K1 架构)
    ↓
    │ 问题：
    │ • 基于 Kotlin K1，不符合 K2 架构
    │ • PSI 解析器与官方编译器不完全一致
    │ • 缺少完整编译流程
    │ • 语义分析不完整
    ↓
当前项目 (K2 架构对齐)
    ↓
    │ 优势：
    │ • CFIR 数据模型对齐 Kotlin K2
    │ • 12 阶段编译流水线设计完整
    │ • 惰性分阶段解析机制（支持 IDE 场景）
    │ • 完整类型系统（ConeCangjieType）
    ↓
    │ 待补齐：
    │ • raw-cfir → cfir-resolve → ... 完整流水线
    │ • IDE Analysis API 集成
    ↓
未来：统一编译器 + IDE 插件
```

---

## 10. 风险与未知数

### 10.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 官方编译器语义差异 | 高 | 详细对比官方实现，编写兼容性测试 |
| Kotlin K2 API 变化 | 中 | 紧跟 Kotlin 版本，预留适配时间 |
| IntelliJ 平台版本升级 | 中 | 使用稳定 API，避免实验性功能 |
| 类型推断复杂度 | 高 | 分阶段实现，先简单场景后复杂场景 |
| 增量编译正确性 | 高 | 设计完善的缓存失效机制 |

### 10.2 实现挑战

| 挑战 | 说明 | 状态 |
|------|------|------|
| `CjBasicType` 转换 | 所有基础类型显示为错误 | ⚠️ 待修复 |
| Match 模式匹配 | 仅支持简单模式 | 🔜 计划中 |
| 泛型类型推断 | 需要完整的类型推断引擎 | 🔜 计划中 |
| 宏展开 | 官方实现复杂，需仔细对齐 | 🔜 计划中 |
| 条件编译 | 需要在 AST 层裁剪 | 🔜 计划中 |
| 增量编译 | 需要设计 AST Diff 机制 | 🔜 计划中 |

### 10.3 待探索领域

1. **序列化格式**: .cjo 文件的 FlatBuffers Schema 设计
2. **CHIR 设计**: 后端 IR 的具体结构和优化 Pass
3. **插件系统**: MetaTransform 插件接口设计
4. **调试信息**: DWARF 调试信息生成
5. **跨平台支持**: OHOS、Linux、Windows 的平台差异处理

---

## 11. 下一步行动

### 11.1 短期任务（本周）

1. ✅ 修复 `CjBasicType` 转换问题（已在规格文档中提供方案）
2. 🔜 验证修复，运行所有测试
3. 🔜 补充基础类型测试用例

### 11.2 中期任务（2-4 周）

1. 🔜 实现 `cfir-resolve` 核心框架
2. 🔜 实现 `IMPORTS` Phase 处理器
3. 🔜 实现 `TYPES` Phase 处理器
4. 🔜 开始 `BODY_RESOLVE` Phase 设计

### 11.3 长期目标

- **IDE Analysis API 集成**: 为 IDE 插件提供语义分析能力
- **增量编译支持**: 基于 AST Diff 的增量构建
- **完整诊断系统**: 编译错误和警告报告
- **插件系统**: 支持编译器插件扩展

---

## 12. 参考资料

### 12.1 内部文档

- `readme.md` - 项目总览
- `cjfir-compiler-stages.md` - 12 阶段编译流水线设计
- `CLAUDE.md` - 开发指南
- `AGENTS.md` - 编码代理操作指南
- `openspec/specs/raw-cfir-implementation/spec.md` - Raw CFIR 实现规格

### 12.2 外部参考

- `external/cangjie_compiler/` - 官方 C++ 编译器（552 个文件）
- `external/kotlin/compiler/fir/` - Kotlin K2 FIR 实现
- `external/intellij-cangjie/` - 现有 IntelliJ 插件（K1）
- Kotlin K2 官方文档: https://kotlinlang.org/docs/whatsnew20.html
- IntelliJ Platform SDK: https://plugins.jetbrains.com/docs/intellij/

---

## 附录 A: 关键文件索引

### A.1 核心接口

| 文件 | 职责 |
|------|------|
| `cfir/cfir-tree/src/org/cangjie/cfir/CfirElement.kt` | IR 根接口 |
| `cfir/cfir-cones/src/org/cangjie/cfir/types/ConeCangjieType.kt` | Cone 类型根类 |
| `cfir/cfir-tree/src/org/cangjie/cfir/declarations/CfirDeclaration.kt` | 声明层次 |
| `cfir/cfir-tree/src/org/cangjie/cfir/expressions/CfirExpression.kt` | 表达式基类 |
| `cfir/cfir-common/src/org/cangjie/cfir/session/CfirSession.kt` | Session 组件中心 |
| `analysis/analysis-api/src/org/cangjie/analysis/api/CaSession.kt` | Analysis API 入口 |

### A.2 构建器

| 文件 | 职责 |
|------|------|
| `cfir/raw-cfir/psi2cfir/src/org/cangjie/cfir/builder/PsiRawCfirBuilder.kt` | PSI → Raw CFIR |
| `cfir/raw-cfir/raw-cfir-common/src/org/cangjie/cfir/builder/AbstractRawCfirBuilder.kt` | 构建器基类 |
| `cfir/raw-cfir/psi2cfir/src/org/cangjie/cfir/builder/PsiConversionUtils.kt` | 类型转换工具 |

### A.3 渲染器

| 文件 | 职责 |
|------|------|
| `cfir/cfir-tree/src/org/cangjie/cfir/renderer/CfirRenderer.kt` | CFIR 渲染（Golden File） |

---

**文档维护**: 本文档应在架构变更时及时更新，确保与实际代码保持同步。

**最后更新**: 2026-03-09  
**下次审查**: 建议在实现 cfir-resolve 阶段前进行架构审查。
