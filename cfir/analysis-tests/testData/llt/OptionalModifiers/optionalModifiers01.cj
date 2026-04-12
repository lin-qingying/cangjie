// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class Father {
    protected open func f(): This {
        this
    }
    public open func f(a: Int64) {
        a
    }
    static func g(a: Int32) {
        a
    }
}

class Child1 <: Father {
    protected override func f() {
        this
    }
    public override func f(a: Int64) {
        a + 1
    }
    static redef func g(a: Int32) {
        a + 1
    }
}

class Child2 <: Father {
    public func f() {
        this
    }
    public func f(a: Int64) {
        a + 1
    }
    static func g(a: Int32) {
        a + 1
    }
}

main() {
    return 0
}
