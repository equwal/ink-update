# How to submit Ink Update to F-Droid

1. Tag the release `v0.1.1` and push the tag.
2. Fork `fdroid/fdroiddata` on GitLab, then clone your fork.
3. Copy `dev.equwal.inkupdate.yml` into `metadata/` of your clone.
4. Run `fdroid readmeta` and `fdroid lint dev.equwal.inkupdate` to check it.
5. Commit on a new branch and push it to your fork.
6. Open a merge request against `fdroid/fdroiddata`.
