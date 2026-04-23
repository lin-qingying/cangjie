// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

func case03<T>() {
    1
}

func case04() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>case03<!>
}
