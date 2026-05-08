// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public open func f(): Int32 {
        return 0
    }
    public init() {
    }
}

class B <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>A<!> {
    public init() {
        f()
        var a = {=> super.f}
        func f3(c!: Int32 = f()) {
            f
        }
        a()
    }
    public open override func f(): Int32 {
        return 3
    }
}

class C <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>B<!> {
    public init() {
        super.f()
        var a = {=> f}
        func f3(c!: Int32 = <!TYPE_MISMATCH!>f<!>) {
            this.f
        }
        a()
    }
}

main(): Int64 {
    0
}
