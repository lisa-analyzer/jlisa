How to Run Examples (JLiSA)

On Windows use gradlew.bat; on Linux/macOS use ./gradlew.
Outputs are written under tests-output/ (ignored by Git).

1) Hello demo

Windows

.\gradlew.bat clean compileJava run --args="-s src/test/resources/demo/com/example/demo -o tests-output/demo-hello -n ConstantPropagation -l INFO"


Linux/macOS

./gradlew clean compileJava run --args="-s src/test/resources/demo/com/example/demo -o tests-output/demo-hello -n ConstantPropagation -l INFO"



2) Micro (simplified service-like example)

Windows

.\gradlew.bat clean compileJava run --args="-s src/test/resources/com/example/micro -o tests-output/micro -n ConstantPropagation -l INFO"


Linux/macOS

./gradlew clean compileJava run --args="-s src/test/resources/com/example/micro -o tests-output/micro -n ConstantPropagation -l INFO"
