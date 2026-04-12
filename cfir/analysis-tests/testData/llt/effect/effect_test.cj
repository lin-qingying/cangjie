// EXEC: %compiler %enableEH %cmp_opt %f -o %output
// EXEC: %run %set_stdx_path %run_opt %output %run_args 2>&1 | compare %f
import stdx.effect.Command

class Effect <: Command<String> {}

main():Int64 {
    try {
        perform Effect();
    } handle (e: Effect) {
        print("in handle block!")
        resume with "result"
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
