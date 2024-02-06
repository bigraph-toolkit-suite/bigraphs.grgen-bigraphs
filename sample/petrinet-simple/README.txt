This file contains execution instructions.

== Requirements
(Check that you have Java 17 installed)


java --version


== Execute bigger.jar to generate GrGen.NET files:
(From the `bin/` directory of this project)


java -jar bigger.jar --verbose --basepath=../sample/petrinet-simple/ --host=host.xmi --output=foo --sig=sig.xmi --sigM=signatureBaseModel.ecore --metamodel=bigraphBaseModel.ecore --rule=rule1:r1-lhs.xmi,r1-rhs.xmi --tracking=map.json


== Execute GrGen.NET:
(from `sample/petrinet-simple/foo/` directory of this project)


GrShell script.grs


Hint: Edit the generated script.grs file to change the rule execution strategy.
Use debug exec instead of exec to go through the transformation step-by-step.
