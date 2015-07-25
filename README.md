# Legion... for we are many

[![Circle CI](https://circleci.com/gh/mojotech/legion.svg?style=svg)](https://circleci.com/gh/mojotech/legion)

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

Change server config in core.clj (for now)

    lein repl
    (-main "ls -la")

## Test Dependencies

    * [VirtualBox](https://www.virtualbox.org/)

## Running Tests

    * Set `VBOX_HOME` for virtualbox
      * `export VBOX_HOME=/Applications/VirtualBox.app/Contents/MacOS` (on OSX)
      * `export VBOX_HOME=/usr/lib/virtualbox` (on Linux)
    * `VBoxManage setproperty websrvauthlibrary null` (only necessary once)
    * `vboxwebsrv -t0`
    * `lein test :all`

Copyright © 2015 MojoTech
