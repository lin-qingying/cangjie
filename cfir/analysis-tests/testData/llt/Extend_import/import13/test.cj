// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg0.cj pkg1.cj 
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg0.cj -o pkg0.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg0.%middle pkg1.cj -o pkg1.%middle
// (CJNATIVE) EXEC: %compiler %cmp_opt pkg1.%middle pkg0.%middle %f -o %output
// (not CJNATIVE) EXEC: %compiler %cmp_opt pkg0.%middle pkg1.%middle %f -o %output

internal import pkg1.*

main(): Int64 {
    let conv = Conv2d()
    let output = conv(1.0)
    return 0
}
