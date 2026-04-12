// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

public class B<T> {}

extend<R> B<R> {
    public func call() {
        this
    }
}

public abstract class A<U> {
    public func getB() {
        return B<Int>()
    }
}

public class C <: A<Any> {
    public C() {
        getB().call().call()
    }
}

main() {
}
