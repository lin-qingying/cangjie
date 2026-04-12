// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args | compare %f
// ASSERT: scan 3

main(): Int64 {
    var a = 1
    var b = 2
    var c = 3
    var res = match (c) {
        case 1 => 0
        case b => //EXPECTED regex 'b' has been defined in the outside of match expression, are you sure using 'b' as a variable pattern here?
            print(b.toString())
            1
        case _ => 2
    }
    if (res == 1) {
        return 0
    }
    return 1
}
