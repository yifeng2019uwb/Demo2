.PHONY: build test clean run integ-test integ-test-python coverage

build:
	./gradlew build -x test

test:
	./gradlew cleanTest test jacocoTestReport

clean:
	./gradlew clean

run:
	./gradlew bootRun

deploy:
	REGISTRY_NAME=app-images APP_ID=eaf865e2-37a3-4bb2-afbb-3a14a25ceb9d ./deploy.sh

integ-test:
	java -ea Integration-test/ReportIT.java

integ-test-python:
	cd Integration-test/python && python3 -m venv .venv && .venv/bin/pip install -r requirements.txt -q && .venv/bin/pytest test_live_deployment.py -v
