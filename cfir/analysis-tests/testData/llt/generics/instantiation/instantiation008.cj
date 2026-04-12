// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class Tester<T> {
    init(x: T) {}
}

class Test {
    func f(): This {
        this
    }

    operator func !(): This {
        return f()
    }

    // Avoid duplicat instantiation for Tester<This> and Tester<Test>
    func test_instantiation() {
        let x = Tester(f())

        let y = Tester(this)

        let z = Tester(!this)

        let w = Tester(Test())
    }
}

main() {
}
