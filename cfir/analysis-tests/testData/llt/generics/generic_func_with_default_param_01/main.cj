// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg0 pkg1
// EXEC: %compiler %cmp_opt %compile_lib_opt -p pkg0 -o pkg0.%lib_extension
// EXEC: %compiler %cmp_opt %compile_lib_opt -p pkg1 -o pkg1.%lib_extension
// (CJNATIVE) EXEC: %compiler %cmp_opt %f pkg1.%lib_extension pkg0.%lib_extension -o %output
// (not CJNATIVE) EXEC: %compiler %cmp_opt %f pkg0.%lib_extension pkg1.%lib_extension -o %output
// EXEC: %run %run_opt %output

package test

import test.pkg1.*

main() {
    bar()
}
