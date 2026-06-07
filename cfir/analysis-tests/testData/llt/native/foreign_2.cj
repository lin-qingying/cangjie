// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {}

open class A<T> {}

class B<T> <: A<T> & I<T> {}

foreign func assert(input: <!INVALID_CFUNC_PARAMETER_TYPE!>B<Int32><!>): Int32
