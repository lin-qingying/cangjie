// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

var a = 1
public var b = a
public var d = 2
var c = d

main() {
    if (a == 1 && b == 1 && d == 2 && c == 2) {
        return 0
    } else {
        return 1
    }
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
