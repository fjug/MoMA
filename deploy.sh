

mvn install:install-file -Dfile=lib/jmathplot.jar -DgroupId=jmathplot -DartifactId=jmathplot -Dversion=1.0 -Dpackaging=jar

mvn install:install-file -Dfile=lib/gurobi.jar -DgroupId=gurobi -DartifactId=gurobi -Dversion=1.0 -Dpackaging=jar

mvn -Denforcer.skip package
