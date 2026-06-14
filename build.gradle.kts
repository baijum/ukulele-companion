plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
}

tasks.register<Copy>("installGitHooks") {
    description = "Installs the pre-commit hook from scripts/pre-commit"
    group = "git hooks"
    from("scripts/pre-commit")
    into(".git/hooks")
    filePermissions { unix("rwxr-xr-x") }
}
