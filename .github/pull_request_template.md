## What this changes

<!-- One or two sentences. New examples belong in
     https://github.com/jlt-commons/raylib-jolt-demo, not here. -->

## Gates

<!-- All four are fast and need no JVM at runtime. Please paste or confirm. -->

- [ ] `bb check:lib` passes (headless load-check of every library namespace)
- [ ] `bb test` passes (ffi/write argument order)
- [ ] `bb lint:strict` passes (clj-kondo, non-zero exit on any finding)
- [ ] `bb lsp:format-check` passes (clojure-lsp formatting, **not** cljfmt)

## If this changes a public var

- [ ] Ran `bb gen:all` and committed the regenerated `all.clj`
- [ ] Ran raylib-jolt-demo's `bb check` against this commit, if callers could break

## Environment you tested on

<!-- The pointer trick behind with-camera-2d / with-camera-3d is AArch64-specific, so
     architecture matters for anything touching the binding layer. -->

- OS / arch (`uname -sm`):
- jolt version (`jolt --version`):

## Notes for the reviewer

<!-- Anything surprising, any deliberate deviation, anything you're unsure about. -->
