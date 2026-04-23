// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {}

class A<M, N> {}

extend<T> A<T, String> <: <!EXTEND_DUPLICATE_INTERFACE!>I<String><!> {}

extend<Q, R> A<Q, R> <: I<R> {}

main() {}
