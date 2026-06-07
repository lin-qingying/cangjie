// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


@Annotation
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
<!parse_illegal_macro_expand_input_args_without_paren!>t<!>ype P<T> = Array<T>
@APILevel[12] @APILevel[255]
<!parse_illegal_macro_expand_input_args_without_paren!>t<!>ype Rk = UIntNative

main() {}
