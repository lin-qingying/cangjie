// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    var a = 9223372036854775807..9223372036854775807 : -9223372036854775808
    for (i in a) {
        print(i.toString())
    }
    if (a.isEmpty()) {
        return 0
    }
    return 2
}
