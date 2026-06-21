

main() {
    let a = 3
    if (let Some(2) | Some(3) | None <- Some(a)) {
        0
    } else {
        1
    }
}
