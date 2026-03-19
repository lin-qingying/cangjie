// RUN_PIPELINE_TILL: FRONTEND
foreign {
    func ext_add(x: Int32): Int32
}

main(): Int32 {
    return unsafe { ext_add(1) }
}

