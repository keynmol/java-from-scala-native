java_home := $(or $(JAVA_HOME), /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home)

bindgen:
	sn-bindgen --header $(java_home)/include/jni.h --package libjni --scala --out libjni.scala --flavour scala-native05 --render.no-constructor JNINativeInterface_ -- -I$(java_home)/include/darwin

build:
	scala-cli package -f . \
		--native-linking -L$(java_home)/lib \
		--native-linking -ljli \
		--native-linking -L$(java_home)/lib/server \
		--native-linking -ljvm \
		--native-linking -Wl,-rpath --native-linking $(java_home)/lib \
		--native-linking -Wl,-rpath --native-linking $(java_home)/lib/server

run: 
	scala-cli run . \
		--native-linking -L$(java_home)/lib \
		--native-linking -ljli \
		--native-linking -L$(java_home)/lib/server \
		--native-linking -ljvm \
		--native-linking -Wl,-rpath --native-linking $(java_home)/lib \
		--native-linking -Wl,-rpath --native-linking $(java_home)/lib/server

custom-classpath:
	cs fetch -p org.scala-lang:scala-library:2.13.15 > custom-classpath
