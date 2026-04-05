// MODULE: helper
// FILE: helper.cj
package sample

func helper() {
}

// MODULE: app(helper)
// MAIN_MODULE
// MAIN_FILE_NAME: entry.cj
// FILE: entry.cj
package sample

func entry() {
    helper()
}

// FILE: other.cj
package sample

func other() {
}
