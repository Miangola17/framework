
echo "=== Compilation du framework ==="

export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
JAVAC="$JAVA_HOME/bin/javac"

mkdir -p bin

"$JAVAC" -cp lib/servlet-api.jar:lib/gson-2.10.1.jar -d bin src/main/java/mg/framework/**/*.java

if [ $? -ne 0 ]; then
    echo "ERREUR : La compilation a echoue !"
    exit 1
fi

echo "Compilation OK !"

jar -cf framework.jar -C bin .

echo "=== framework.jar généré avec succès ==="
echo "Vous pouvez copier framework.jar dans sprint/WebContent/WEB-INF/lib/"
