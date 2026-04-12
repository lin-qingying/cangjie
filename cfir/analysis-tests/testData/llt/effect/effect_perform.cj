// EXEC: %compiler %enableEH %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %set_stdx_path %run_opt %output

import stdx.effect.Command
class Eff <: Command<String> {}

main():Int64 {
    try {
            perform Eff();
            var a = 1
    } handle (e: Eff) {
            print("in handle block!")
            resume with "Result"
    } finally {
        print("in finally block!")
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
