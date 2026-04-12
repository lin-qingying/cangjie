// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC-PIPE-0: %run %run_opt %n.%suffix | compare %f

public class Binary<T> <: Collection<T> where T <: Equatable<T> & ToString {
    init(v: T) {
        println(1)
    }

    init(v: Array<T>) {
        println(2)
    }
    public func toString() {
        ""
    }
    public prop size: Int64 {
        get() {
            0
        }
    }
    public func isEmpty() {
        true
    }
    public func iterator() {
        return It<T>()
    }
}

public class It<T> <: Iterator<T> where T <: Equatable<T> & ToString {
    public func next(): Option<T> {
        None
    }
}

main() {
    let v = Binary([1, 2, 3])
    0
}

/* SCAN
2
*/
