// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt --diagnostic-format=json %f 2>&1 | compare %f

main(): Int64 {
    var a: Int64
    func b() {
        do {
            a = 1 // captured before initialization
        } while (a == 0)
        return a
    }

    b()
    return 0
}

// ASSERT: scan sema_capture_before_initialization
