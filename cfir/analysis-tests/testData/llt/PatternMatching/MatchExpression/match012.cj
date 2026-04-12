// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var result = match (50) {
        case 60 => "pass"
        case 70 | 80 =>
            "yes";
            var a = 1;
            "good"
        case other =>
            "yes";
            var test: (Int64) -> String = {x: Int64 => x.toString()};
            test(other - 1)
    }
    if (result == "49") {
        return 0
    }
    return 1
}
