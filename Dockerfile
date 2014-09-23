# DOCKER-VERSION 1.2.0

FROM        camelpunch/pong-matcher-base:groovy

# install app as unprivileged user
ADD         app pong_matcher_groovy
RUN         chown -R web:web pong_matcher_groovy

USER        web
ENV         GROOVY_HOME /groovy
ENV         PATH $GROOVY_HOME/bin:$PATH

# cache dependencies so that running is fast
RUN         cd pong_matcher_groovy; ./gradlew installApp

ENTRYPOINT  redis-server & cd pong_matcher_groovy; ./gradlew test
