// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class ContainerC {
    public interface I1 {                         // expected: regex interface type must be toplevel
        default foo1(): Int32 { return 1 }
    }

    public interface I2<T> {                      // expected: regex interface type must be toplevel
        default foo2(): Int32 { return 2 }
    }

    public interface I3 <: I1 & I2<Int64> {  // expected: regex interface type must be toplevel
        override default foo1(): Int32 { return 3 }
        override default foo2(): Int32 { return 3 }
    }
}

main(): Int64 {
    return 0
}

