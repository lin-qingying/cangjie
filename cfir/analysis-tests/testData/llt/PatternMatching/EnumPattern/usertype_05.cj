// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-2: %run %run_opt %output %run_args
// compile and run with switch opt
// EXEC: %compiler %cmp_opt %f -o %output %enableO2
// EXEC-2: %run %run_opt %output %run_args

enum MyList<T> {
    | Cons(T, MyList<T>)
    | Nil
}

main() {
    let x = MyList.Cons(0, Cons(1, Cons(2, Nil)))
    match (x) {
        case MyList.Nil => 0
        case MyList.Cons(_, Nil) => 1
        case _ => 2
    }
}
