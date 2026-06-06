// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %pkg_opt %compile_lib_opt pkg 2>&1 | compare %f

internal import <!package_search_error!>pkg<!>.*

main(): Int64 {
    return 0
}
