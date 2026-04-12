// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A {
    static func a(): Unit {}
    static func i(input!: () -> Unit): Unit {
        input()
    }
    static func test(): Unit {
        i(input: a)
    }
}

class Test <: A {}

class B {
    public static var a = {=>}
    public static func i(input!: () -> Unit = a): Unit {
        input()
    }
    public static func test(): Unit {
        i()
    }
}

abstract class C {
    public static var a = {=>}
    public static func i(input!: () -> Unit = a): Unit {
        input()
    }
    public static func test(): Unit {
        i()
    }
}

main() {
    Test.a()
    Test.test()
    B.a()
    B.test()
    C.a()
    C.test()
    return 0
}
