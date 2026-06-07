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

// FILE: hide_use2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.dep

import ohos.labels.*

@!Hide[isChecked: false]
public open class A {
    @!Hide[isChecked: false]
    public var a = 1
    @!Hide[isChecked: false]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: false]
    public A(@!Hide[isChecked: false] public var v!: Int64 = 1) {}
    @!Hide[isChecked: false]
    public init(a!: String) { v = 2 }
    @!Hide[isChecked: false]
    public func foo(a: Int64) {}
}

@!Hide[isChecked: false]
public enum B {
    | @!Hide[isChecked: false] AA
    | @!Hide[isChecked: false] BB(Int64)

    @!Hide[isChecked: false]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: false]
    public func foo(a: Int64) {}
}

@!Hide[isChecked: false]
extend A {
    @!Hide[isChecked: false]
    public prop c: Int64 { get() {1} }
    @!Hide[isChecked: false]
    public func goo(a: Int64) {}
}

@!Hide[isChecked: false]
public var aaa = 1
@!Hide[isChecked: false]
public func goo(a: Int64) {}

public enum E {
    | @!Hide[isChecked: false] AA
    | @!Hide[isChecked: false] BB(Int64)

    @!Hide[isChecked: false]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: false]
    public func foo(a: Int64) {}
}

// FILE: call_hide2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import ohos.dep.*
import ohos.dep

class D<T> <: A/*ok*/ where T <: A/*ok*/ {
    func xxx<R>(d: A/*ok*/): A/*ok*/ where R <: A/*ok*/ {
        var a: A/*ok*/ = A()/*ok*/
        B/*ok*/.AA
        B/*ok*/.BB(1)
        E.AA/*ok*/
        E.BB(1)/*ok*/
        goo(1)/*ok*/
        var b: D<A/*ok*/>
        var c: dep.A/*ok*/
        a is A/*ok*/
        a as A/*ok*/
        aaa/*ok*/ // global var
        a
    }
}

main() {}
