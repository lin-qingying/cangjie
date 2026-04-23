// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public interface I1 {
    static func foo1(): Unit
}

public interface I2 <: I1 {
    static func foo2() {
        foo1()
    }
}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.foo2()<!>
    return 1
}
