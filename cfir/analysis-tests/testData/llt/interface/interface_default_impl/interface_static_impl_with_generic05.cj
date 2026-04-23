// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {
    static func f1(a: T): Unit

    static func f3(a: T): Unit {
        f1(a)
    }
}

interface I1 <: I<Int64> & I<String> {
    static func f1(a: Int64): Unit {
        f1("1")
    }
    static func f1(a: String): Unit {
        f2()
    }
    static func f2(): Unit
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I1.f3(1)<!>
}
