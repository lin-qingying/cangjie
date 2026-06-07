// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A<T> <: B<C<T>> {}

interface B<T> where T <: A<T> {}

open class C<T> <: <!sema_generic_infinite_instantiation!>A<!><C<T>> & B<C<T>> {}

main() {
    return 0
}
