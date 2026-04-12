// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a.b

private interface I1 { // external
    public func f1() {
        1
    } // external
}

internal interface I2 { // external
    public func f1() {
        1
    } // external
}

protected interface I3 { // external
    public func f1() {
        1
    } // external
}

private open class C1 { // external
    public func f1() {
        1
    } // external
    protected func f2() {
        2
    } // external
    internal func f3() {
        3
    } // external
    private func f4() {
        4
    } // internal
}

internal open class C2 { // external
    public func f1() {
        1
    } // external
    protected func f2() {
        2
    } // external
    internal func f3() {
        3
    } // external
    private func f4() {
        4
    } // internal
}

protected open class C3 { // external
    public func f1() {
        1
    } // external
    protected func f2() {
        2
    } // external
    internal func f3() {
        3
    } // external
    private func f4() {
        4
    } // internal
}

public class AAAA <: I1 {}

public class BBBB <: I2 {}

public class CCCC <: I3 {}

public class DDDD <: C1 {}

public class EEEE <: C2 {}

public class FFFF <: C3 {}
