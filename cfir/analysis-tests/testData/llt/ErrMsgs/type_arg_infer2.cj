// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// test err msg for conflicting constraints
func f<T>(x: T, y: T) {
}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f(1, "Hello")<!>
}
