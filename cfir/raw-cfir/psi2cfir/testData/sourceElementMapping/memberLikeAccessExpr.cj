// RUN_PIPELINE_TILL: FRONTEND
class Box {
    let v: Int64 = 7
}

main() {
    let b = Box()
    let x = <expr>b.v</expr>
}

