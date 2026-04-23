// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface EQ {}

class S <: EQ {}

class A<T> {
    static func ad(a: T) {}
}

extend<T> A<T> where T <: EQ {
    static func add(a: T) {}
}

main() {
    let s: S = S()
    A.add(s)
    A.ad(s)
    0
}
