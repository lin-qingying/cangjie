# S4 冻结类型角色：源码草稿、authority 与独立回归计划

两份 CJ 原稿按已采纳草稿原样新增。本报告保留此前只读核验得到的完整 authority、断言和独立回归计划。两份源均**尚未 cjc 验证**；固定 SDK 的 `cjc.exe` 路径已确认存在，但未调用 cjc 或 Gradle，未执行 ABI 后端、外部 C 函数或跨语言链接测试。本次仅新增这两份源及本报告，未修改任何已存在生产／测试代码、原 fixture 或 golden。

主线程待当前 compileKotlin 编译终态后取得官方 cjc 证据并新增 Kotlin 测试。本报告是待官方产物验证的源码／协议断言依据，不是新 fixture 已通过验证或 S4 已接受的结论。

native 证据全部取自 `git show 14276740ffec1262b81b6b5236b1d840af270b37`，Kotlin 取自 `3c7a5041f238102285764d370665f56f587f9dc4`。下述 native 行号指固定提交，文件链接用于定位。已确认语义还复用 [ordered-type-payload-native-authority.md](../../docs/cjmp-source-helper-20261117/ordered-type-payload-native-authority.md) 及下文列明的 docs MCP 文档。

新增候选：[orderedAliasRoles.cj](../../cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/orderedAliasRoles.cj)

```cangjie
package s4_frozen_type_roles

// Alias 声明自身、RHS 和使用槽分别保留其类型角色。
public type Pair<T> = (T, T)
public type PairLink<T> = Pair<T>
public type IntPair = PairLink<Int64>
public type IntPairChain = IntPair

// self type 的实参绑定本类自己的 T、U；This 独立保留标记。
public class Carrier<T, U> {
    public func asThis(): This {
        return this
    }

    public func asNominal(): Carrier<T, U> {
        return this
    }

    public func choose(first: T, middle: U, last: T): U {
        return middle
    }

    public func pair(value: T): Pair<T> {
        return (value, value)
    }
}

// 同名 T 属于函数，与 Alias、class 的 T 分别绑定。
public func pairOf<T>(value: T): Pair<T> {
    return (value, value)
}

public func orderedSlots(first: Int64, middle: Bool, last: Int64): Bool {
    return middle
}

public func reorderedSlots(first: Bool, middle: Int64, last: Int64): Bool {
    return first
}

public func repeatedReturn(first: Int64, last: Int64): Int64 {
    return first
}

// 接口默认正文提供真实 SOURCE 使用路径。
public interface FrozenTypeSource {
    func aliasUse(value: IntPairChain): IntPairChain {
        return value
    }

    func tupleUse(value: (Int64, Int64)): (Int64, Int64) {
        return value
    }

    func functionUse(
        callback: (Int64, Bool, Int64) -> Bool,
        first: Int64,
        middle: Bool,
        last: Int64
    ): Bool {
        return callback(first, middle, last)
    }

    func functionReturn(
        callback: (Int64, Bool, Int64) -> Bool
    ): (Int64, Bool, Int64) -> Bool {
        return callback
    }

    func carrierUse(value: Carrier<Int64, Bool>): Carrier<Int64, Bool> {
        return value.asThis()
    }

    func publicArrayUse(
        value: Array<Array<Int64>>
    ): Array<Array<Int64>> {
        return value
    }
}
```

可核验断言如下。`Pair::T` 等记号表示所属声明的真实 TP 身份，断言时应通过 `GenericTyInfo.declPtr` 找到该声明。

| 检查对象 | 固定语义与 wire 断言 |
|---|---|
| `Pair` 普通 SOURCE 声明的 `Decl.type` | `kind=Type`、`CompositeTyInfo.declPtr=Pair`、实参 `[Pair::T]`、`isThisTy=false` |
| `Pair` 的 `AliasInfo.aliasedTy` | `kind=Tuple`、元素 `[Pair::T, Pair::T]`；长度为 2，重复槽及绑定保留 |
| `PairLink` 的 self / RHS | self 为 `Type(PairLink,[PairLink::T])`；RHS 为 `Type(Pair,[PairLink::T])` |
| public Alias 链 | `IntPair` RHS 为 `Type(PairLink,[Int64])`；`IntPairChain` RHS 为 `Type(IntPair,[])` |
| `aliasUse` 参数与返回 | 两个显式使用槽均保留 `Type(IntPairChain,[])`；展开语义为 `(Int64,Int64)` |
| `tupleUse` 参数与返回 | 使用槽直接为 `Tuple[Int64,Int64]`，与 Alias 使用路径区分 |
| `Carrier` 声明 self | `kind=Class`，绑定 `Carrier`，有序实参 `[Carrier::T,Carrier::U]`，`isThisTy=false` |
| `asThis` / `asNominal` 返回 | 都绑定 `Carrier` 及其 `[T,U]`；前者 `isThisTy=true`，后者 `false` |
| `choose` | 参数 `[Carrier::T,Carrier::U,Carrier::T]`，返回独立为 `Carrier::U` |
| `Carrier.pair` / `pairOf` 返回 | 分别为 `Type(Pair,[Carrier::T])`、`Type(Pair,[pairOf::T])`；不能按名字 `T` 合并绑定 |
| `orderedSlots` / `reorderedSlots` | wire 参数分别为 `[Int64,Bool,Int64]`、`[Bool,Int64,Int64]`；返回均为独立的 `Bool` 槽 |
| `repeatedReturn` | 参数向量长度 2，两个 `Int64` 槽都存在；独立 `retType=Int64` |
| `functionReturn` | 外层函数参数向量长度 1，参数和返回均指向内层函数类型；内层参数长度 3，返回为 `Bool` |
| `publicArrayUse` | 用户类型为嵌套的 `std.core.Array` nominal 类型；不能解释成 RawArray 的 `dims=2` |

