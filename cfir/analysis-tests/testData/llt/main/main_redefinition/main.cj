// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/*.cj -o pkg.%middle
// EXEC: %compiler --verbose %cmp_opt pkg.%middle %f -o %output

internal import pkg.*

main() {
    PrintHello()
    return 0
}
