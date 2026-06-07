// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Test {
    operator func <!UNABLE_TO_INFER_RETURN_TYPE!>+<!>(other: Test) {
        return this <!INVALID_BINARY_OPERATOR!>+<!> other  // recursive call without return type
    }
}

main() {
    let a = Test()
    let b = Test()
    let c = a + b
}
