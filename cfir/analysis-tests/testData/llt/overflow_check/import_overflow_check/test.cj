// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: p1
// EXEC: %compiler %compile_lib_opt p1/pkg_1.cj -o p1.%middle
// EXEC: %compiler p1.%middle test.cj -o %output
// ASSERT: regex-not error

internal import p1.*

main() {
    var varCO2 = fun_1(1, 1)

    return 0
}
