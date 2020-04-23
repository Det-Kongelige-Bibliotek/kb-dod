# How to install this

Appdrift info: <https://sbprojects.statsbiblioteket.dk/display/SSYS/...>


Extract the archive
```
tar -xzf ~/kb-dod.tar.gz -C ~/
```


## Tomcat Apps

Deploy the service `kb-dod.war` into `services/tomcat-apps/`

Copy the file `conf/context.xml` to `services/tomcat-apps/kb-dod.xml`

Symlink this file as `tomcat/conf/Catalina/localhost/kb-dod.xml`

Install the other config files (`conf/*`) into `services/conf/kb-dod/` and
adapt `services/tomcat-apps/kb-dod.xml` with these paths.

Adapt the config files as nessesary:

 * Check that `email.properties` reference an available SMTP server, and that `printer.email.override` is not set.
 * Check that `logback.xml` logs the way you want to where you want.
 * Check `dod.properties`:
    * Check that `alma_apikey` is set to the right value for the alma instance you want
