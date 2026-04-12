// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-3: %run %run_opt %output %run_args
// compile and run with switch opt
// EXEC: %compiler %cmp_opt %f -o %output %enableO2
// EXEC-3: %run %run_opt %output %run_args

enum MyList<T> {
    | Cons(T, MyList<T>)
    | Nil
}

main() {
    let x = Cons(0, Cons(1, Cons(2, Nil)))
    match (x) {
        case MyList<Int64>.Nil => 0
        case Cons(_, Nil) => 1
        case Cons(_, MyList<Int64>.Cons(_, Nil)) => 2
        case MyList<Int64>.Cons(_, Cons(_, MyList<Int64>.Cons(_, MyList<Int64>.Nil))) => 3
        case _ => 4
    }
}
