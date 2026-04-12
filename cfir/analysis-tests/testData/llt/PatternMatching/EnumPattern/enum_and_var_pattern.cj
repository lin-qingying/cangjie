// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

let f1 = {
    x => match (x) {
        case Some(v) => v + 1
        case NOne => 0
    }
}

main() {
    println(f1(Some(3)))
    println(f1(None))
    0
}
