// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f(b: Byte) {
    match (b) {
        case 97 => "A"
        case b'b' => "B"
        case _ => "?"
    }
}

main() {
    if (f(b'a') != "A") {
        1
    } else if (f(b'b') != "B") {
        2
    } else {
        0
    }
}
