// RUN_PIPELINE_TILL: FRONTEND
// VArray 表达式的花括号直接初始化形态：`VArray<T, $N>{ i => expr }`。
// 与圆括号调用形态 `VArray<T, $N>({ i => expr })`（见 syntax/expressions/constructionForms.cj）不同：花括号不是实参列表，而是挂在 VArray 原子表达式后的尾随闭包。
//
// 官方依据：
// - src/Parse/ParseAtom.cpp:1324 ParserImpl::ParseVArrayExpr：先 ParseVarrayType() 取 `<T, $N>`，再 Skip(LPAREN)；不是 `(` 时只要下一个 token 是 LCURL 就合法返回，
//   注释原文 "Only 'VArray <type, $n>(expr)' or 'VArray <type, $n>{ i => expr }' is valid grammar."（否则报 parse_varray_with_paren）。
// - src/Parse/ParseType.cpp:58 ParserImpl::ParseVarrayType：第二个泛型实参必须是 `$` + 整数字面量（否则 parse_varray_type_args_mismatch / parse_expect_integer_literal_varray）。
// - src/Parse/ParserImpl.h:174 ParserImpl::SeeingTrailingClosure：宿主是 ARRAY_EXPR（VArray 表达式）时允许尾随 `{ ... }`。
// - src/Parse/ParseExpr.cpp:789 ParserImpl::ParseTrailingClosureExpr、src/Parse/ParseExpr.cpp:814 ParserImpl::CheckBaseOfTrailingClosureExpr：合法宿主之一即注释所列 "4. VArrayExpr"。
// - src/Parse/ParseExpr.cpp:973 ParserImpl::ParseBaseExprPostfix：尾随闭包后缀只在未跨行（!newlineSkipped）时接在原子表达式之后。
// - src/Sema/Desugar/DesugarBeforeTypeCheck.cpp:444 DesugarTrailingClosureExpr：宿主是 ARRAY_EXPR 时，把 lambda 当作 ArrayExpr 的最后一个实参。
// - src/Sema/TypeCheckBuiltinExpr.cpp:370 ChkVArrayArg：VArray 恰好一个实参，无名 lambda 的期望类型是 `(Int64) -> T`；同文件 :401 ChkVArrayExpr 做 VArray 类型检查。
// - src/CodeGen/CJNative/CJNativeIntrinsicsCall.cpp:974 IRBuilder2::VArrayInitedByLambda：下标从 0 递增到 `$N` 减一逐个调用 lambda，所以 `i` 是 Int64 元素下标，不是长度本身。
// 官方文档《数组类型》VArray 段只写了 `VArray<T, $N>(initElement: (Int64) -> T)` 与 `repeat:` 两种构造子，花括号形态以上述解析器注释与检查为准。
// cjc 1.0.5 / 1.1.3 零诊断（--output-type=staticlib）。
//
// 形态清单：
// 1. let 初值 + 下标读取 + size；2. 函数返回位置（return 语句与块体最后一条表达式语句）；3. 表达式位置后缀 `.size` / `[n]`；
// 4. var 下标写入；5. while + 下标遍历（VArray 不实现 Iterator，不能 for-in）；6. 带类型标注的 let；
// 7. 实参位置 / 二元运算操作数 / 括号整体表达式；8. lambda 恰好一个形参（`i`、`i: Int64`、`_`），体可为表达式、多语句、if、match；
// 9. 花括号体跨行与体前注释，`>` 与 `{` 之间一个空格合法（跨行会让两版 cjc 崩溃，见 VArrayBraceNotAllowed 注释）；
// 10. 元素类型由 `T` 决定（Int32 / Float64 / Float32 / Bool / Int8 / Rune / UInt64），`$0` 长度合法；
// 11. 嵌套 VArray 作为元素类型与下标链 `a[1][0]`。
const VArrayBraceStep: Int64 = 3

// 形态 1：let 初值 + 下标读取 + size
func VArrayBraceLetInit(): Int64 {
    let v = VArray<Int64, $2>{ i => i }
    return v[0] + v[1] + v.size
}

// 形态 2：函数返回位置
func VArrayBraceReturn(): VArray<Int64, $3> {
    return VArray<Int64, $3>{ i => i * 2 }
}

// 形态 2：块体最后一条表达式语句
func VArrayBraceReturnDirect(): VArray<Int64, $2> {
    VArray<Int64, $2>{ i => i }
}

// 形态 3 / 4 / 5：下标与 size 后缀、var 下标写入、while + 下标遍历
func VArrayBraceIndex(): Int64 {
    var v = VArray<Int64, $3>{ i => i * 10 }
    v[1] = v[0] + v[2]
    var k: Int64 = 0
    var sum: Int64 = v[1]
    while (k < v.size) {
        sum += v[k]
        k++
    }
    return sum + VArrayBraceCount(v) + VArray<Int64, $2>{ i => i }.size
}

