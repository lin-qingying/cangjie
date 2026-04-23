// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class I1<F, K> {
    static func f3(a: F, b: K) {
        println("123")
    }
}

class I2<K> <: I1<K, K> {}

main(): Unit {
    <!GENERIC_TYPE_INCONSISTENT!>I2<!>.f3(Int64(1), Int32(1))
}
