// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class A {
    public mut prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

class B <: A {
    public mut prop i: Int64 {
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
    public mut prop i: Int64 {
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

class B2 <: A4 {
    public mut prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

main() {
    0
}

/* SCAN
error: redefinition of declaration 'B2'
  ==> property8.cj:90:7:
   | 
90 | class B2 <: A4 {
   |       ^^ 
   | 
note: 'B2' is previously declared here
  ==> property8.cj:54:7:
   | 
54 | class B2 <: A2 {
   |       ^^ 
   | 

error: cannot override property 'i'
  ==> property8.cj:19:21:
   | 
19 |     public mut prop i: Int64 {
   |                     ^ 
   | 
note: the parent property is not modified by 'open'
  ==> property8.cj:10:21:
   | 
10 |     public mut prop i: Int64 {
   |                     ^ 
   | 

error: cannot override property 'i'
  ==> property8.cj:55:21:
   | 
55 |     public mut prop i: Int64 {
   |                     ^ 
   | 
note: the parent property is not modified by 'open'
  ==> property8.cj:46:21:
   | 
46 |     public mut prop i: Int64 {
   |                     ^ 
   | 

error: cannot override property 'i'
  ==> property8.cj:91:21:
   | 
91 |     public mut prop i: Int64 {
   |                     ^ 
   | 
note: the parent property is not modified by 'open'
  ==> property8.cj:82:21:
   | 
82 |     public mut prop i: Int64 {
   |                     ^ 
   | 

4 errors generated, 4 errors printed.
*/
