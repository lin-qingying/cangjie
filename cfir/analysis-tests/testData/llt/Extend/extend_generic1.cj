// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

interface EQ {}

class S <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add() {}
}

main() {
    let a: A<S> = A<S>()
    a.add()
    let b: A<Int32> = A<Int32>()
    b.<!NOT_MEMBER_OF!>add<!>()
    0
}
