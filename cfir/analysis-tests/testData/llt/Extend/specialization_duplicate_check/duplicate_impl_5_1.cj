// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1<T> {}

interface I2<T> <: I1<T> {}

class A<T> {}

extend A<String> <: I2<String> {}

extend<T> A<T> <: I1<T> {}

main() {}
