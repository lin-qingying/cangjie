


const global: VArray<Int64, $3> = [1, 2, 3]

const func f(arr: VArray<Int64, $3>) {
    arr[0] + arr[1] + arr[2]
}

main() {
    f(global)
}
