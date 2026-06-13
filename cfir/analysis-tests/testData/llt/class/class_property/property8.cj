// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public mut prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

class B <: A {
    public mut prop <!CANNOT_OVERRIDE!>i<!>: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

open class A1 {
    public mut open prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

class B1 <: A1 {
    public mut prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

abstract class A2 {
    public mut prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

class B2 <: A2 {
    public mut prop <!CANNOT_OVERRIDE!>i<!>: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

interface A3 {
    mut prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

class B3 <: A3 {
    public mut prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

abstract class A4 {
    public mut prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

class <!CLASSIFIER_REDECLARATION!>B2<!> <: A4 {
    public mut prop <!CANNOT_OVERRIDE!>i<!>: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

main() {
    0
}
