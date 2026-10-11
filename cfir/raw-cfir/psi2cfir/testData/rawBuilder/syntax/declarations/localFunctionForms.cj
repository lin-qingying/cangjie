// RUN_PIPELINE_TILL: FRONTEND
// 局部函数（函数体内的 func 声明）形态清单：
//   1. 普通函数体内的局部 func（捕获外层形参）；2. init() 体内的局部 func（可读 this 成员）；3. prop get() / mut prop set(v) 体内的局部 func；
//   4. 局部函数自递归；5. 局部函数捕获外层 let 与外层 var（var 被修改时局部函数仍只能直接调用）；6. 局部函数遮蔽外层同名函数；
//   7. 局部 const / let / var 与局部函数共存；8. 局部函数名作为函数值绑定给 let、作为实参传给高阶函数；
//   9. 局部函数体内再声明局部函数（内层可捕获外层局部函数的形参）；10. if / else / for / while / do-while 体里的局部 func；
//   11. try / catch / finally 体与 match case 体里的局部 func；12. 局部泛型函数与局部 const 函数；
//   13. 只捕获外层 let 的局部函数可显式标注函数类型后作为返回值逃逸。
// 官方依据：
//   include/cangjie/Parse/Parser.h:29 ScopeKind::FUNC_BODY —— 局部函数所在作用域；
//   src/Parse/ParseAtom.cpp:1309 ParserImpl::ParseFuncBodyInLambdaExpr（局部 lambda 体）与 src/Parse/ParseAtom.cpp:1706 ParserImpl::ParseFuncBody（普通函数体）都按 FUNC_BODY 解析块；
//   src/Parse/ParseAtom.cpp:374/392/433/459/463/1035/1045 ParseIfExpr / ParseWhileExpr / ParseTryExpr / ParseDoWhileExpr 的分支块同样按 FUNC_BODY 解析；
//   src/Parse/ParseAtom.cpp:397 ParserImpl::ParseExprOrDeclsInMatchCase -> src/Parse/Parser.cpp:452 ParserImpl::ParseExprOrDecl（match case 体里的声明）；
//   src/Parse/Parser.cpp:481 ParserImpl::ParseExpressionOrDeclarations -> src/Parse/Parser.cpp:468 SeeingDecl() 时转 ParseDecl(sk)；
//   src/Parse/ParserImpl.h:258 declHandlerMap 的 {TokenKind::FUNC, &ParserImpl::ParseFuncDecl}（局部 func 与顶层 func 共用同一入口）；
//   src/Parse/ParseDecl.cpp:1486 ParserImpl::ParseFuncDecl -> src/Parse/ParserModifierRules.cpp:260 FUNC_BODY 作用域的 FUNC 修饰符白名单（仅 unsafe / const；public / static / open / operator / override / redef 均非法）；
//   src/Parse/ParserModifierRules.cpp:264-277 DefKind::CLASS / INTERFACE 等只允许 TOPLEVEL（函数体内不能声明 class / interface）；
//   src/Parse/ParseDecl.cpp:899 ParserImpl::CheckDeclarationInScope 与 src/Parse/ParserDiag.cpp:656 ParserImpl::DiagUnexpectedDeclInScope（main 等只允许顶层）；
//   src/Parse/ParseDecl.cpp:1664 ParserImpl::ParsePropMemberBody（prop 的 get / set 体）与 src/Parse/ParseDecl.cpp:1706（init 体）都是 FUNC_BODY；
//   src/Parse/ParseDecl.cpp:1533 ParserImpl::CheckFuncBody（函数体内声明的 func 必须有函数体）；
//   src/CHIR/AST2CHIR/Utils.cpp:224 IsLocalFunc（外层声明是 FuncDecl 即局部函数，src/Parse/ParserImpl.h:981 的 ParseFuncDecl 同一入口）；
//   src/Sema/CheckFunctionLinkage.cpp:67 局部函数一律 internal linkage；src/Sema/Diags.cpp:571 DiagUseClosureCaptureVarAlone（捕获 var 的局部函数不能作为一等公民）。
//   注：局部函数互相递归（前引后声明）在 1.0.5 / 1.1.3 上均报 sema_undeclared_identifier，故不纳入本文件。
// cjc 1.0.5 / 1.1.3 零诊断。

// 形态 1：普通函数体内的局部 func，捕获外层形参。
func localFunctionFormsPlain(seed: Int64): Int64 {
    func localGreet(name: String): String {
        return "hello ${name}#${seed}"
    }
    return localGreet("x").size
}

// 形态 2：init() 体内的局部 func，可读 this 成员。
class LocalFunctionFormsHolder {
    private var value: Int64 = 0

    public init(x: Int64) {
        func scale(a: Int64): Int64 {
            return a * 2 + this.value
        }
        this.value = scale(x)
    }

