// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var result = match {
        case false => "pass"
        case 2 < 1 =>
            "yes";
            var a = 1;
            "good"
        case _ =>
            "yes";
            var test: (Int64) -> String = {x: Int64 => x.toString()};
            test(50 - 1)
    }
    if (result == "49") {
        return 0
    }
    return 1
}
