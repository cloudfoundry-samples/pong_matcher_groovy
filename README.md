# Ping Pong Matcher (Groovy)

## Deploying

```bash
./gradlew distZip
```

Edit manifest.yml to specify the name of the Redis service to bind to.

```bash
cf push -n $HOSTNAME_OF_YOUR_CHOICE
```
