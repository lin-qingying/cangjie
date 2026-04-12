// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output
// EXEC-0: %run %run_opt %output %run_args

public interface I1 {
    func f() {}
}

public interface I2 {
    func f() {}
}

public open class C1 <: I2 {}

public class C2 <: C1 & I1 {
    public func f() {}
}

main() {
    return 0
}

extend C1 {}

extend C2 {}
