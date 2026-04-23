// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    <!CLASS_UNINITIALIZED_FIELD!>public init() {}<!>

    public var a: UInt
    public mut static prop v: UInt64 {
        get() {
            ()
            UInt64(123)
            b
        }
        set(Value) {
            Int64(40)
            ()
            b = Value
        }
    }
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>static var b: UInt64<!>
    static init() {}
    // Should not report error bellow
    func goo() {
        get()
    }
    func get() {
        a
        this.a
    }
}

main(): Int64 {
    1
}
