// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    if (case02()(1) != ()) {
        return 2
    }
    return 0
}

func case02() {
    var a: Int32 = 1
    var c: (Int32) -> Unit = {x: Int32 => x}
    return c
}
