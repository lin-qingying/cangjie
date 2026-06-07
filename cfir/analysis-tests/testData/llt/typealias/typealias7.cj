// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


<!sema_typealias_cycle, typealias_unused_type_parameters!>t<!>ype D<T> = F<Int32>

<!sema_typealias_cycle, typealias_unused_type_parameters!>t<!>ype F<U> = D<Rune>

main() {
    return 0
}
