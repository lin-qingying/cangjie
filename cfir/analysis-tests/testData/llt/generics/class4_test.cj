// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func getHeight(height: Int64): Int64 {
    return height * 2
}

func foo<T>(a: T): Int32 {
    return 10
}

class Data<T> {
    public let width: T
    public let f: Int32
    public init(x: T) {
        width = <!sema_ref_not_be_type!>T<!>
        <!sema_cannot_assign_to_immutable!>width = ((<!sema_ref_not_be_type!>T<!>))<!>
        f = foo<T>(<!sema_ref_not_be_type!>T<!>)
    }
}

func run(): Int32 {
    let data: Data<(Int64) -> Int64> = Data<(Int64) -> Int64>(getHeight)
    var res = data.width(5)
    if (res == 10) {
        return 0
    }
    return 1
}

main(): Int32 {
    return run()
}
