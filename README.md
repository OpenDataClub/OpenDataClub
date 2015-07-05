# Open Data Club

## Requirements

* Scala: [installing Scala](http://scala-lang.org/download/).

* SBT: [installing sbt](http://www.scala-sbt.org/download.html).

* PostgreSQL 9.4. You can find database configuration at `conf/application.conf`.

## How to run

Clone repository.

```
$ sbt
[Open Data Cloud] $ run
```

### How to run for development:

```
export SBT_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=4000,server=y,suspend=n" && activator
[Open Data Cloud] $ ~run
``

This way you can debug from EclipseIDE with Debug --> Remote Java Application, Standard (Socket attach), port: 4000, and files will be automatically recompiled.

### Troubleshooting

[It looks like sbteclipse can't generate relative paths at .classpath](https://github.com/typesafehub/sbteclipse/issues/164), so we can't share it.
It's removed at `.gitignore`, so you might want to run `eclipse` inside activator console in order to generate a fresh .classpath.