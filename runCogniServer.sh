currentdir=$(pwd)

#even if this dir does not exist on this machine, we need it for cp scc accessing
projectdir="/root/eval/instagram4j/"
originaldir=${projectdir}target/instagram4j-1.8-SNAPSHOT-jar-with-dependencies.jar
redefdir=${projectdir}patch
originalclasslist=$currentdir/originalclasses.out
patchlist=$currentdir/allpatches.out

ssDiff="/root/ssDiffTool/target/ssDiffTool-1.0-SNAPSHOT-jar-with-dependencies.jar"
rtjce="/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/rt.jar:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/jce.jar"
cp=".:$ssDiff:$rtjce:${currentdir}/adapterOutput/"
rulesDir="/root/CryptoAnalysis/CryptoAnalysis/src/main/resources/JavaCryptographicArchitecture"
sootcp=/root/eval/instagram4j/target/classes/:$originaldir:.:$ssDiff:$rtjce:${currentdir}/adapterOutput/:${redefdir}

java -Xshareclasses:destroy,name=Cryptotest

java -Dcom.ibm.j9ddr.structurefile=/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/vm/j9ddr.dat  -Xshareclasses:name=Cryptotest -Djava.library.path=$LD_LIBRARY_PATH:/root/openj9-openjdk-jdk8/openj9/runtime/ddrext:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/jdk/classes/com/ibm/oti/shared/ -cp /root/eval/instagram4j/target/classes/:$originaldir:/root/soot/target/sootclasses-trunk-jar-with-dependencies.jar:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/jdk/ddr/classes/com/ibm/j9ddr/vm29/structure/:CryptoAnalysis/build/CryptoAnalysis-2.3-jar-with-dependencies.jar:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/jdk/classes/com/ibm/oti/shared/:/root/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/jre/lib/ddr/j9ddr.jar:$cp  crypto.TCPCryptoRunner --rulesDir=$rulesDir --patchlist=$patchlist -sootCp=$sootcp -redefcp $redefdir  -originalclasslist $originalclasslist

#-XXjitdirectory=/root/openj9-openjdk-jdk8/objs

#Does cp need: /root/openj9cryptoReleases/Agent/?
