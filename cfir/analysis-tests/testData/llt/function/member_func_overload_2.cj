// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    println(Data().foo() - 4)
    println(Data().fooprop - 4)
    return 0
}

open class Base {
    private static func foo(): Int32 {
        return 1
    }
    private static prop fooprop: Int32 {
        get() {
            return 1
        }
    }
}

open class Data <: Base {
    public open func foo(): Int32 {
        return 4
    }
    public open prop fooprop: Int32 {
        get() {
            return 4
        }
    }
}
