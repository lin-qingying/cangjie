// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: typealias36_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt typealias36_dep.cj -o pkga.%middle
// EXEC-PIPE-0: %compiler %cmp_opt pkga.%middle %f -o %output 2>&1 | compare %f

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// EXEC: %run %run_opt %output %run_args

import pkga

main() {
    var a = pkga.AA<Int64>()
    0
}
