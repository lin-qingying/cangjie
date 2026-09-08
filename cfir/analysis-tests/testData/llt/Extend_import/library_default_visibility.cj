// IMPORT_PATH: ../../libraries/extend_conflicts/primitive
import a.IA
import b.Unrelated

main() {
    let x: Int64 = 1
    println(x.code)
    Unrelated()
    return 0
}
