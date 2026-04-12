// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

public interface I1 {
    func foo(): Int64
}

public interface I2 {
    func foo(): Int64 {
        return 1
    }
}

public open class C1 {
    public open func foo(): Int64 {
        return 2
    }
}

public open class C2 <: C1 & I1 & I2 {
    public override func foo(): Int64 {
        print("calling foo() in C2\n")
        return 3
    }
}

public class C3 <: C2 {}
