# Raw CFIR 实现规格说明

## 1. 概述

### 1.1 背景

仓颉编译器项目采用 12 阶段编译流水线，当前优先级是实现 **Raw CFIR** 阶段（阶段 6）。Raw CFIR 构建器（`PsiRawCfirBuilder`）负责将 PSI 语法树转换为未解析的 CFIR 中间表示，对齐 Kotlin K2 的 `PsiRawFirBuilder` 架构。

### 1.2 项目演进路径

```
external/intellij-cangjie/ (K1 架构)
    ↓ 问题：基于 Kotlin K1，不符合 K2 架构
    ↓ PSI 解析器与官方编译器不完全一致
    ↓ 缺少完整编译流程
    ↓
当前项目 (K2 架构对齐)
    ↓ 优势：CFIR 数据模型对齐 Kotlin K2
    ↓ 12 阶段编译流水线设计完整
    ↓ 惰性分阶段解析机制（支持 IDE 场景）
    ↓
    待补齐：raw-cfir → cfir-resolve → ... 完整流水线
```

### 1.3 设计目标

1. **架构对齐**: 完全对齐 Kotlin K2 的 FIR 架构
2. **完整覆盖**: 支持仓颉语言全部语法结构
3. **测试驱动**: 采用 Golden File 对比机制验证转换正确性
4. **可扩展性**: 支持未来 IDE 插件集成

---

## 2. 当前实现状态

### 2.1 已实现模块

| 模块 | 职责 | 状态 |
|------|------|------|
| `cfir-common` | CfirSession、CfirModuleData、SourceElement | ✅ 完成 |
| `cfir-cones` | 完整类型系统 (ConeCangjieType) | ✅ 完成 |
| `cfir-tree` | IR 树、Visitor、Scope、Symbol | ✅ 完成 |
| `psi` | PSI 定义（仓颉语言语法） | ✅ 完成 |
| `raw-cfir:psi2cfir` | PSI → Raw CFIR 构建器 | ✅ 核心完成 |
| `raw-cfir:raw-cfir-common` | 抽象构建器基类 | ✅ 完成 |

### 2.2 核心架构

```
┌─────────────────────────────────────────────────────────────┐
│ PSI (IntelliJ)                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ CjFile / CjClass / CjFunction / CjExpression ...       │ │
│ └─────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────┘
                         │ PsiRawCfirBuilder.buildCfirFile()
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Raw CFIR (未解析)                                           │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ CfirFile                                                 │ │
│ │ ├── CfirDeclaration (sealed)                             │ │
│ │ │   ├── CfirClass / CfirFunction / CfirProperty          │ │
│ │ │   └── CfirExtend (仓颉特有: extend Type <: Interface)  │ │
│ │ ├── CfirExpression (sealed)                              │ │
│ │ │   ├── CfirFunctionCall / CfirPropertyAccess            │ │
│ │ │   └── CfirIfExpression / CfirMatchExpression           │ │
│ │ ├── CfirTypeRef（按语法形态细分，如 User/Basic/Function/Tuple/VArray，均为未解析 type ref） │ │
│ │ └── CfirReference (全为 CfirNamedReference，未绑定符号)   │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 PsiRawCfirBuilder 实现分析

#### 核心类层次

```kotlin
// 基类（泛型设计）
abstract class AbstractRawCfirBuilder<T : Any>(
    val baseSession: CfirSession,
    val context: Context<T> = Context(),
)

