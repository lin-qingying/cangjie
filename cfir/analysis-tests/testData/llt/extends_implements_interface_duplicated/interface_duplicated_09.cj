// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0<T> {}

interface I1<T> <: I0<T> {}

interface I2<T> <: I1<T> {}

open class A<T> <: I2<T> {}

open class B<P> <: A<P> {}

class D<T> <: A<T> & I0<Int32> & I0<Int64> {}

main() {
    var <!chir_dce_unused_variable!>b<!> = B<Int64>()
    var <!chir_dce_unused_variable!>d1<!> = D<Int64>()
    var <!chir_dce_unused_variable!>d2<!> = D<Int32>()
    return 0
}
