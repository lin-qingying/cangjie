// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package dep1

import ohos.labels.APILevel

@!APILevel[since: "1"]
public func foo(): Int64
@!APILevel[since: "2"]
public func foo(a: Array<Int64>): Int64
@!APILevel[since: "3"]
public func foo(a: Array<String>): Int64

@!APILevel[since: "4"]
class A {
    @!APILevel[since: "5"]
    init() {}
    @!APILevel[since: "6"]
    init(a: Int64) {}
    @!APILevel[since: "7"]
    init(a: String) {}
    @!APILevel[since: "8"]
    public func foo(): Int64
    @!APILevel[since: "9"]
    public func foo(@!APILevel[since: "10"]a: Array<Int64>): Int64
    @!APILevel[since: "11"]
    public func foo(@!APILevel[since: "12"]a: Array<String>): Int64

    @!APILevel[since: "13"]
    public var a = 0
    @!APILevel[since: "14"]
    public prop b: Int64
}

@!APILevel[since: "15"]
struct B {
    @!APILevel[since: "16"]
    init() {}
    @!APILevel[since: "17"]
    init(a: Int64) {}
    @!APILevel[since: "18"]
    init(a: String) {}
    @!APILevel[since: "19"]
    public func foo(): Int64
    @!APILevel[since: "20"]
    public func foo(@!APILevel[since: "21"]a: Array<Int64>): Int64
    @!APILevel[since: "22"]
    public func foo(@!APILevel[since: "23"]a: Array<String>): Int64

    @!APILevel[since: "24"]
    public var a = 0
    @!APILevel[since: "25"]
    public prop b: Int64
}

@!APILevel[since: "26"]
enum C {
    @!APILevel[since: "27"]A | @!APILevel[since: "28"]B(Int64)

    @!APILevel[since: "29"]
    public func foo(): Int64
    @!APILevel[since: "30"]
    public func foo(a: Array<Int64>): Int64
    @!APILevel[since: "31"]
    public func foo(a: Array<String>): Int64
}

interface D {
    @!APILevel[since: "32"]
    func foo(): Int64
    @!APILevel[since: "33"]
    func foo(@!APILevel[since: "34"]a: Array<Int64>): Int64
    @!APILevel[since: "35"]
    func foo(@!APILevel[since: "36"]a: Array<String>): Int64
}
