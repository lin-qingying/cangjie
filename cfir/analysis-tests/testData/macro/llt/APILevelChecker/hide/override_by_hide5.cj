// FILE: hide.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.labels

@Annotation
protected class Hide {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_use1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.dep

import ohos.labels.*

@!Hide[isChecked: true]
public open class A {
    @!Hide[isChecked: true]
    public var a = 1
    @!Hide[isChecked: true]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public A(public var v!: Int64 = 1) {}
    @!Hide[isChecked: true]
    public init(a!: String) { v = 2 }
    @!Hide[isChecked: true]
    public func foo(a: Int64) {}
}

@!Hide[isChecked: true]
public enum B {
    | @!Hide[isChecked: true] AA
    | @!Hide[isChecked: true] BB(Int64)

    @!Hide[isChecked: true]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public func foo(a: Int64) {}
}

@!Hide[isChecked: true]
extend A {
    @!Hide[isChecked: true]
    public prop c: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public func goo(a: Int64) {}
}

@!Hide[isChecked: true]
public var aaa = 1
@!Hide[isChecked: true]
public func goo(a: Int64) {}

@!Hide[isChecked: true]
public class C<T> {}

public class D<T> {}

public enum E {
    | @!Hide[isChecked: true] AA
    | @!Hide[isChecked: true] BB(Int64)

    @!Hide[isChecked: true]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public func foo(a: Int64) {}
}

// FILE: override_by_hide5.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package ohos.dep

import ohos.labels.*

public interface I {
    func foo() {}
    @!Hide
    func hoo() {}
    @!Hide[isChecked: false]
    func goo() {}
    @!Hide[isChecked: true]
    func koo() {}
}

public class A <: I {
    public func foo() {}
    public func hoo() {} // error
    public func goo() {} // error
    public func koo() {} // error
}

public class B <: I {
    @!Hide
    public func <!HIDE_MISSING_HIDE!>foo<!>() {} // error
    @!Hide
    public func hoo() {}
    @!Hide
    public func goo() {}
    @!Hide
    public func <!HIDE_DIFF_PARAM!>koo<!>() {} // error
}

public class C <: I {
    @!Hide[isChecked: false]
    public func foo() {} // error
    @!Hide[isChecked: false]
    public func hoo() {}
    @!Hide[isChecked: false]
    public func goo() {}
    @!Hide[isChecked: false]
    public func <!HIDE_DIFF_PARAM!>koo<!>() {} // error
}

public class D <: I {
    @!Hide[isChecked: true]
    public func foo() {} // error
    @!Hide[isChecked: true]
    public func <!HIDE_DIFF_PARAM!>hoo<!>() {} // error
    @!Hide[isChecked: true]
    public func <!HIDE_DIFF_PARAM!>goo<!>() {} // error
    @!Hide[isChecked: true]
    public func koo() {}
}
