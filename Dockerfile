FROM openjdk:11
COPY ./build/classes/java/main/ tmp/
WORKDIR tmp/
ENTRYPOINT ["java","Application"]