// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class AAA <: Resource {
    public func close(a: Bool): Unit {}
    public func close(): Unit {}
    public func isClosed(): Bool {
        return true
    }
}

class BBB <: AAA {
    public func close(a: Int64): Unit {}
}

main() {
    try (aaa = AAA()) {
        println("try")
    } finally {
        println("finally")
    }
    try (bbb = BBB()) {
        println("try")
    } finally {
        println("finally")
    }
}
