// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: p1.cj p2.cj
// EXEC:%compiler %cmp_opt %compile_lib_opt p1.cj -o p1.%middle
// EXEC:%compiler %cmp_opt %compile_lib_opt p1.%middle p2.cj -o p2.%middle
// EXEC:%compiler %cmp_opt p2.%middle p1.%middle main.cj -o %output
// EXEC:%run %run_opt %output %run_args

internal import p1.*
internal import p2.*

main() {
    let b = Bar()
    b.g()
    return 0
}
