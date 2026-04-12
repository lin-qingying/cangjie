// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt --module-name="ohos" %f -o %n.%suffix

package ohos.base

class MainThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
    func getSchedulerHandle(): CPointer<Unit> {
        CPointer<Unit>()
    } // inner implement
}

type NewName = MainThreadContext

main(): Int64 {
    let fut = spawn (MainThreadContext()) {
        println("hello world spawn9")
    }

    let fut1: Future<Unit> = spawn (MainThreadContext()) {
        println("hello world spawn9")
    }

    let fut2: Future<Unit> = spawn (NewName()) {
        println("hello world spawn9")
    }
    return 0
}
