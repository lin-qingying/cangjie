// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0<T> {}

class A<Q> <: I0<Q> & I0<Int64> {}

main() {
    var b = <!SUPER_TYPES_DUPLICATE!>A<Int64><!>()
    return 0
}
