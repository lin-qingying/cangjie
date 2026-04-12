// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A {
    static var b: Int64 = 1
    static var a: Int64
    static init() {
        a = b
        b = a
    }
}

struct B {
    static var a: Int64
    static var b: Int64
    static init() {
        b = 1
        a = b
        b = a
    }
}

class C {
    static prop a: Int64 {
        get() {
            b
        }
    }
    static var b: Int64
    static init() {
        b = 5
        b = a
    }
}

main() {
    return A.b - A.a + B.a - B.b + C.a - C.b
}
