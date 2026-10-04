# Contributing

Thanks for taking an interest. This repo is `net.b12n.raylib`, the
[raylib](https://github.com/raysan5/raylib) bindings for
[jolt](https://github.com/jolt-lang) (native Clojure, no JVM), calling the real
`libraylib` over its C ABI through `jolt.ffi`.

**Want to add an example?** The 187 example programs moved to
[raylib-jolt-demo](https://github.com/jlt-commons/raylib-jolt-demo), one project
each. New examples go there.

The documentation is published at <https://jlt-commons.github.io/raylib-jlt/>. It's generated from
this repo's `docs/guide/`; edit the Markdown here, never the site.

## Setting up

You need two things:

```sh
jolt --version    # jolt 0.8.0 or newer (lib/deps.edn declares :jolt/min-version)
bb lib:check      # is the native libraylib installed for this OS/arch?
```

If `bb lib:check` says no, `bb lib:install` will install it via your platform's
package manager (brew / pacman / apt / dnf / zypper / apk), or `bb lib:install
--dry-run` prints the command it would run so you can do it yourself.

If any task stops with `this project needs jolt 0.8.0 or newer`, your jolt is below
the floor `lib/deps.edn` declares. jolt v0.8.0 was released on 2026-09-01, so installing
a current jolt is the fix. The one exception is a build from `main` made between the
`ffi/write` change and that tag: it reports something like `v0.7.29-25-gd4e92a43`,
whose numeric prefix sorts below the floor even though it carries the new behaviour.
Prefix the command with `JOLT_SKIP_MIN_VERSION=1` in that case only. The
[README](README.md#jolt) explains why the floor exists, and why it could not have
caught an older jolt in the first place.

CI and your machine can be on different jolts, and that is deliberate rather than
a fault. The workflow pins `JOLT_VERSION` (0.8.1 at the time of writing), while
local development here tracks jolt's `main`, which runs ahead of the pin: 45
commits ahead as of 2026-09-02. So a regression introduced upstream shows up
locally and stays invisible in CI, and an upstream fix does the opposite. If a
failure you see locally goes green in CI, compare `jolt --version` on both sides
before concluding the failure was yours. To test a different runtime without
moving the pin, run the workflow via `workflow_dispatch` with the `jolt-version`
input.

[babashka](https://babashka.org) is optional but makes everything friendlier,
and `bb info` lists what there is. jolt reads `bb.edn` too, so `jolt <task>` works
as well.

## Before you open a PR

Run the gates. All four are fast and none needs a JVM at runtime:

```sh
bb check:lib          # every library namespace loads, headless (no window opens)
bb test               # unit suite: ffi/write puts the value where the offset says
bb lint:strict        # clj-kondo over lib/src and lib/test, non-zero exit on any finding
bb lsp:format-check   # clojure-lsp formatting, dry run
```

`bb test` earns its place by catching what `bb check:lib` structurally cannot. An
`ffi/write` argument flip swaps two integers, so the wrong call compiles exactly
as cleanly as the right one, and a compile-only gate reports green while every
write lands at the wrong address.

If you add, rename or remove anything public in a module, run `bb gen:all` and
commit the regenerated `lib/src/net/b12n/raylib/all.clj` with it. CI runs
`bb check:aggregator`, which fails when the two disagree.

`bb lsp:fix` applies formatting and ns cleanup in place if `lsp:format-check`
complains. **Formatting is owned by clojure-lsp, not cljfmt**; please don't run
cljfmt over the source, the two disagree on some compact literal tables.

If you'd like these to run automatically, `bb hooks:install` sets up a local
pre-commit hook (~2s). It's never committed, so each clone opts in.

Two rules worth knowing before you write any code:

- **Definitions must precede their first use.** Since jolt 0.4.0 an unresolved
  symbol is a compile error, not a late-bound reference. In the modules under
  `lib/src/net/b12n/raylib/`, one misordered symbol stops the module compiling
  and, because most programs require `net.b12n.raylib.all` (which aggregates all
  19 modules), everything that uses the library along with it. Only the first
  offender is reported. Fix them one at a time; `bb check:lib` is the quick
  confirmation.
- **A change here reaches raylib-jolt-demo only when it moves its pin.** It
  depends on this library by `:git/sha`, once, in its `common/deps.edn`. Running
  its `bb check` against your branch's commit is the best test of whether a
  change breaks a caller.

## Understanding the FFI

Before touching the binding layer, read
[`docs/guide/`](docs/guide/index.md) first. Every non-obvious decision in
the library traces back to how a particular C struct crosses the FFI boundary,
and the answer differs per struct: `Color` packs into a `:uint`,
`Camera2D`/`Camera3D` go by pointer, and `Vector2`/`Vector3` geometry has to fall
back to rlgl immediate mode. Those three pages explain why.

Note the **x86-64 caveat** documented in
[`struct-by-value-pointer-trick.md`](docs/guide/struct-by-value-pointer-trick.md):
the `Camera2D`/`Camera3D` pointer trick is AArch64-specific. If you're on x86-64
and raylib-jolt-demo's `camera2d` crashes with an invalid memory reference, that's the known cause,
not a bug in your setup, and a portable rlgl-matrix replacement would be a
genuinely valuable contribution.

## Publishing the site

Publishing is automatic. `.github/workflows/site.yml` builds it on every
pull request and deploys it when your change lands on `main`, so a docs change goes
live on merge without anyone running anything. You can preview it locally with
`bb site:serve` if you clone
[jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine) alongside this
repo, but the pull request build is the authority.

## Licensing

This project is released under the Eclipse Public License 2.0 (`EPL-2.0`), the
licence used across jlt-commons; it was zlib until 2026-09-05. By contributing, you
agree your contribution is licensed under those terms.
