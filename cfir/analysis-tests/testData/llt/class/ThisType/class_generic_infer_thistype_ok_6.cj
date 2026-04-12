// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func arrayify<T>(body: () -> T): () -> Array<T> {
    {=> [body()]}
}

class Test {
    func self(): This {
        this
    }
    func selfArray() {
        arrayify(self)
    }
}

main(): Int64 {
    let arr = Test().selfArray()()
    if (arr.size != 1) {
        return 1
    }
    return 0
}
