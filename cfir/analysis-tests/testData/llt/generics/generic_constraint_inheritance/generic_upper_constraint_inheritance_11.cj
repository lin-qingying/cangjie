// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {}

class A<T> where T <: I {}

extend<T> A<T> where T <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>String<!> {}

main() {}
