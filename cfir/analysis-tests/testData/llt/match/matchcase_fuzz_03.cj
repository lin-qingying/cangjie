// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    var var7: Int16 = Int16(-6);

    match (Int16(-6)) {
        case -121 => 55
        case caseVar24 => 55
        case _ => (65.0, 15, (-107), Float32(-84.0), Int32(-16))[2]
    }
    return 0;
}
