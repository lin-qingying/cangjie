// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface EQ {}

class S <: EQ {}

class A<T> {}

extend<T> A<T> where T <: EQ {
    static func add() {}
}

main() {
    A<S>.add()
    A<Int32>.<!NOT_MEMBER_OF!>add<!>()
    0
}
