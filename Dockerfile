# DOCKER-VERSION 1.2.0

FROM        camelpunch/pong-matcher-base-groovy

# install app as unprivileged user
ADD         app pong_matcher_groovy
RUN         chown -R web:web pong_matcher_groovy

USER        web
ENV         GROOVY_HOME /groovy
ENV         PATH $GROOVY_HOME/bin:$PATH

RUN         redis-server & cd pong_matcher_groovy; ./gradlew test
