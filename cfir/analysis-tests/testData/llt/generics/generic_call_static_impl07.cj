// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

public interface I<T> {
    static func test(): Bool
}

public open class A {
    public static func test(): Bool {
        return true
    }
}

public open class B <: A & I<B> {}

public class C<T> where T <: I<T> {
    public func f1(): Bool {
        return T.test()
    }
}

main() {
    var a = C<B>()
    if (a.f1()) {
        return 0
    }
    return 1
}
