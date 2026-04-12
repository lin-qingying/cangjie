// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package ohos.dep

import ohos.labels.*

@!Hide
open public class A {
    @!Hide
    public var a = 1
    @!Hide
    public prop b: Int64 { get() {1} }
    @!Hide
    public A(@!Hide public var v!: Int64 = 1) {}
    @!Hide
    public init(a!: String) { v = 2 }
    @!Hide
    public func foo(a: Int64) {}
}

@!Hide
public enum B {
    | @!Hide AA
    | @!Hide BB(Int64)

    @!Hide
    public prop b: Int64 { get() {1} }
    @!Hide
    public func foo(a: Int64) {}
}

@!Hide
extend A {
    @!Hide
    public prop c: Int64 { get() {1} }
    @!Hide
    public func goo(a: Int64) {}
}

@!Hide
public var aaa = 1
@!Hide
public func goo(a: Int64) {}

public enum E {
    | @!Hide AA
    | @!Hide BB(Int64)

    @!Hide
    public prop b: Int64 { get() {1} }
    @!Hide
    public func foo(a: Int64) {}
}
