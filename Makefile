.PHONY: build test clean run

build:
	./gradlew build -x test

test:
	./gradlew cleanTest test

clean:
	./gradlew clean

run:
	./gradlew bootRun

deploy:
	REGISTRY_NAME=app-images APP_ID=eaf865e2-37a3-4bb2-afbb-3a14a25ceb9d ./deploy.sh

integ-test:
	java -ea Integration-test/LiveDeploymentE2ETest.java
	