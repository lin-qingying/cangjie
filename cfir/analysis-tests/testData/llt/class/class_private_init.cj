// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f %error_count_limit_all 2>&1 | compare %f

class A {
    private init() {}
    protected init(a: Int64) {}
    public init(b: Int64, c: Int64) {}
    public func m() {
        var a = A() // ok
        var b = A(1) // ok
        var c = A(1, 1) // ok
    }
    public static func p() {
        var a = A() // ok
        var b = A(1) // ok
        var c = A(1, 1) // ok
    }
}

class B {
    public func m() {
        var a = A<!NO_VALUE_FOR_PARAMETER!>()<!>
    }
}

class C <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>A<!> {
    public init() {
        super<!NO_VALUE_FOR_PARAMETER!>()<!>
    }
    public init(a: Int64) {
        super(a)
    }
    public func k() {
        var a = A<!NO_VALUE_FOR_PARAMETER!>()<!>
        var b = A(1) // OK
        var c = A(1, 1) // OK
    }
    public static func j() {
        var a = A<!NO_VALUE_FOR_PARAMETER!>()<!>
        var b = A(1) // OK
        var c = A(1, 1) // OK
    }
}

main() {
    var a = A(1)
    var b = A<!NO_VALUE_FOR_PARAMETER!>()<!>
    var c = A(1, 1) // OK
    return 0
}
