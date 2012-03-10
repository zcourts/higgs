call mvn compile
call mvn dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target\classes;target\lib\*" info.crlog.higgs.App