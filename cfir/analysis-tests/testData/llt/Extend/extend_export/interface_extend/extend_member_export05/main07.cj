// FILE: testa.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

private interface I0 {
    public func f0(): Unit
}

public class Foo<T> {}

// The extension is exported.
extend<T> Foo<T> <: I0 {
    public func f0(): Unit {}
}

// FILE: testb4.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b.c

import a.Foo

private interface I1 {
    public func f1(): Unit
}

internal interface I2 {
    public func f2(): Unit
}

protected interface I3 {
    public func f3(): Unit
}

public interface I4 {
    public func f4(): Unit
}

extend<T> Foo<T> <: I1 & I2 & I3 {
    public func f1() {} // not export
    public func f2() {} // export
    public func f3() {} // export
    public func f4() {} // not export
}

// FILE: main07.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

package d

import a.*
import b.c.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().<!NOT_MEMBER_OF!>f1<!>() // error
    Foo<Int64>().<!NOT_MEMBER_OF!>f2<!>() // error
    Foo<Int64>().<!NOT_MEMBER_OF!>f3<!>() // error
}
