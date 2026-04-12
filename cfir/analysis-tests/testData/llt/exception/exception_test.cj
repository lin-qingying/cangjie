// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import std.core.*

main(): Int64 {
    try {
        throw gumentException();
    } catch (e: IllegalArgumentException) {
        print("in catch block!")
    } finally {
        print("in finally block!")
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
