

func func_166() {
    try {
        var d = {
            => return 4
        }
        return d()
    } finally {
    }
}

main() {
    var zz = func_166();
    if (zz.toString() != "4") {
        return 1
    }
    return 0;
}
