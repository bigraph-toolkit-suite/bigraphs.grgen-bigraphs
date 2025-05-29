This file contains execution instructions.

== Requirements
(Check that you have Java 17 installed)


java --version


== Execute bigger.jar to generate GrGen.NET files:
(From the `bin/` directory of this project)


java -jar bigger.jar --verbose --basepath=../sample/selfsortingrobots-n2/ --output=foo \
--host=host.xmi --metamodel=bigraphMetaModel.ecore  --sig=sig.xmi --sigM=signatureMetaModel.ecore \
--rule=es_0_1:es_0_1-lhs.xmi,es_0_1-rhs.xmi \
--rule=ss_1_0:ss_1_0-lhs.xmi,ss_1_0-rhs.xmi \
--rule=initMvmt:initMvmt-lhs.xmi,initMvmt-rhs.xmi \
--rule=move:move-lhs.xmi,move-rhs.xmi \
--tracking=map.json