// PSI 实现
class PsiRawCfirBuilder(session: CfirSession) : 
    AbstractRawCfirBuilder<PsiElement>(session) {
    
    fun buildCfirFile(file: CjFile): CfirFile
    
    inner class Visitor {
        fun convertDeclaration(psi: CjDeclaration): CfirDeclaration
        fun convertExpression(psi: CjExpression): CfirExpression
    }
}
```

#### 已实现的转换

**声明转换**（完整）：
- ✅ `CjClass` → `CfirClass` (支持 class/interface/struct/enum)
- ✅ `CjExtend` → `CfirExtend` (仓颉特有)
- ✅ `CjNamedFunction` → `CfirFunction`
- ✅ `CjProperty` → `CfirProperty`
- ✅ `CjPrimaryConstructor` / `CjSecondaryConstructor` → `CfirConstructor`
- ✅ `CjTypeAlias` → `CfirTypeAlias`
- ✅ `CjParameter` → `CfirValueParameter`
- ✅ `CjTypeParameter` → `CfirTypeParameter`

**表达式转换**（完整）：
- ✅ 字面量: `CjConstantExpression` → `CfirLiteralExpression`
- ✅ 字符串插值: `CjStringTemplateExpression` → `CfirStringInterpolation`
- ✅ 二元运算: `CjBinaryExpression` → `CfirBinaryOp` / `CfirComparisonExpression`
- ✅ 一元运算: `CjPrefixExpression` / `CjPostfixExpression` → `CfirFunctionCall`
- ✅ 函数调用: `CjCallExpression` → `CfirFunctionCall`
- ✅ 成员访问: `CjDotQualifiedExpression` → `CfirPropertyAccess` / `CfirFunctionCall`
- ✅ 控制流: `CjIfExpression` / `CjMatchExpression` / `CjForExpression` / `CjWhileExpression`
- ✅ 跳转: `CjReturnExpression` / `CjBreakExpression` / `CjContinueExpression`
- ✅ 异常: `CjTryExpression` / `CjThrowExpression`
- ✅ Lambda: `CjLambdaExpression` → `CfirLambdaExpression`
- ✅ 数组/元组: `CjArrayLiteralExpression` / `CjTupleExpression`
- ✅ 类型检查: `CjIsExpression` → `CfirTypeOperator`
- ✅ 仓颉特有: `CjSpawnExpression` → `CfirSpawnExpression`

---

## 3. 已知问题与待改进

### 3.1 关键问题：特殊类型引用覆盖仍需持续补齐

**问题描述**：
当前 Raw CFIR 的类型引用建模仍依赖 `PsiConversionUtils.kt` 的显式分支覆盖；任何已存在于 PSI/语法层、但未在该层补齐 lowering 的类型都会被降级为错误类型引用。因此需要持续对齐诸如基础类型、`VArray<T, $N>` 等特殊语法类型的覆盖范围。

**典型代码路径**：
```kotlin
private fun CjTypeElement.toFirTypeRef(...): CfirTypeRef = when (this) {
    is CjBasicType -> toFirBasicTypeRef(...)
    is CjUserType -> toFirUserTypeRef(...)
    is CjFunctionType -> toFirFunctionTypeRef(...)
    is CjTupleType -> toFirTupleTypeRef(...)
    is CjVArrayType -> toFirVArrayTypeRef(...)
    else -> CfirErrorTypeRef(reason = "Unsupported type element: ${javaClass.simpleName}")
}
```

**当前状态**：
- `CjBasicType` 已映射为 `CfirBasicTypeRef`；
- `CjVArrayType` 已映射为专用 `CfirVArrayTypeRef`，保留元素类型与尺寸字面量；
- 后续新增语法类型时，应继续遵循“RAW 阶段保留语法结构、resolve 阶段再引入语义类型”的边界。

### 3.2 其他待改进项

| 问题 | 优先级 | 说明 |
|------|--------|------|
| 特殊类型引用覆盖（如 `VArray`） | 🔴 高 | PSI 已支持但 lowering 缺失时会直接退化为错误类型 |
| Match 表达式模式简化 | 🟡 中 | 当前仅支持简单的常量模式和通配符 |
| 错误处理增强 | 🟡 中 | 缺少源码位置信息的错误表达式 |
| 注解支持 | 🟢 低 | 暂未处理注解节点 |
| 操作符转换完整性 | 🟢 低 | 部分操作符名称映射需完善 |

---

## 4. 测试框架

### 4.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│ tests:test-infrastructure (零 IntelliJ 依赖核心)             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ • Directive 系统 (Simple/String/Value)                  │ │
│ │ • TestModule 模型                                        │ │
│ │ • TestServices 容器                                      │ │
│ │ • TestConfigurationBuilder DSL                          │ │
│ └─────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ cfir:raw-cfir:psi2cfir 测试                                 │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ • AbstractRawCfirBuilderTestCase (测试基类)              │ │
│ │ • TestGeneratorForPsi2Cfir (测试生成器)                  │ │
│ │ • CfirRenderer (CFIR 渲染器)                             │ │
│ │ • Golden File 对比机制                                    │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 测试生成机制

**生成器**：`TestGeneratorForPsi2Cfir.kt`
```kotlin
object TestGeneratorForPsi2Cfir {
    @JvmStatic
    fun main(args: Array<String>) {
        generateSuite(
            modelRelativePath = "cfir/raw-cfir/psi2cfir/testData/rawBuilder",
            generatedClassName = "RawCfirBuilderTestCaseGenerated",
            baseClassName = "AbstractRawCfirBuilderTestCase",
        )
    }
}
```

**生成流程**：
1. 扫描 `testData/rawBuilder/` 目录下的 `.cj` 文件
2. 为每个文件生成对应的测试方法
3. 输出到 `tests-gen/RawCfirBuilderTestCaseGenerated.kt`

### 4.3 testData 组织

```
testData/rawBuilder/declarations/
├── emptyFile.cj              → emptyFile.txt
├── emptyClass.cj             → emptyClass.txt
├── topLevelFunction.cj       → topLevelFunction.txt
├── classWithMembers.cj       → classWithMembers.txt
├── extendDeclaration.cj      → extendDeclaration.txt
├── controlFlow.cj            → controlFlow.txt
└── ...
```

```
testData/rawBuilder/expressions/
├── binaryMissingRightOperand.cj   → binaryMissingRightOperand.txt / .lazyBodies.txt
├── ifMissingCondition.cj          → ifMissingCondition.txt / .lazyBodies.txt
├── whileMissingCondition.cj       → whileMissingCondition.txt / .lazyBodies.txt
├── throwMissingExpression.cj      → throwMissingExpression.txt / .lazyBodies.txt
└── forMissingIterable.cj          → forMissingIterable.txt / .lazyBodies.txt
```

### 4.4 Golden File 对比

**执行流程**：
```kotlin
fun doRawCfirTest(filePath: String) {
    val cjFile = createCjFile(file.nameWithoutExtension, sourceText)
    val cfirFile = cjFile.toCfirFile()
    val actual = CfirRenderer.render(cfirFile)
    assertEqualsToFile(File(expectedPath), actual)
}
```

**更新 Golden File**：
```bash
./gradlew :cfir:raw-cfir:psi2cfir:test -Dupdate.test.data=true
```

### 4.5 测试覆盖度

| 语法结构 | 测试覆盖 | 说明 |
|---------|---------|------|
| 空文件 | ✅ | `emptyFile.cj` |
| 类声明 | ✅ | `emptyClass.cj`, `classWithMembers.cj` |
| 接口声明 | ✅ | `interfaceDeclaration.cj` |
| Struct 声明 | ✅ | `structDeclaration.cj` |
| Enum 声明 | ✅ | `enumDeclaration.cj` |
| Extend 声明 | ✅ | `extendDeclaration.cj` |
| 泛型类 | ✅ | `classWithTypeParameters.cj` |
| 继承关系 | ✅ | `classWithSupertype.cj` |
| 顶层函数 | ✅ | `topLevelFunction.cj` |
| 顶层属性 | ✅ | `topLevelProperty.cj` |
| TypeAlias | ✅ | `typeAlias.cj` |
| 控制流 | ✅ | `controlFlow.cj` |
| 函数表达式 | ✅ | `functionExpressions.cj` |
| Package/Import | ✅ | `packageAndImport.cj` |
| 缺失表达式恢复 | ✅ | `rawBuilder/expressions/*` 覆盖缺失右操作数、缺失 `if/while` 条件、缺失 `throw` 表达式与缺失 `for-in` 可迭代对象 |

---

## 5. 与 Kotlin K2 对比

### 5.1 架构对齐

| Kotlin K2 | 仓颉 CFIR | 对齐程度 |
|-----------|----------|---------|
| `AbstractRawFirBuilder<T>` | `AbstractRawCfirBuilder<T>` | ✅ 完全对齐 |
| `PsiRawFirBuilder` | `PsiRawCfirBuilder` | ✅ 完全对齐 |
| `FirSession` | `CfirSession` | ✅ 完全对齐 |
| `FirResolvePhase` | `CfirResolvePhase` | ✅ 8/9 Phase 对齐 |
| `FirElement` | `CfirElement` | ✅ 完全对齐 |
| `ConeKotlinType` | `ConeCangjieType` | ✅ 完全对齐 |

### 5.2 仓颉特有扩展

| 仓颉特性 | CFIR 表示 | Kotlin 对应 |
|---------|----------|-------------|
| `extend Type <: Interface` | `CfirExtend` 节点 | 无（用扩展函数） |
| `struct` 值类型 | `CfirClassKind.STRUCT` | 无（全是引用类型） |
| `enum` ADT | `CfirClassKind.ENUM` | `enum class` |
| `match` 表达式 | `CfirMatchExpression` | `when` 表达式 |
| `spawn` 表达式 | `CfirSpawnExpression` | 无（用协程） |
| `@When` 条件编译 | `CONDITION_COMPILE` 阶段 | `expect/actual` |
| 宏系统 | `MACRO_EXPAND` 阶段 | 编译器插件 |

### 5.3 可借鉴的设计

**Builder DSL**（Kotlin K2）：
```kotlin
// Kotlin 的构建 DSL
val myFunction = buildNamedFunction {
    name = Name.identifier("myFunction")
    returnTypeRef = buildTypeRef(...)
    valueParameters += buildValueParameter { ... }
}
```

**建议引入**：
```kotlin
// 仓颉可借鉴的 DSL
val myFunction = buildCfirFunction {
    name = CjName.identifier("myFunction")
    returnTypeRef = buildCfirTypeRef(...)
    valueParameters += buildCfirValueParameter { ... }
}
```

---

## 6. 实现建议

### 6.1 短期任务（立即）

| 任务 | 优先级 | 预估工时 |
|------|--------|---------|
| 修复 `CjBasicType` 转换 | 🔴 高 | 2h |
| 增强 Match 表达式模式支持 | 🟡 中 | 4h |
| 补充错误处理的源码位置 | 🟡 中 | 2h |

### 6.2 中期任务（下一阶段）

| 任务 | 优先级 | 说明 |
|------|--------|------|
| 实现 `cfir-resolve` 核心框架 | 高 | Phase 处理器设计 |
| 实现 `IMPORTS` Phase | 高 | 导入符号绑定 |
| 实现 `TYPES` Phase | 高 | 类型引用解析 |
| 实现 `BODY_RESOLVE` Phase | 中 | 表达式类型推断 |

### 6.3 长期目标

- **IDE Analysis API 集成**: 为 IDE 插件提供语义分析能力
- **增量编译支持**: 基于 AST Diff 的增量构建
- **诊断系统**: 完整的编译错误和警告报告

---

## 7. 参考资料

### 7.1 内部文档

- `readme.md` - 项目总览
- `cjfir-compiler-stages.md` - 12 阶段编译流水线设计
- `CLAUDE.md` - 开发指南

### 7.2 外部参考

- `external/kotlin/compiler/fir/raw-fir/` - Kotlin K2 FIR 实现
- `external/cangjie_compiler/` - 官方 C++ 编译器
- `external/intellij-cangjie/` - 现有 IntelliJ 插件（K1）

---

## 8. 附录

### 8.1 CfirRenderer 输出格式示例

**输入**：
```cangjie
func add(a: Int64, b: Int64): Int64 {
    return a + b
}
```

**输出**（修复后）：
```
FILE: add.cj
func add(a: R|Int64|, b: R|Int64|): R|Int64| {
    RETURN {
        FUNCTION_CALL(plus) {
            receiver:
                QUALIFIED_ACCESS(a)
            arguments:
                QUALIFIED_ACCESS(b)
        }
    }
}
```

### 8.2 测试运行命令

```bash
# 运行 raw-cfir 测试
./gradlew :cfir:raw-cfir:psi2cfir:test

# 更新 golden file
./gradlew :cfir:raw-cfir:psi2cfir:test -Dupdate.test.data=true

# 生成测试代码
./gradlew :cfir:raw-cfir:psi2cfir:generateRawCfirBuilderTests
```

---

**文档版本**: 1.0  
**最后更新**: 2026-03-09  
**维护者**: Cangjie Compiler Team
