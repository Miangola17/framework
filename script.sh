
echo "=== Compilation du framework ==="

mkdir -p bin

javac -cp lib/servlet-api.jar:lib/gson-2.10.1.jar -d bin src/main/java/mg/framework/**/*.java

if [ $? -ne 0 ]; then
    echo "ERREUR : La compilation a échoué !"
    exit 1
fi

echo "Compilation OK !"

jar -cf framework.jar -C bin .

echo "=== framework.jar généré avec succès ==="
echo "Vous pouvez copier framework.jar dans sprint/WebContent/WEB-INF/lib/"
