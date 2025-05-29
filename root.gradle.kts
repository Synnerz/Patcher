plugins {
    kotlin("jvm") version "1.9.0" apply false
    id("gg.essential.loom") apply false
    id("gg.essential.multi-version.root")
}

version = "1.8.9"

preprocess {
    "1.12.2"(11202, "srg") {
        "1.8.9"(10809, "srg", file("versions/1.12.2-1.8.9.txt"))
    }
}