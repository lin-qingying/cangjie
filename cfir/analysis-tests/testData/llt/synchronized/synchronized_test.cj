// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %frontendCompiler %typecheck %f 2>&1 | compare %f
// ASSERT: regex-not core dump
// ASSERT: regex-not internal Compiler Error: exist invalid sema ty

internal import std.sync.*

var arr: Array<Int64> = Array<Int64>(10, repeat: 10)
let lock = ReentrantMutex()

func foo() {
    synchronized(lock) {
        for (i in arr) {
            println(i)
        }
    }
}

main() {
    spawn {
        foo()
    }
    return 0
}
