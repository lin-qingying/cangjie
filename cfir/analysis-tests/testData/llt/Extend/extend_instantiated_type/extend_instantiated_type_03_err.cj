// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class X {}

class Y {}

class A<T, K> where K <: ToString {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend<T> A<Int64, T> <: ToString {
    func test() {
        println("A int64")
    }
}<!>

extend A<Int64, Int64> {
    public func toString() { // invisible for extend ToString
        "A<Int64, Int64>"
    }
}

main() {
    let a1 = A<Int64, Rune>()
    let t: ToString = a1
    if (t.toString() != "A<Int64, Int64>") {
        return 1
    }
    return 0
}
