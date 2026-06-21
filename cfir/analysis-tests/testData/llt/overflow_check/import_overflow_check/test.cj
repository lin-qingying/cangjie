// FILE: p1/pkg_1.cj


package p1

public func fun_1(para_1: Int32, para_2: Int32, para_3!: Int32 = -10) {
    var pri_v1: Int32 = para_1
    var pri_v2: Int32 = para_2
    var pri_v3: Int32 = para_3
    return pri_v1 + pri_v2 + para_3
}

// FILE: test.cj



internal import p1.*

main() {
    var varCO2 = fun_1(1, 1)

    return 0
}
