



interface I {
    mut static prop x: Int64
}

struct A <: I {
    public mut static redef prop x: Int64 {
        set(_) {}
    }
}

main() {
    println(A.x)
}

