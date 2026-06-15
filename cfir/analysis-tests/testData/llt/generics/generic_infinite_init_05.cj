// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface A<T> <: B<C<T>> {}

interface B<T> {}

open class C<T> <: <!GENERIC_INFINITE_INSTANTIATION!>A<!><C<T>> {}

main(): Int64 {
    return 0
}
