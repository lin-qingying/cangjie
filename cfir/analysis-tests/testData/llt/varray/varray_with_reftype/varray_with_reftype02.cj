// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan 'Struct-S1' directly or indirectly contains an unsupported type
// ASSERT: scan 'Struct-S2<Class-A>' directly or indirectly contains an unsupported type
// ASSERT: scan 'Struct-S2<Interface-I>' directly or indirectly contains an unsupported type
// ASSERT: scan 'Interface-I' directly or indirectly contains an unsupported type
// ASSERT: scan 'Struct-S2<Class-A>' directly or indirectly contains an unsupported type
// ASSERT: scan 'Enum-E' directly or indirectly contains an unsupported type
// ASSERT: scan 'Struct-S4' directly or indirectly contains an unsupported type
// ASSERT: scan 'Struct-String' directly or indirectly contains an unsupported type

interface I {}

class A <: I {}

struct S1 {
    var a: A = A()
}

struct S2<T> {
    var a: T
    init(a1: T) {
        a = a1
    }
}

struct S2_1<T> {
    var a: Int64 = 0
}

struct S3 {
    prop a: A {
        get() {
            A()
        }
    }
}

struct S4 {
    var a: A
    init(a1: A) {
        a = a1
    }
}

struct S5 {
    static var a: A = A()
}

enum E {
    | C
    | D
}

main() {
    var a: VArray<S1, $1> = [S1()] // error
    var b: VArray<S2<A>, $1> = [S2(A())] // error
    var c: VArray<S2<I>, $1> = [S2(A())] // error
    var c0 = VArray<I, $1>(repeat: A()) // error
    var c1 = VArray<I, $1>({i => A()}) // error
    var c2: VArray<I, $1> = [A()] // error
    var c3: VArray<S2<A>, $1> = [S2(A())] // error
    var d: VArray<S3, $1> = [S3()] // pass
    var e: VArray<S4, $1> // error
    var f: VArray<String, $1> = ["123"] // error
    var g = VArray<S5, $1>(repeat: S5()) // pass
    var h: VArray<E, $3> = [E.C, E.D, E.D] // error
    0
}
