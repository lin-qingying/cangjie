// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {
    static func f(): Unit {}
}

interface I2 <: I1 {
    static func f1(): Unit
    static func f(): Unit {
        f1()
    }
}

interface I3 <: I2 {
    static func f1(): Unit {
        println("I3 f1")
    }
}

func f<T>(): Unit where T <: I1 {
    T.f()
}

main() {
    f<I3>()
}
