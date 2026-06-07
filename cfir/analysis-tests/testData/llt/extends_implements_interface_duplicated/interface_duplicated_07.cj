// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

interface I2<Q> <: I1<Q> & I0<Int64> {}

interface I3<P> <: I2<Int32> {}

main() {
    var i2: I2<Int64>
    var i1: <!SUPER_TYPES_DUPLICATE!>I1<Rune><!>
    return 0
}
