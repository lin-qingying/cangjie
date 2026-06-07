// FILE: testa.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

public class Foo {}

// FILE: testb.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

import a.Foo

private interface I1 {
    public func f1(): Unit
}

internal interface I2 {
    public func f1(a: Int64): Unit
    public func f2(): Unit
}

extend Foo <: I1 & I2 {
    public func f1() {}
    public func f1(a: Int64) {}
    public func f2(): Unit {}
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b.c

import a.*
import b.*

main() {
    <!NO_VALUE_FOR_PARAMETER!>Foo().f1()<!> // error
    Foo().f1(1) // ok
    Foo().f2() // ok
}
