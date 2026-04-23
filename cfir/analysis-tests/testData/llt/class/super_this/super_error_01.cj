// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Base {
    public var b: Int32 = 6
    public init(x: Int32) {
        this.b = 5
    }
}

class Test<T> <: Base {
    public var a: Int32 = 3
    <!EXPLICIT_SUPER_CALL_REQUIRED!>public Test()<!> {
        this.a == a
    }

    <!EXPLICIT_SUPER_CALL_REQUIRED!>public init(x: Int32)<!> {
        super.b = x
        this.a = x
    }
}

main(): Int64 {
    var test = Test<Int32>()
    var test2 = Test<Int32>(8)
    if (test.b == 5 && test.a == 3 && test2.a == 8 && test2.b == 8) {
        return 0
    }
    return 1
}
