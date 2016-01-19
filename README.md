# SiteTracker
SiteTracker is desgined to help explorers document the sites they find. It's more or less just a pet project of mine that was birthed when a friend and I were exploring together. We had a hard time coordinating the systems and sites we already had been to. We then tried a spreadsheet, but that got nasty fast. This is here to help with that.

## Building
Clone this repo, move to its directory, and run `sbt assembly`. Executable jars will be output in `../client/target/scala-2.11` and `../server/target/scala-2.11`. These are packaged with Scala, so they can be run by invoking them with `java -jar ..`, no need for `scala` to be installed on the machine to run.

## Usage
The `Server` can be run on any machine and needs no further configuration. The `Client`s however will need to be configured to point at the correct address. The `Client` [config](client/src/main/resources/application.conf) has the setting `server.host` that is used to connect to the remote server. Locally, it can be left as it is. Any remote `Client` though must be packaged with the config that has the address that the `Server` machine can be reached at.

## TODO
- Documentation
- Help within the program (for commands)
- More commands (?)
- GUI (!)




