// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    public static let opt: Option<Bool> = true as Bool
}

main(): Int64 {
    match (A.opt) {
        case Some(v) =>
            if (v) {
                return 0
            } else {
                return 1
            }
        case None => return 1
    }
}
