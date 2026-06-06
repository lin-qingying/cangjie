// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt %n.cj pkg.%middle 2>&1 | compare %f

internal import <!package_search_error!>pkg<!>.*

main() {
    let a = C()
    a.f()
    a.p = 0
    a.p
    return 0
}
