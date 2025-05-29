This file contains execution instructions.

== Requirements
(Check that you have at least Java 17 installed)


java --version


== Execute bigger.jar to generate GrGen.NET files:
(From the `bin/` directory of this project)

java -jar bigger.jar --verbose --basepath=../sample/selfsortingrobots-n2/ --output=foo \
--sig=sig.xmi --sigM=signatureMetaModel.ecore \
--host=host.xmi --metamodel=bigraphMetaModel.ecore \
--rule=es_0_1:es_0_1-lhs.xmi,es_0_1-rhs.xmi \
--rule=es_0_2:es_0_2-lhs.xmi,es_0_2-rhs.xmi \
--rule=es_1_2:es_1_2-lhs.xmi,es_1_2-rhs.xmi \
--rule=ss_1_0:ss_1_0-lhs.xmi,ss_1_0-rhs.xmi \
--rule=ss_2_0:ss_2_0-lhs.xmi,ss_2_0-rhs.xmi \
--rule=ss_2_1:ss_2_1-lhs.xmi,ss_2_1-rhs.xmi \
--rule=initMvmt:initMvmt-lhs.xmi,initMvmt-rhs.xmi \
--rule=move:move-lhs.xmi,move-rhs.xmi \
--tracking=map.json
