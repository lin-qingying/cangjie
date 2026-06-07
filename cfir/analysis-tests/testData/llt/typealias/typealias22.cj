// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type Parse<T, Result> = ((Array<T>, Result)) -> ?Array<T>

func f<T, U>(a: ((Array<T>, U))): ?Array<T> {
    var (x, <!chir_dce_unused_variable!>y<!>) = a

    if (false) <!chir_dce_unreachable_block_in_expression!>{
        return None
    }<!> else {
        return x
    }
}

main() {
    let x: Parse<Int64, Bool> = f<Int64, Bool>

    let y: Int64 = match (x(([1, 2, 3], true))) {
        case None => 1
        case _ => 2
    }
    println(y)
}
