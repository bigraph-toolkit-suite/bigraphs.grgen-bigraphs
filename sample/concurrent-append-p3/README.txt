This file contains execution instructions.

== Requirements
(Check that you have Java 17 installed)


java --version


== Execute bigger.jar to generate GrGen.NET files:
(From the `bin/` directory of this project)


java -jar bigger.jar --verbose \
--basepath=../sample/concurrent-append-p3/ --output=foo \
--sig=sig.xmi --sigM=signatureMetaModel.ecore \
--host=host.xmi --metamodel=bigraphMetaModel.ecore \
--rule=nextRule:nextRule-lhs.xmi,nextRule-rhs.xmi \
--rule=appendRule:appendRule-lhs.xmi,appendRule-rhs.xmi \
--rule=returnRule:returnRule-lhs.xmi,returnRule-rhs.xmi \
--tracking=map.json


== Execute GrGen.NET:
(from `sample/concurrent-append-p3/foo/` directory of this project)


GrShell script.grs


Hint: Edit the generated script.grs file to change the rule execution strategy.
Use debug exec instead of exec to go through the transformation step-by-step.
