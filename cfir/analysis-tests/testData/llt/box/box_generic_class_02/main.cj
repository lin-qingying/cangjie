// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// (CJNATIVE) EXEC: %compiler %cmp_opt %compile_shared_lib_opt -p pkg
// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %main -L. -lpkg

// (not CJNATIVE) EXEC: %compiler %cmp_opt %compile_shared_lib_opt -p pkg -o pkg.%lib_extension
// (not CJNATIVE) EXEC: %compiler %cmp_opt %f -o %main pkg.%lib_extension

// EXEC: %run %run_opt %main

import pkg.*

main() {
    var res = bar()
    if (res is B) {
        0
    } else {
        1
    }
}
