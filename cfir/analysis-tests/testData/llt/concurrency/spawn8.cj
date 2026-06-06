// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

<!INHERIT_THREAD_CONTEXT_INVALID!>class MyCustomThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
}<!>

<!INHERIT_THREAD_CONTEXT_INVALID!>interface MyInterfaceThreadContext <: ThreadContext {
    func custom(): Unit
}<!>

<!INHERIT_THREAD_CONTEXT_INVALID!>class MyExtendThreadContext {}<!>

extend MyExtendThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
}

main(): Int64 {
    let fut = spawn (<!SPAWN_ARG_INVALID!>MyCustomThreadContext()<!>) {}
    return 0
}
