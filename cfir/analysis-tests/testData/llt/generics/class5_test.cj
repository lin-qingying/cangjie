// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func getHeight(height: Int64): Int64 {
    return height * 2
}

class C {}

class Data<T> {
    public let width: T

    public init(x: C) {
        width = <!sema_mismatched_types!>C<!>
    }
}

func run(): Int32 {
    let data: Data<(Int64) -> Int64> = <!sema_parameters_and_arguments_mismatch!>D<!>ata<(Int64) -> Int64>(<!sema_mismatched_types!>getHeight<!>)
    var res = data.width(5)
    if (res == 10) {
        return 0
    }
    return 1
}

main(): Int32 {
    return run()
}
