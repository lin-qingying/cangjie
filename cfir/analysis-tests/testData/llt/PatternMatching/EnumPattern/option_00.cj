// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum Foo {
    | Some(Int64)

    prop value: Int64 {
        get() {
            match (this) {
                case Some(value_) => value_
            }
        }
    }
}

main(): Unit {
    let a: Option<Foo> = Option.Some(Foo.Some(10))
    let valueA = a?.value
}
