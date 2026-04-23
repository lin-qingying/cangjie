// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A<F, H> {}

extend<F, H> A<F, H> {
    static func f3(a: F, b: H) {
    }
}

class B<K> <: A<K, K> {}

main(): Unit {
    <!GENERIC_TYPE_INCONSISTENT!>B<!>.f3(Int64(1), Int32(1))
}
