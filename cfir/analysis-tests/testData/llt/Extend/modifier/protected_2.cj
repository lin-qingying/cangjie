// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

extend Int64 {
    protected operator func ==(i: String): Bool {
        true
    }
}

main() {
    let i = 0
    if (i == "") {
        0
    } else {
        1
    }
}