// VArray 不实现 Iterator，只能按下标遍历
func VArrayBraceCount(a: VArray<Int64, $3>): Int64 {
    var k: Int64 = 0
    while (k < a.size) {
        k++
    }
    return k
}

// 形态 6：带类型标注的 let
func VArrayBraceAnnotated(): Int64 {
    let v: VArray<Int64, $2> = VArray<Int64, $2>{ i => i }
    return v[1]
}

// 形态 7：实参位置、二元运算操作数、括号整体表达式
func VArrayBraceAdd(a: Int64, b: Int64): Int64 {
    return a + b
}

func VArrayBraceOperands(): Int64 {
    let x = VArrayBraceAdd(VArray<Int64, $2>{ i => i }[0], VArray<Int64, $2>{ i => i }[1])
    let y = (VArray<Int64, $2>{ i => i }).size
    return x + y
}

// 形态 8：lambda 恰好一个形参，形参可带类型标注或写成 _；体可以是表达式、多语句、if、match
func VArrayBraceBodies(): Int64 {
    let plain = VArray<Int64, $2>{ i => i }
    let typed = VArray<Int64, $2>{ i: Int64 => i * 2 }
    let ignored = VArray<Int64, $2>{ _ => 0 }
    let block = VArray<Int64, $2>{ i => let x = i + 1; x * 2 }
    let branch = VArray<Int64, $2>{ i => if (i == 0) { VArrayBraceStep } else { i } }
    let matched = VArray<Int64, $2>{ i => match (i) {
        case 0 => 10
        case _ => 20
    } }
    return plain[1] + typed[1] + ignored[1] + block[1] + branch[1] + matched[1]
}

// 形态 9：花括号体跨行、体前注释；`>` 与 `{` 之间一个空格合法
func VArrayBraceLayout(): Int64 {
    let multiline = VArray<Int64, $2>{
        i => i
    }
    let commented = VArray<Int64, $2>{ i =>
        // i 是元素下标，取值 0..$N-1
        i + VArrayBraceStep
    }
    let spaced = VArray<Int64, $2> { i => i }
    return multiline[0] + commented[0] + spaced[0]
}

// 形态 10：数值元素类型由 `T` 决定，`$0` 长度合法
func VArrayBraceElemTypes(): Int64 {
    let i32v = VArray<Int32, $2>{ i => Int32(i) }
    let f64v = VArray<Float64, $2>{ i => Float64(i) }
    let f32v = VArray<Float32, $2>{ i => Float32(i) }
    let i8v = VArray<Int8, $2>{ i => Int8(i) }
    let u64v = VArray<UInt64, $2>{ i => UInt64(i) }
    let zero = VArray<Int64, $0>{ i => i }
    return Int64(i32v[1]) + Int64(f64v[1]) + Int64(f32v[1]) + Int64(i8v[1]) + Int64(u64v[1]) + zero.size
}

// 形态 10：Bool / Rune 元素类型
func VArrayBraceElemBoolAndRune(): Bool {
    let boolv = VArray<Bool, $2>{ i => i == 0 }
    let runeV = VArray<Rune, $2>{ _ => r'a' }
    return boolv[0] && runeV[1] == r'b'
}

// 形态 11：嵌套 VArray 作为元素类型与下标链
func VArrayBraceNested(): Int64 {
    let a = VArray<VArray<Int64, $2>, $2>{ i => VArray<Int64, $2>{ j => j + i } }
    return a[1][0] + a[0].size
}

// 形态 11：嵌套 VArray 作为返回值
func VArrayBraceNestedReturn(): VArray<VArray<Int64, $2>, $2> {
    return VArray<VArray<Int64, $2>, $2>{ i => VArray<Int64, $2>{ j => j + i } }
}

// 被官方拒绝的花括号形态（此处不出现，仅备注）：
// - `VArray<T, $N>\n{ i => i }`（类型与花括号之间换行）：两版 cjc 都 Internal Compiler Error "begin of range is zero"（Error Code: 6）。
// - `VArray<T, $N>{}`、`VArray<T, $N>{ => 1 }`（无 / 零形参）：sema_mismatched_types，期望 `(Int64) -> T`。
// - `VArray<T, $N>{ i, j => ... }`（两个形参）：sema_param_miss_match "expected '1', found '2'"。
// - `VArray<T, $N>{ (i) => ... }`（圆括号形参）：parse_expected_character / parse_expected_expr_or_decl_in。
// - `VArray<T, $N>{ repeat: 0 }`：parse_expected_type，具名实参 repeat 只属于圆括号形态。
// - `VArray<T, $N>{ i => exprOfWrongType }`：sema_mismatched_types `(Int64) -> T`。
// - `for (x in v)`（v 为 VArray）：sema_expr_in_forin_must_has_iterator，VArray 不实现 Iterator。
