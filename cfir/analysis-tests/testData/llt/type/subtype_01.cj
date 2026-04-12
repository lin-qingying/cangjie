// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

interface I {}

class A<T> <: I where T <: A<T> & A<Int64> {}

main() {}

// ASSERT: regex generics type arguments do not match the constraint of 'Class-A<Generics-T>'
