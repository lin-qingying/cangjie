// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: nn
// EXEC: %compiler %cmp_opt %compile_lib_opt nn/nn.cj -o nn/nn.%middle
// EXEC: %compiler %cmp_opt nn/nn.%middle %n.cj --import-path nn -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import nn.*

main(): Int64 {
    A<Int64>()
    return 0
}
