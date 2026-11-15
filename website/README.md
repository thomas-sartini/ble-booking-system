# Ausleihbox 01: interactive 3D build guide

The site in `ausleihbox/index.html` contains the interactive model, assembly
instructions, wiring guide, measurements and borrowing simulation. The HTML is
self-contained: its model data and JavaScript dependencies are embedded.

This static site is separate from the booking application in `web/`.

The build guide supports the documentation work in
[T-045: Assemble terminal hardware prototype](https://github.com/thomas-sartini/ble-booking-system/issues/83).
Publishing the guide does not complete the physical assembly or hardware tests.

## Publish

Publication is handled by `.github/workflows/ausleihbox-pages.yml`. The workflow
uploads only `website/ausleihbox/`, not the rest of the repository.

Before the first deployment, a repository administrator must open **Settings >
Pages** and select **GitHub Actions** as the build and deployment source. The
`github-pages` environment must allow deployments from `main`.

Follow the repository contribution process: push a working branch and open a pull
request targeting `main`. Merging site or workflow changes into `main` starts a
deployment. The workflow can also be run manually on `main`. Working branches and
pull requests do not publish the site.

The expected project-site address after activation is:

<https://thomas-sartini.github.io/ble-booking-system/>

The deployment artifact places the contents of `website/ausleihbox/` at that URL.
The Actions run and the `github-pages` environment show the actual published URL.

## Update the model

Replace `ausleihbox/index.html` with the reviewed self-contained HTML, keeping the
entry filename `index.html`. Check its JavaScript syntax and review the diff before
opening a pull request. Preserve the embedded third-party license notices.
