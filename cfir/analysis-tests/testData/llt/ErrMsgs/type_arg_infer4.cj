// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// test constraints from destruction
func f<T>(a: Array<T>): Array<T> {
    a
}

main() {
    let v: Array<Array<String>> = <!UNABLE_TO_INFER_GENERIC_FUNC!>f([[1]])<!>
}
