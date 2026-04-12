// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output
// EXEC-0: %run %run_opt %output %run_args

interface Boo {
    mut prop m: Int32 {
        get() {
            return 1
        }
        set(input) {
            ()
        }
    }
}

class Coo <: Boo {
    public mut override prop m: Int32 {
        get() {
            return (10)
        }
        set(input) {
            ()
        }
    }
}

main() {
    return Int64(Coo().m - 10)
}
