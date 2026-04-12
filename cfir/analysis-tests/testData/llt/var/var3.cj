// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

main(): Int64 {
    let val = {
    var a:Bool = false
        a
        return 1;
    }
    if (!val) {
        return 1
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
