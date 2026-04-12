// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

extend String {
    protected prop flag: Bool {
        get() {
            true
        }
    }
}

main() {
    let s = ""
    if (s.flag) {
        0
    } else {
        1
    }
}
