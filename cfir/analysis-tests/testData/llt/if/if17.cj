// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class C {
    C(i: Int32) {
        if (i > 0) {
            1
        } else {
            ()
        }
    }

    init(i: Int64) {
        if (i > 0) {
            1
        } else {
            ()
        }
    }
}

main() {
}
