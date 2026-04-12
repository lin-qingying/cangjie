// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Coo {
    public mut prop d: Int32 {
        get() {
            d = 2
            return 12
        }
        set(input) {
            ()
        }
    }
}

main(): Int64 {
    if (Coo().d == 12) {
        return 0
    }
    return 1
}
