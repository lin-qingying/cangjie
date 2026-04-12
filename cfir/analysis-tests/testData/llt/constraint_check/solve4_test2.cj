// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface X<T> where T <: Y<T> {}

interface Y<T> where T <: X<T> {}

interface C <: X<C> & Y<C> {}

main() {
    return 0
}
