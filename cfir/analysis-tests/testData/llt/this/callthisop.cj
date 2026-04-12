// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

class Test {
    operator func +(rhs: Test): This {
        return this
    }
    operator func -(): This {
        this
    }

    func f9(rhs: Test): This {
        this + rhs
    }

    func f10(): This {
        return -this
    }
}

main() {
}