    public func get(): Int64 {
        return this.value
    }
}

// 形态 3：prop get() 与 mut prop set(v) 体内的局部 func。
class LocalFunctionFormsProps {
    private var store: Int64 = 0

    prop doubled: Int64 {
        get() {
            func twice(a: Int64): Int64 {
                return a * 2 + this.store
            }
            return twice(this.store)
        }
    }

    mut prop value: Int64 {
        set(v) {
            func shift(a: Int64): Int64 {
                return a + 1
            }
            this.store = shift(v)
        }

        get() {
            return this.store
        }
    }
}

// 形态 4：局部函数自递归。
func localFunctionFormsRecursion(): Int64 {
    func factorial(n: Int64): Int64 {
        if (n <= 1) {
            return 1
        }
        return n * factorial(n - 1)
    }
    return factorial(5)
}

// 形态 5：局部函数捕获外层 let 与外层 var（var 被修改，局部函数仍只能直接调用）。
func localFunctionFormsCapture(seed: Int64): Int64 {
    let factor = 3
    var total: Int64 = 0
    func step(a: Int64): Int64 {
        total = total + a
        return total * factor + seed
    }
    step(1)
    return step(2)
}

// 形态 6：局部函数遮蔽外层同名函数。
func localFunctionFormsShadowTarget(): Int64 {
    return 0
}

func localFunctionFormsShadow(): Int64 {
    func localFunctionFormsShadowTarget(): Int64 {
        return 1
    }
    return localFunctionFormsShadowTarget()
}

// 形态 7：局部 const / let / var 与局部函数共存。
func localFunctionFormsWithVars(): Int64 {
    const K: Int64 = 2
    let base = K * 3
    var acc = base + 1
    func useLocal(x: Int64): Int64 {
        return x + acc
    }
    acc = useLocal(1)
    return acc
}

// 形态 8：局部函数名作为函数值绑定给 let、作为实参传给高阶函数。
func localFunctionFormsApply(f: (Int64) -> Int64, v: Int64): Int64 {
    return f(v)
}

func localFunctionFormsHigherOrder(): Int64 {
    var current = 1
    func inc(a: Int64): Int64 {
        return a + 1
    }
    let bound = inc
    current = localFunctionFormsApply(bound, current)
    return localFunctionFormsApply(inc, current)
}

// 形态 9：局部函数体内再声明局部函数，内层捕获外层局部函数的形参。
func localFunctionFormsNested(): Int64 {
    func outerLocal(x: Int64): Int64 {
        func innerLocal(a: Int64): Int64 {
            return a + 1
        }
        return innerLocal(x)
    }
    return outerLocal(1)
}

// 形态 10：if / else / for / while / do-while 体里的局部 func。
func localFunctionFormsInControlFlow(flag: Bool, n: Int64): Int64 {
    var acc: Int64 = 0
    if (flag) {
        func inIf(): Int64 {
            return 1
        }
        acc += inIf()
    } else {
        acc += 2
    }
    for (i in 0..n) {
        func inFor(step: Int64): Int64 {
            return step + 1
        }
        acc += inFor(i)
    }
    while (acc < 100) {
        func inWhile(): Int64 {
            return 10
        }
        acc += inWhile()
    }
    do {
        func inDoWhile(): Int64 {
            return 1
        }
        acc -= inDoWhile()
    } while (acc > 0)
    return acc
}

// 形态 11：try / catch / finally 体与 match case 体里的局部 func。
enum LocalFunctionFormsChoice {
    | First
    | Second
}

func localFunctionFormsInTry(choice: LocalFunctionFormsChoice): Int64 {
    try {
        func inTry(): Int64 {
            return 1
        }
        println(inTry())
    } catch (e: Exception) {
        func inCatch(): Int64 {
            return 2
        }
        println(inCatch())
    } finally {
        func inFinally(): Int64 {
            return 3
        }
        println(inFinally())
    }
    match (choice) {
        case LocalFunctionFormsChoice.First =>
            func inMatchCase(): Int64 {
                return 4
            }
            println(inMatchCase())
        case LocalFunctionFormsChoice.Second =>
            func inMatchCase2(): Int64 {
                return 5
            }
            println(inMatchCase2())
    }
    return 0
}

// 形态 12：局部泛型函数与局部 const 函数。
func localFunctionFormsGenericAndConst(): Int64 {
    func identity<T>(a: T): T {
        return a
    }
    const func constantValue(): Int64 {
        return 7
    }
    return identity(1) + constantValue()
}

// 形态 13：只捕获外层 let 的局部函数，显式标注函数类型后可作为返回值逃逸。
func localFunctionFormsEscapeLetCapture(): (Int64) -> Int64 {
    let num: Int64 = 10

    func add(a: Int64): Int64 {
        return a + num
    }
    return add
}
