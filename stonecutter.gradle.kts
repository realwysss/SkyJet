plugins { id("dev.kikugie.stonecutter") }

stonecutter active "26.2"

stonecutter parameters {
    swaps["mc_version"] = "\"${node.metadata.version}\";"
    constants["fabric"] = true
}
