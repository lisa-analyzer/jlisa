How to Run Examples (JLiSA)

On Windows use gradlew.bat; on Linux/macOS use ./gradlew.
Outputs are written under tests-output/ (ignored by Git).

1) Hello demo

Windows

gradlew.bat run --args="-s src/test/resources/demo/com/example/demo -o tests-output/demo-hello"


Linux/macOS

./gradlew run --args="-s src/test/resources/demo/com/example/demo -o tests-output/demo-hello"

2) Micro (simplified service-like example)

Windows

gradlew.bat run --args="-s src/test/resources/com/example/micro -o tests-output/micro"


Linux/macOS

./gradlew run --args="-s src/test/resources/com/example/micro -o tests-output/micro"
