// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

class A<X> {}

extend<V> A<V> <: I where V <: I {}

func f<<!ONLY_ONE_CLASS_BOUND_ALLOWED!>T<!>>() where T <: A<T> & A<Int64> {}

main() {}