**槽独立不要求 ID 全部不同。** 无实参 Alias 的声明 self 与相同 Alias 使用可以共享类型 ID；重复 primitive 槽也可共享 ID。需检查槽位置、长度、类型种类和声明绑定。

依据：

- docs MCP：`manual_source_zh_cn_typealias` 确认顶层 Alias、泛型 Alias、无循环链及“别名不定义新类型”；`manual_source_zh_cn_class_This_类型_8` 确认实例方法 `This` 返回；`manual_source_zh_cn_first_class_citizen` 确认重复参数、函数参数和函数返回语法。
- [PreCheck.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/PreCheck.cpp)：L638–640、734–750 建立 Alias self 与独立 RHS；L850–863 保存实例化 Alias 身份再展开；L1731–1747 构造绑定类自身 TP 的 `ClassThisTy`。
- [TypeManager.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeManager.cpp)：L2411–2428 根据节点及 Alias 导出资格选择使用类型；L2447–2486 分别处理函数、tuple、参数与声明节点。
- [ASTWriter.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Modules/ASTSerialization/ASTWriter.cpp)：L995–1012 写 nominal 实参和 `isThisTy`；L1033–1064 分别写函数参数、返回及 tuple；L1448–1460 写独立 Alias RHS；L1601–1602 获取声明 self type。
- 固定 Kotlin [FirElementSerializer.kt](https://github.com/JetBrains/kotlin/blob/3c7a5041f238102285764d370665f56f587f9dc4/compiler/fir/fir-serialization/src/org/jetbrains/kotlin/fir/serialization/FirElementSerializer.kt#L792)：underlying、expanded 和 abbreviation 分角色处理；仓颉具体断言依据上述 native 合同。

另备 ABI 候选：[functionAbi.cj](../../cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/functionAbi.cj)

```cangjie
package s4_function_abi

// foreign 与访问修饰符冲突；省略 @C 是官方允许的形式。
foreign func s4ForeignFixed(
    first: Int32, middle: Int64, last: Int32
): Int32

foreign func s4ForeignVariadic(
    first: Int32, middle: Int64, last: Int32, ...
): Int32

@C
public func cOrdered(first: Int32, middle: Int64, last: Int32): Int32 {
    return first
}

public func cjOrdered(first: Int32, middle: Int64, last: Int32): Int32 {
    return first
}

public type OrderedC = CFunc<(Int32, Int64, Int32) -> Int32>

// 默认正文保留 foreign 引用的真实 SOURCE 路径。
public interface FunctionAbiSource {
    func fixed(first: Int32, middle: Int64, last: Int32): Int32 {
        let result = unsafe { s4ForeignFixed(first, middle, last) }
        return result
    }

    func variadic(
        first: Int32, middle: Int64, last: Int32, extra: Int32
    ): Int32 {
        let result = unsafe {
            s4ForeignVariadic(first, middle, last, extra)
        }
        return result
    }

    func callback(): OrderedC {
        return cOrdered
    }

    func callbackUse(
        value: CFunc<(Int32, Int64, Int32) -> Int32>
    ): CFunc<(Int32, Int64, Int32) -> Int32> {
        return value
    }

    func ordinary(first: Int32, middle: Int64, last: Int32): Int32 {
        return cjOrdered(first, middle, last)
    }
}
```

| 函数类型来源 | 参数向量 | 返回 | `isC` | `hasVariableLenArg` |
|---|---|---|---|---|
| `cjOrdered` | `[Int32,Int64,Int32]` | `Int32` | false | false |
| `cOrdered` | 同上 | `Int32` | true | false |
| `s4ForeignFixed`，CJNative | 同上 | `Int32` | true | false |
| `s4ForeignVariadic`，CJNative | 同上，长度仍为 3 | `Int32` | true | true |
| `OrderedC` RHS 的函数类型 | 同上 | `Int32` | true | false |
| `callbackUse` 外层方法 | `[CFunc类型]` | 同一个 CFunc 类型 | false | false |

ABI 的已核验证据：

- docs MCP：`manual_source_zh_cn_cangjie_c_仓颉调用_C_的函数_1`、`manual_source_zh_cn_cangjie_c_CFunc_2`：foreign 无实现、调用处使用 unsafe；`@C` 顶层非泛型函数及 `CFunc` 合法；foreign 支持末尾 `...`。
- [ParserModifierRules.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParserModifierRules.cpp)：L142–154 明确禁止 `public foreign func`。
- [ParseDecl.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Parse/ParseDecl.cpp)：L2237–2275 限定 `...` 只能在 foreign 中、不能在首位、必须在末尾；移除伪参数并保存 variadic 标志。
- [Types.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/AST/Types.cpp)：L856–863 明确包含 `Int32`、`Int64` 的 primitive CType 支持。
- PreCheck L1798–1802 裁决 C 与 variadic；ASTWriter L1033–1050 写出参数向量及独立返回。native `FuncTy.typeArgs` 内部包含返回类型，而 wire 的 `SemaTy.typeArgs` **只写 `paramTys`**。
- [ModuleFormat.fbs](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/schema/ModuleFormat.fbs#L591) 没有 closure wire 字段。[Types.h](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/AST/Types.h#L684) 明确 `isClosureTy` 仅用于 CHIR，不据源码 lambda 发明 AST closure 期望。

此 ABI 草稿用于 SOURCE 类型、签名和默认正文取证；跨语言实际链接与执行需要配套 C 实现。本轮后端不执行。

RawArray 应进入内部类型构造回归。固定源码证据已经明确其内部用途：

- [TypeChecker.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp)：L1797–1808 注册 RawArray 时只设置 `GLOBAL/GENERIC`；L2105–2111 仅在编译非 imported 的 `std.core` 时添加。
- [Expression.h](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/include/cangjie/CHIR/IR/Expression/Expression.h) L1496 原文：`RawArray is an internal type used in struct Array of std.core`。
- TypeManager L113–126 的原始构造逻辑：

```cpp
if (Ty::IsTyCorrect(elemTy)) {
    if (elemTy->IsArray()) {
        auto arrayTy = RawStaticCast<ArrayTy*>(elemTy);
        tmpElemTy = arrayTy->typeArgs[0];
        tmpDims += arrayTy->dims;
    }
}
return GetTypeTy<ArrayTy>(tmpElemTy, tmpDims);
```

因此，内部 `GetArrayTy(GetArrayTy(Int64,1),1)` 的结果为叶子 `Int64`、`dims=2`、单个元素槽；再嵌套一层为 `dims=3`。ASTWriter L1067–1077 将其写为单个 `typeArgs[0]` 和 `ArrayTyInfo.dimsOrSize`。该结论来自内部构造合同，本轮没有用户源码 `RawArray<RawArray<T>>` 的 SDK 编译见证。

独立回归计划：

1. **官方取证。** 两份原稿已通过 `apply_patch` 原样落盘。主线程待当前编译终态后取证；临时输入仅放指定工作树的 `tmp/`，产物使用独立的 `build/s4-frozen-types-authority/`。记录固定 SDK 版本、源 SHA-256、命令、JSON diagnostics、AST、CJO 及其哈希。参数沿已确认 fixture 使用 `--output-type=staticlib --dump-ast --diagnostic-format=json`。解码官方 CJO 后，才固定声明数量、图边和产物摘要；正例要求零错误，warning 按实际结果记录。

2. **新增真实 frontend 套件。** 建议独立新增 `CfirSourceFrozenTypesTest` 和 `CfirSourceFrozenTypesLightTreeTest`，复用现有 frontend 流水线、1.1.3、WITH_STDLIB 和普通包配置。分别验收 frontend 完成产品、冻结 payload 和写出图，按上表检查角色及身份。新 fixture 的 SOURCE 默认正文必须实际到达相应使用槽。

3. **冻结生命周期。** 基于真实解析产品检查 Alias 实参、TP owner、This 标记、函数参数及独立返回槽。替换等值类型对象、重排重复槽、改变返回或 Alias 绑定后，旧冻结内容保持原值，输入验证拒绝改变后的图。写出期间不得推进 resolve 或重新展开 Alias。

4. **RawArray 内部回归。** 从正式 builtin 类型构造入口准备有效 primitive／Generic 叶子及嵌套层，检查维度累计、叶子绑定、单元素槽和 wire `dimsOrSize`；另检查 VArray 的该字段为 size。保留草稿中的公共 `Array<Array<Int64>>` 对照。

5. **执行边界。** 本轮仅交付原稿和报告，尚未 cjc 验证，不冒称接受。待主线程完成官方取证和实现后，以 `gradlew-queue.bat` 定向运行新增 PSI、LightTree 套件及相关 snapshot 回归；已有 correct fixtures、goldens 和官方输入哈希保持原样。此计划中的编译、测试和 ABI 后端执行均未在本轮进行。
