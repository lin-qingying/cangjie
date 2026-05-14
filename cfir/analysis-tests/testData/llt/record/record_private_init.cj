// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f
struct A {
    private init() {}
    public init(b: Int64, c: Int64) {}
    public func m() {
        var a = A() // ok
        var c = A(1, 1) // ok
    }
    public static func f() {
        var a = A() // ok
        var c = A(1, 1) // ok
    }
}

struct B {
    public func m() {
        var a = A<!NO_VALUE_FOR_PARAMETER!>()<!>
        var c = A(1, 1) // OK
    }
    public static func f() {
        var a = A<!NO_VALUE_FOR_PARAMETER!>()<!>
        var c = A(1, 1) // OK
    }
}

main() {
    var a = A<!NO_VALUE_FOR_PARAMETER!>()<!>
    var c = A(1, 1) // OK
    return 0
}
