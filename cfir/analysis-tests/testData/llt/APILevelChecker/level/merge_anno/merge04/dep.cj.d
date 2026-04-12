// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package dep

public interface I {
    func foo(): Unit
}

public class A {}

extend A <: I {
    @!APILevel[since: "12"]
    public func foo(): Unit
}

extend ?Int64 <: I {
    @!APILevel[since: "12"]
    public func foo(): Unit
}

extend Int64 <: I {
    @!APILevel[since: "12"]
    public func foo(): Unit
}
