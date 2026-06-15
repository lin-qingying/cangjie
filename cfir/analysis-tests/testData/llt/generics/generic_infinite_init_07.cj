// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface B<T> where T <: A<T> {}

// This must defined before 'class C'
open class CI<T> <: A<C<T>> {}

interface A<T> <: B<C<T>> {}

open class C<T> <: <!GENERIC_INFINITE_INSTANTIATION!>A<!><C<T>> {}

main() {
    return 0
}
