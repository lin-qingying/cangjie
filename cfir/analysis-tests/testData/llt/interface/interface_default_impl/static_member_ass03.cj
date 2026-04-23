// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func f1(): Unit
    static func f2(): Unit {
        f1()
    }
    static func f3(): Unit {
        f2()
    }
}

interface I2 {
    static func f4(): Unit {
        <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I.f2()<!> // error
    }
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I.f3()<!> // error
}
