// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func function26(
    param37!: Bool = {
        =>
        let a: Bool = true
        func function41(param42!: Bool = a): Bool {
            param42
        }
        function41();
    }()): Bool {
    param37;
}

main(): Int64 {
    if (function26() != true) {
        return 1;
    }
    return 0;
}
