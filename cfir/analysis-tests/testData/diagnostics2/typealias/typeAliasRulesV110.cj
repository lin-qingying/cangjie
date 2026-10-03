// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_VERSION: 1.1.0
// 本文件是 typeAliasRules.cj 中只在 1.1.0 及以后产出诊断的 1 个段（形态 8 的 V113 段）。
// LLT 框架的版本指令是**文件级**指令、不能写在 `// FILE:` 段内，故单独成文件。
// 其余取证说明见 typeAliasRules.cj 头部。

// FILE: typeAliasCycleUnusedParamGateV113.cj
<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!><!TYPEALIAS_CYCLE!>t<!><!>ype TARBF<T> = TARBG<T>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!><!TYPEALIAS_CYCLE!>t<!><!>ype TARBG<U> = TARBF<U>

func tarMainCycleUnusedGateV113() {}

// ===== 形态 9：形参放进**被引用别名的类型实参列表**（`TARBH<T> = TARBI<Int64>`）**不算使用** —— `usedTys` 只收 RHS 类型树
// 的类型实参（`GetTyArgsRecursive`，TypeCheckDecl.cpp:648-658），`Int64` 里没有 T → T 未用；两版一条 warning。
// 对照：形参出现在**类**的类型实参上（`type A<T, K> = C<T>`，C 是类）算使用，只报 K（形态 7）。
