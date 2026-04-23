// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A <: Resource {
    public func isClosed() {
        true
    }
    public func close() {}
}

func f() {}

func f(_: Unit) {}

func g<T>(_: Unit) {}

main() {
    f(try (x = A()) {} finally {
        let _ = <!TOO_MANY_ARGUMENTS!>A(1)<!>
    }) |> g<Unit>
}
